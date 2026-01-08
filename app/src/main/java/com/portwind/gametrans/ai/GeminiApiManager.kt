package com.portwind.gametrans.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.portwind.gametrans.BuildConfig
import com.portwind.gametrans.settings.SettingsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.ServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 翻译结果数据类
 * @param originalText 识别出的原文
 * @param translatedText 翻译后的文本
 */
data class TranslationResult(
    val originalText: String?,
    val translatedText: String
)

/**
 * 文本对话消息
 */
data class ChatMessage(
    val role: String, // "user" | "assistant"
    val text: String
)

/**
 * Gemini API管理器
 * 负责处理图像识别和翻译功能
 */
class GeminiApiManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GeminiApiManager"
        
        // 模型配置
        private const val MODEL_FLASH = "gemini-3-flash-preview"  // 默认/优化模式：速度快
        private const val MODEL_PRO = "gemini-3-pro-preview"    // 详细模式：更精确
        
        // 速率限制配置
        private const val MIN_REQUEST_INTERVAL_MS = 500L  // 最小请求间隔 0.5 秒
        private const val MAX_RETRY_ATTEMPTS = 2  // 最大重试次数（减少等待时间）
        private const val INITIAL_BACKOFF_MS = 1000L  // 初始退避时间 1 秒
        private const val MAX_BACKOFF_MS = 16000L  // 最大退避时间 16 秒
    }
    
    private val settingsManager = SettingsManager(context)
    
    // 防止重复请求的原子锁
    private val isTranslating = AtomicBoolean(false)
    
    // 记录上次请求时间，用于速率限制
    private val lastRequestTime = AtomicLong(0L)
    
    // 存储最后的错误消息
    @Volatile private var lastErrorMessage: String? = null
    
    // Flash 模型（默认/优化模式）
    private val flashModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_FLASH,
            apiKey = getApiKey()
        )
    }
    
    // Pro 模型（详细模式）
    private val proModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_PRO,
            apiKey = getApiKey()
        )
    }
    
    // 兼容旧代码的默认模型
    private val generativeModel: GenerativeModel
        get() = getModelForCurrentMode()
    
    /**
     * 根据当前提示词模式获取对应的模型
     */
    private fun getModelForCurrentMode(): GenerativeModel {
        val mode = settingsManager.getPromptMode()
        return when (mode) {
            com.portwind.gametrans.settings.PromptMode.DETAILED -> {
                Log.d(TAG, "使用 Pro 模型: $MODEL_PRO")
                proModel
            }
            else -> {
                Log.d(TAG, "使用 Flash 模型: $MODEL_FLASH")
                flashModel
            }
        }
    }

    /**
     * 获取最后的错误消息
     */
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
     * 文本多轮对话（简化实现，带速率限制和重试）：
     * 将历史与当前问题拼接为一个上下文提示，获取助手回复。
     */
    suspend fun sendChatMessage(history: List<ChatMessage>, userMessage: String): String? = withContext(Dispatchers.IO) {
        try {
            if (getApiKey().isBlank()) {
                lastErrorMessage = "API密钥未配置"
                return@withContext null
            }

            val sb = StringBuilder()
            if (history.isNotEmpty()) {
                sb.appendLine("对话历史：")
                history.takeLast(20).forEach { msg ->
                    val prefix = if (msg.role.equals("assistant", true)) "助手" else "用户"
                    sb.append(prefix).append(": ").appendLine(msg.text.trim())
                }
                sb.appendLine("---")
            }
            sb.append("用户: ").appendLine(userMessage.trim()).append("助手: ")

            val input = content { text(sb.toString()) }
            
            // 带重试机制的 API 调用
            for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
                try {
                    // 速率限制控制
                    enforceRateLimit()
                    
                    Log.d(TAG, "发送聊天消息（尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS）...")
                    val response = generativeModel.generateContent(input)
                    val text = response.text?.trim()
                    
                    if (text.isNullOrBlank()) {
                        lastErrorMessage = "API返回空结果"
                        return@withContext null
                    }
                    
                    lastErrorMessage = null
                    return@withContext text
                    
                } catch (e: ServerException) {
                    val errorMsg = e.message ?: ""
                    
                    // 检查是否为 429 错误（配额超限）
                    if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
                        val backoffTime = calculateBackoffTime(attempt)
                        Log.w(TAG, "聊天API配额超限 (429)，尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS")
                        
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            Log.i(TAG, "等待 ${backoffTime}ms 后重试...")
                            delay(backoffTime)
                            continue
                        } else {
                            Log.e(TAG, "聊天API配额超限，已达最大重试次数")
                            lastErrorMessage = "API配额已用完，请稍后再试"
                            return@withContext null
                        }
                    } else {
                        Log.e(TAG, "服务器错误 (尝试 ${attempt + 1})", e)
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            delay(calculateBackoffTime(attempt))
                            continue
                        } else {
                            lastErrorMessage = "服务器错误: ${e.message}"
                            return@withContext null
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "sendChatMessage失败 (尝试 ${attempt + 1})", e)
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "聊天失败: ${e.message}"
                        return@withContext null
                    }
                }
            }
            
            lastErrorMessage = "聊天失败，已达最大重试次数"
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "sendChatMessage意外错误", e)
            lastErrorMessage = "发送消息失败: ${e.message}"
            return@withContext null
        }
    }
    
    /**
     * 获取API密钥
     * 从BuildConfig获取，确保密钥安全
     */
    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            // 调试日志：显示密钥长度和前缀（隐藏完整密钥）
            val keyPreview = if (key.length > 10) "${key.take(8)}...${key.takeLast(4)}" else "[空或过短]"
            Log.d(TAG, "API密钥已获取, 长度: ${key.length}, 预览: $keyPreview")
            key
        } catch (e: Exception) {
            Log.e(TAG, "无法获取API密钥，请检查secrets.properties配置", e)
            ""
        }
    }
    
    /**
     * 优化图像以提升API处理速度
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
     * 使用Gemini进行图像翻译（带重试和速率限制）
     * @param bitmap 要翻译的屏幕截图
     * @return 翻译结果，包含原文和译文
     */
    suspend fun translateImage(bitmap: Bitmap, promptOverride: String? = null): TranslationResult? = withContext(Dispatchers.IO) {
        // 防止重复请求 - 如果正在翻译中，直接返回
        if (!isTranslating.compareAndSet(false, true)) {
            Log.w(TAG, "翻译请求已在进行中，跳过重复请求")
            lastErrorMessage = "翻译请求已在进行中，请稍候"
            return@withContext null
        }
        
        var optimizedBitmap: Bitmap? = null
        try {
            Log.d(TAG, "开始调用Gemini API进行图像翻译...")
            Log.d(TAG, "原始图像尺寸: ${bitmap.width}x${bitmap.height}")
            
            // 优化图像以提升处理速度
            optimizedBitmap = optimizeImage(bitmap)
            Log.d(TAG, "图像优化完成")
            
            // 构建翻译指令（支持临时覆盖）
            val prompt = if (!promptOverride.isNullOrBlank()) {
                Log.d(TAG, "使用临时提示词覆盖设置中的提示词")
                promptOverride
            } else {
                settingsManager.buildTranslationPrompt()
            }
            Log.d(TAG, "翻译指令构建完成")
            
            // 构建请求内容 - 使用优化后的图像
            Log.d(TAG, "构建API请求内容...")
            val inputContent = content {
                image(optimizedBitmap)
                text(prompt)
            }
            Log.d(TAG, "API请求内容构建完成")
            
            // 带重试机制的 API 调用
            var lastException: Exception? = null
            for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
                try {
                    // 速率限制控制
                    enforceRateLimit()
                    
                    Log.d(TAG, "发送API请求（尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS）...")
                    val response = generativeModel.generateContent(inputContent)
                    Log.d(TAG, "API响应接收完成")
                    
                    val result = response.text
                    
                    if (result.isNullOrBlank()) {
                        Log.w(TAG, "API返回空结果")
                        lastErrorMessage = "API返回空结果"
                        return@withContext null
                    }
                    
                    Log.d(TAG, "API响应获取成功")
                    Log.d(TAG, "响应长度: ${result.length} 字符")
                    
                    // 分析响应内容
                    val lineCount = result.lines().size
                    val wordCount = result.split("\\s+".toRegex()).size
                    Log.d(TAG, "响应统计: $lineCount 行, $wordCount 词")
                    
                    if (result.contains("...") || result.contains("省略") || result.contains("truncated")) {
                        Log.w(TAG, "检测到可能的不完整响应标志")
                    }
                    
                    // 解析结果，提取原文和译文
                    val translationResult = parseTranslationResult(result)
                    Log.d(TAG, "翻译结果解析完成")
                    lastErrorMessage = null
                    return@withContext translationResult
                    
                } catch (e: ServerException) {
                    lastException = e
                    val errorMsg = e.message ?: ""
                    
                    // 检查是否为 429 错误（配额超限）
                    if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
                        val backoffTime = calculateBackoffTime(attempt)
                        Log.w(TAG, "API配额超限 (429)，尝试 ${attempt + 1}/$MAX_RETRY_ATTEMPTS")
                        
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            Log.i(TAG, "等待 ${backoffTime}ms 后重试...")
                            lastErrorMessage = "API配额超限，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                            delay(backoffTime)
                            continue
                        } else {
                            Log.e(TAG, "API配额超限，已达最大重试次数")
                            lastErrorMessage = "API配额已用完，请稍后再试或检查您的配额设置\n\n" +
                                "解决方案：\n" +
                                "1. 等待一段时间后重试（建议等待1分钟以上）\n" +
                                "2. 检查您的 Gemini API 配额：https://ai.dev/usage?tab=rate-limit\n" +
                                "3. 如果是免费版，请考虑升级到付费版本"
                            return@withContext null
                        }
                    } else {
                        // 其他服务器错误
                        Log.e(TAG, "服务器错误 (尝试 ${attempt + 1})", e)
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            val backoffTime = calculateBackoffTime(attempt)
                            Log.i(TAG, "等待 ${backoffTime}ms 后重试...")
                            lastErrorMessage = "服务器错误，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                            delay(backoffTime)
                            continue
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    Log.e(TAG, "API调用超时 (尝试 ${attempt + 1})", e)
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
                    Log.e(TAG, "网络IO异常 (尝试 ${attempt + 1})", e)
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        Log.i(TAG, "等待后重试...")
                        lastErrorMessage = "网络IO异常，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "网络IO异常: ${e.message}"
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "API调用失败 (尝试 ${attempt + 1})", e)
                    Log.e(TAG, "错误类型: ${e.javaClass.simpleName}")
                    Log.e(TAG, "错误消息: ${e.message}")
                    
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        Log.i(TAG, "等待后重试...")
                        lastErrorMessage = "API调用失败，正在重试...（${attempt + 1}/$MAX_RETRY_ATTEMPTS）"
                        delay(calculateBackoffTime(attempt))
                        continue
                    } else {
                        lastErrorMessage = "API调用失败: ${e.message}"
                    }
                }
            }
            
            // 所有重试都失败
            Log.e(TAG, "API调用失败，已达最大重试次数")
            if (lastException != null) {
                lastException.printStackTrace()
            }
            return@withContext null
            
        } finally {
            // 清理优化后的bitmap（如果和原bitmap不同）
            if (optimizedBitmap != null && optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
                Log.d(TAG, "优化后的bitmap已回收")
            }
            
            // 重置翻译状态，允许新的翻译请求
            isTranslating.set(false)
            Log.d(TAG, "翻译状态已重置，允许新的翻译请求")
        }
    }
    
    /**
     * 构建优化的翻译指令（简化版本以提升速度）
     */
    private fun buildOptimizedTranslationPrompt(): String {
        return """
            识别图中的外语文本并翻译成中文。
            
            格式要求：
            - 每段原文后紧跟中文翻译
            - 段落间用空行分隔
            - 无文本时回复"未检测到文本"
            
            注意：专注于UI文本、菜单、按钮等界面元素。
        """.trimIndent()
    }
    
    /**
     * 构建翻译指令
     */
    private fun buildTranslationPrompt(): String {
        return """
            请仔细分析这张截图中的所有文本内容，并按照以下要求处理：

            1. 识别图片中的所有文字（包括日语、英语、韩语等外语文本）
            2. 将识别到的外语文本翻译成简体中文
            3. 按以下格式返回结果，每段原文后紧跟对应的译文：

            [原文段落1]
            [译文段落1]

            [原文段落2]
            [译文段落2]

            （如有更多段落，继续此格式）

            注意事项：
            - 保持段落的对应关系，一段原文紧跟一段译文
            - 段落之间用空行分隔
            - 如果是游戏界面，请特别注意游戏术语的翻译准确性
            - 如果图片中没有可识别的文字，请回复"未检测到可翻译的文本"
            - 不要添加"原文："、"译文："等标签，直接给出内容

        """.trimIndent()
    }
    
    /**
     * 解析Gemini返回的翻译结果
     */
    private fun parseTranslationResult(response: String?): TranslationResult? {
        if (response.isNullOrBlank()) {
            Log.w(TAG, "响应为空或空白")
            return null
        }
        
        try {
            val trimmedResponse = response.trim()
            Log.d(TAG, "解析翻译结果")
            // Log.d(TAG, "完整结果: $trimmedResponse") // 注释掉，避免重复
            
            // 检查是否包含常见的"无内容"回复
            val noContentPhrases = listOf(
                "未检测到文本", "未检测到可翻译的文本", "没有找到文字", 
                "no text found", "no text detected", "无文字内容"
            )
            
            if (noContentPhrases.any { trimmedResponse.contains(it, ignoreCase = true) }) {
                Log.i(TAG, "检测到无文本内容的回复")
                return TranslationResult(
                    originalText = null,
                    translatedText = "未检测到可翻译的文本内容"
                )
            }
            
            // 统计翻译对的数量
            val sections = trimmedResponse.split("\n\n").filter { it.trim().isNotEmpty() }
            val pairCount = sections.size / 2
            Log.d(TAG, "检测到 $pairCount 对翻译内容")
            
            if (pairCount == 0) {
                Log.w(TAG, "未找到有效的翻译对格式，可能是纯翻译结果")
            }
            
            return TranslationResult(
                originalText = null, // 原文已经混合在翻译结果中
                translatedText = trimmedResponse
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "解析翻译结果失败", e)
            return TranslationResult(
                originalText = null,
                translatedText = response.trim()
            )
        }
    }
    
    /**
     * 将Bitmap转换为ByteArray (备用方法)
     * 注意：Gemini SDK可以直接接受Bitmap对象
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val settings = settingsManager.getSettings()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, settings.compressionQuality, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * 检查API是否可用
     */
    fun isApiAvailable(): Boolean {
        val key = getApiKey()
        val isBlank = key.isBlank()
        val isPlaceholder = key == "YOUR_GEMINI_API_KEY_HERE"
        val isAvailable = !isBlank && !isPlaceholder
        Log.d(TAG, "API可用性检查: isBlank=$isBlank, isPlaceholder=$isPlaceholder, isAvailable=$isAvailable")
        return isAvailable
    }
} 