package com.portwind.gametrans

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Qwen3-Omni API 管理器（OpenAI 兼容模式）
 * 说明：使用 DashScope 的 OpenAI 兼容接口，格式标准化
 */
class QwenApiManager(private val context: Context) {

    companion object {
        private const val TAG = "QwenApiManager"
        private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        private const val MODEL_NAME = "qwen-vl-plus"  // 使用 Qwen VL Plus 多模态视觉模型
        
        // 速率限制配置
        private const val MIN_REQUEST_INTERVAL_MS = 1000L  // 最小请求间隔 1 秒
        private const val MAX_RETRY_ATTEMPTS = 1  // 最大重试次数
        private const val INITIAL_BACKOFF_MS = 2000L  // 初始退避时间 2 秒
        private const val MAX_BACKOFF_MS = 32000L  // 最大退避时间 32 秒
    }

    private val settingsManager = SettingsManager(context)
    private val httpClient: OkHttpClient = OkHttpClient()
    @Volatile private var lastErrorMessage: String? = null
    
    // 记录上次请求时间，用于速率限制
    private val lastRequestTime = AtomicLong(0L)

    fun getLastErrorMessage(): String? = lastErrorMessage

    /**
     * 速率限制控制：确保请求间隔不小于最小间隔
     */
    private suspend fun enforceRateLimit() {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastRequestTime.get()
        val timeSinceLastRequest = currentTime - lastTime
        
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            val waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            Log.d(TAG, "速率限制：等待 ${waitTime}ms 后再发送请求")
            delay(waitTime)
        }
        
        lastRequestTime.set(System.currentTimeMillis())
    }
    
    /**
     * 计算指数退避时间
     */
    private fun calculateBackoffTime(attempt: Int): Long {
        val backoff = (INITIAL_BACKOFF_MS * 2.0.pow(attempt.toDouble())).toLong()
        return min(backoff, MAX_BACKOFF_MS)
    }

    /**
     * 优化图像以提升API处理速度和减小传输大小
     * @param bitmap 原始图像
     * @return 优化后的图像
     */
    private fun optimizeImage(bitmap: Bitmap): Bitmap {
        val settings = settingsManager.getSettings()
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "开始优化图像: ${bitmap.width}x${bitmap.height}")

        // 1. 计算缩放比例
        val maxDimension = max(bitmap.width, bitmap.height)
        val scaleFactor = if (maxDimension > settings.maxImageSize) {
            settings.maxImageSize.toFloat() / maxDimension
        } else {
            1.0f
        }

        val optimizedBitmap = if (scaleFactor < 1.0f) {
            // 2. 缩放图像
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()

            Log.d(TAG, "缩放图像到: ${newWidth}x${newHeight} (缩放比例: ${String.format("%.2f", scaleFactor)})")

            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            Log.d(TAG, "图像尺寸已符合要求，无需缩放")
            bitmap
        }

        val endTime = System.currentTimeMillis()
        Log.d(TAG, "图像优化完成，耗时: ${endTime - startTime}ms")
        Log.d(TAG, "优化后尺寸: ${optimizedBitmap.width}x${optimizedBitmap.height}")

        return optimizedBitmap
    }

    /**
     * 翻译/理解屏幕截图（图片+提示词）带重试机制
     */
    suspend fun translateImage(bitmap: Bitmap, promptOverride: String? = null): TranslationResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.QWEN_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "Qwen API key missing")
            lastErrorMessage = "API 密钥未配置"
            return@withContext null
        }

        var optimizedBitmap: Bitmap? = null
        try {
            Log.d(TAG, "开始调用 Qwen API 进行图像翻译...")
            Log.d(TAG, "原始图像尺寸: ${bitmap.width}x${bitmap.height}")

            // 1. 优化图像以提升处理速度和减小传输大小
            optimizedBitmap = optimizeImage(bitmap)
            Log.d(TAG, "图像优化完成")

            // 2. 构建翻译指令（支持临时覆盖）
            val prompt = if (!promptOverride.isNullOrBlank()) {
                Log.d(TAG, "使用临时提示词覆盖设置中的提示词")
                promptOverride
            } else {
                settingsManager.buildTranslationPrompt()
            }
            Log.d(TAG, "翻译指令构建完成")

            // 3. 将 Bitmap 压缩为 JPEG 并转换为 Base64 编码（使用优化后的图像）
            val baos = ByteArrayOutputStream()
            val compressionQuality = settingsManager.getCompressionQuality()
            optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, baos)
            val imageBytes = baos.toByteArray()
            Log.d(TAG, "图像压缩完成，质量: $compressionQuality%, 大小: ${imageBytes.size / 1024}KB")

            // 4. Base64 编码（使用 NO_WRAP 避免换行符）
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            Log.d(TAG, "Base64 编码完成，长度: ${imageBase64.length}")

            // 5. 构建 Data URL（符合标准格式：data:[MIME_type];base64,{base64_image}）
            val dataUri = "data:image/jpeg;base64,$imageBase64"
            Log.d(TAG, "Data URL 构建完成")

            // 6. 使用 OpenAI 兼容格式发送请求（带重试机制）
            var lastException: Exception? = null
            for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
                try {
                    // 速率限制控制
                    enforceRateLimit()
                    
                    val request = buildQwenRequest(apiKey, prompt, dataUri)
                    Log.d(TAG, "发送 API 请求（尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS）...")
                    
                    val resp = httpClient.newCall(request).execute()
                    try {
                        if (!resp.isSuccessful) {
                            val errorBody = try { resp.body?.string()?.trim() } catch (_: Exception) { null }
                            val errorCode = resp.code
                            
                            // 检查是否为 429 错误（速率限制）或 5xx 服务器错误
                            if (errorCode == 429 || errorCode in 500..599) {
                                val backoffTime = calculateBackoffTime(attempt)
                                Log.w(TAG, "Qwen API 错误 $errorCode，尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS")
                                
                                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                                    Log.i(TAG, "等待 ${backoffTime}ms 后重试...")
                                    lastErrorMessage = "API 错误 $errorCode，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                                    delay(backoffTime)
                                    continue
                                } else {
                                    Log.e(TAG, "API 错误 $errorCode，已达最大重试次数")
                                    lastErrorMessage = if (errorCode == 429) {
                                        "API 配额已用完，请稍后再试"
                                    } else {
                                        "服务器错误 ($errorCode)，请稍后再试"
                                    }
                                    return@withContext null
                                }
                            } else {
                                // 其他错误不重试
                                val msg = "Qwen request failed: ${resp.code} ${resp.message} ${if (!errorBody.isNullOrBlank()) "| body: $errorBody" else ""}"
                                Log.e(TAG, msg)
                                lastErrorMessage = errorBody ?: "HTTP ${resp.code} ${resp.message}"
                                return@withContext null
                            }
                        }

                        val respStr = resp.body?.string()?.trim()
                        if (respStr.isNullOrBlank()) {
                            Log.w(TAG, "Qwen empty response body")
                            lastErrorMessage = "Empty response body"
                            return@withContext null
                        }

                        Log.d(TAG, "API 响应接收完成")
                        val parsed = parseQwenResponse(respStr)
                        if (parsed != null) {
                            Log.d(TAG, "Qwen translation success")
                            lastErrorMessage = null
                            return@withContext parsed
                        } else {
                            Log.w(TAG, "Qwen parse returned null")
                            lastErrorMessage = "Failed to parse Qwen response"
                            return@withContext null
                        }
                    } finally {
                        try { resp.close() } catch (_: Exception) { }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    Log.e(TAG, "API 调用超时 (尝试 ${attempt + 1})", e)
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        Log.i(TAG, "等待后重试...")
                        lastErrorMessage = "网络超时，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "网络超时，请检查网络连接"
                    }
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "网络连接失败，请检查网络状态", e)
                    lastErrorMessage = "网络连接失败，请检查网络连接"
                    return@withContext null
                } catch (e: java.io.IOException) {
                    lastException = e
                    Log.e(TAG, "网络 IO 异常 (尝试 ${attempt + 1})", e)
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        Log.i(TAG, "等待后重试...")
                        lastErrorMessage = "网络 IO 异常，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "网络 IO 异常: ${e.message}"
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "API 调用失败 (尝试 ${attempt + 1})", e)
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        Log.i(TAG, "等待后重试...")
                        lastErrorMessage = "API 调用失败，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "API 调用失败: ${e.message}"
                    }
                }
            }
            
            // 所有重试都失败
            Log.e(TAG, "API 调用失败，已达最大重试次数")
            if (lastException != null) {
                lastException.printStackTrace()
            }
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "Qwen translateImage failed", e)
            lastErrorMessage = e.message
            return@withContext null
        } finally {
            // 清理优化后的 bitmap（如果和原 bitmap 不同）
            if (optimizedBitmap != null && optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
                Log.d(TAG, "优化后的 bitmap 已回收")
            }
        }
    }

    /** 构建 Qwen 请求（OpenAI 兼容格式） */
    private fun buildQwenRequest(
        apiKey: String,
        prompt: String,
        dataUri: String
    ): Request {
        // 构建 content 数组：图片 + 文本（OpenAI 标准格式）
        val contentArr = JSONArray()
        
        // 添加图片内容（OpenAI 格式：image_url 是对象）
        contentArr.put(JSONObject().apply {
            put("type", "image_url")
            put("image_url", JSONObject().apply {
                put("url", dataUri)
            })
        })
        
        // 添加文本内容
        contentArr.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        
        val userMsg = JSONObject().apply {
            put("role", "user")
            put("content", contentArr)
        }
        val messages = JSONArray().put(userMsg)

        // OpenAI 兼容格式：messages 直接在根层级
        val reqJson = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", messages)
        }

        // 打印请求体用于调试（不包含图片数据以避免日志过长）
        val debugJson = JSONObject(reqJson.toString())
        try {
            val debugMessages = debugJson.getJSONArray("messages")
            for (i in 0 until debugMessages.length()) {
                val msg = debugMessages.getJSONObject(i)
                val content = msg.optJSONArray("content")
                if (content != null) {
                    for (j in 0 until content.length()) {
                        val item = content.getJSONObject(j)
                        if (item.has("image_url")) {
                            val imageUrlObj = item.optJSONObject("image_url")
                            if (imageUrlObj != null && imageUrlObj.has("url")) {
                                val url = imageUrlObj.getString("url")
                                if (url.startsWith("data:")) {
                                    imageUrlObj.put("url", "data:image/jpeg;base64,[TRUNCATED]")
                                }
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Qwen request body (OpenAI compatible): ${debugJson.toString(2)}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log debug JSON", e)
        }

        val media = "application/json; charset=utf-8".toMediaType()
        val body = reqJson.toString().toRequestBody(media)
        return Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
    }

    private fun parseQwenResponse(json: String): TranslationResult? {
        return try {
            val obj = JSONObject(json)

            // OpenAI 兼容格式：choices[0].message.content（字符串）
            val choices = obj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.optJSONObject(0)?.optJSONObject("message")
                val content = msg?.optString("content")
                if (!content.isNullOrBlank()) {
                    val trimmedContent = content.trim()
                    Log.d(TAG, "Parsed Qwen response: ${trimmedContent.take(100)}...")
                    Log.d(TAG, "响应长度: ${trimmedContent.length} 字符")
                    
                    // 检查是否为"未检测到文本"的回复
                    val noContentPhrases = listOf(
                        "未检测到需要翻译的文本", "未检测到文本", "没有找到文字",
                        "no text found", "no text detected", "无文字内容"
                    )
                    
                    if (noContentPhrases.any { trimmedContent.contains(it, ignoreCase = true) }) {
                        Log.w(TAG, "⚠️ Qwen 返回未检测到文本，可能原因：")
                        Log.w(TAG, "  1. 图片质量过低或太模糊")
                        Log.w(TAG, "  2. 图片压缩率过高")
                        Log.w(TAG, "  3. 图片中确实没有可识别的文本")
                        Log.w(TAG, "  4. 模型识别失败")
                    }
                    
                    return TranslationResult(originalText = null, translatedText = trimmedContent)
                }
            }

            // 兼容 DashScope 原生格式：output.text
            val text1 = obj.optJSONObject("output")?.optString("text")
            if (!text1.isNullOrBlank()) {
                Log.d(TAG, "使用 DashScope 原生格式解析响应")
                return TranslationResult(originalText = null, translatedText = text1.trim())
            }

            Log.w(TAG, "Unable to parse Qwen response, no recognized format")
            Log.w(TAG, "响应 JSON 结构: ${obj.keys().asSequence().joinToString()}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "parseQwenResponse error", e)
            null
        }
    }
}


