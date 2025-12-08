package com.portwind.gametrans

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * 文本转语音 API 管理器（Cloudflare Worker - VoiceCraft）
 * 端点文档参考：/v1/audio/speech
 * 示例部署域名： https://tts-voice-magic.zl282145321.workers.dev
 */
class TtsApiManager(private val context: Context) {

    companion object {
        private const val TAG = "TtsApiManager"
        private const val BASE_URL = "https://tts-voice-magic.zl282145321.workers.dev"
        private const val ENDPOINT = "/v1/audio/speech"
    }

    data class TtsConfig(
        val voice: String = "zh-CN-XiaoxiaoNeural",
        val speed: Double = 1.0,
        val pitch: String = "0",
        val style: String = "general"
    )

    private val httpClient: OkHttpClient = OkHttpClient()

    /**
     * 合成语音为临时 MP3 文件，调用成功返回文件，否则返回 null。
     */
    suspend fun synthesizeToFile(text: String, config: TtsConfig = TtsConfig()): File? = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext null

            val json = JSONObject().apply {
                put("input", text)
                put("voice", config.voice)
                put("speed", config.speed)
                put("pitch", config.pitch)
                put("style", config.style)
            }

            val media = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(media)
            val request = Request.Builder()
                .url(BASE_URL + ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "TTS request failed: ${'$'}{resp.code} ${'$'}{resp.message}")
                    return@withContext null
                }
                val bytes = resp.body?.bytes()
                if (bytes == null || bytes.isEmpty()) return@withContext null

                val outFile = File(context.cacheDir, "tts_${'$'}{System.currentTimeMillis()}.mp3")
                FileOutputStream(outFile).use { fos ->
                    fos.write(bytes)
                    fos.flush()
                }
                return@withContext outFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "synthesizeToFile error", e)
            null
        }
    }
}


