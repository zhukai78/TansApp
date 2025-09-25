package com.portwind.gametrans

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Qwen3-Omni API 管理器（DashScope 兼容）
 * 说明：本实现按“多模态对话”接口风格发送 1 张图片 + 文本提示词
 */
class QwenApiManager(private val context: Context) {

    companion object {
        private const val TAG = "QwenApiManager"
        private const val ENDPOINT = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        private const val MODEL_NAME = "qwen3-omni"
    }

    private val settingsManager = SettingsManager(context)
    private val httpClient: OkHttpClient = OkHttpClient()

    /**
     * 翻译/理解屏幕截图（图片+提示词）
     */
    suspend fun translateImage(bitmap: Bitmap, promptOverride: String? = null): TranslationResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.QWEN_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "Qwen API key missing")
            return@withContext null
        }

        try {
            val prompt = promptOverride ?: settingsManager.buildTranslationPrompt()

            // 将 Bitmap 压缩为 JPEG 并 Base64 编码（data URI）
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, settingsManager.getCompressionQuality(), baos)
            val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$imageBase64"

            // DashScope 多模态请求格式（messages）
            val imageContent = JSONObject().apply {
                put("image", JSONObject().put("image_url", dataUri))
            }
            val textContent = JSONObject().apply { put("text", prompt) }
            val contentArr = JSONArray().apply {
                put(imageContent)
                put(textContent)
            }
            val userMsg = JSONObject().apply {
                put("role", "user")
                put("content", contentArr)
            }
            val messages = JSONArray().put(userMsg)

            val reqJson = JSONObject().apply {
                put("model", MODEL_NAME)
                put("input", JSONObject().put("messages", messages))
            }

            val media = "application/json; charset=utf-8".toMediaType()
            val body = reqJson.toString().toRequestBody(media)
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Qwen request failed: ${'$'}{resp.code} ${'$'}{resp.message}")
                    return@withContext null
                }
                val respStr = resp.body?.string()?.trim()
                if (respStr.isNullOrBlank()) return@withContext null

                // 解析返回，优先尝试通用字段
                // 参考：{"output":{"text":"..."}} 或 chat 风格的 choices/messages
                return@withContext parseQwenResponse(respStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Qwen translateImage failed", e)
            return@withContext null
        }
    }

    private fun parseQwenResponse(json: String): TranslationResult? {
        return try {
            val obj = JSONObject(json)

            // 方案1：output.text 直接给文本
            val text1 = obj.optJSONObject("output")?.optString("text")
            if (!text1.isNullOrBlank()) {
                return TranslationResult(originalText = null, translatedText = text1)
            }

            // 方案2：choices[0].message.content[].text/image 等
            val choices = obj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.optJSONObject(0)?.optJSONObject("message")
                val contentArr = msg?.optJSONArray("content")
                if (contentArr != null) {
                    val sb = StringBuilder()
                    for (i in 0 until contentArr.length()) {
                        val part = contentArr.optJSONObject(i)
                        val t = part?.optString("text")
                        if (!t.isNullOrBlank()) sb.appendLine(t)
                    }
                    val text = sb.toString().trim()
                    if (text.isNotBlank()) {
                        return TranslationResult(originalText = null, translatedText = text)
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "parseQwenResponse error", e)
            null
        }
    }
}


