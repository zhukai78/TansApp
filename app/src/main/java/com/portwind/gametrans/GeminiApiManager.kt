package com.portwind.gametrans

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

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
 * Gemini API管理器
 * 负责处理图像识别和翻译功能
 */
class GeminiApiManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GeminiApiManager"
        private const val MODEL_NAME = "gemini-2.5-flash"
        // TODO: 将API密钥配置到local.properties中
        private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    }
    
    private val settingsManager = SettingsManager(context)
    
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = getApiKey()
        )
    }
    
    /**
     * 获取API密钥
     * 从BuildConfig获取，确保密钥安全
     */
    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            Log.d(TAG, "API密钥已获取")
            key
        } catch (e: Exception) {
            Log.e(TAG, "无法获取API密钥，请检查local.properties配置", e)
            ""
        }
    }
    
//    /**
//     * 优化图像以提升API处理速度
//     * @param bitmap 原始图像
//     * @return 优化后的图像
//     */
//    private fun optimizeImage(bitmap: Bitmap): Bitmap {
//        val settings = settingsManager.getSettings()
//        val startTime = System.currentTimeMillis()
//        Log.d(TAG, "开始优化图像: ${bitmap.width}x${bitmap.height}")
//
//        // 1. 计算缩放比例
//        val maxDimension = max(bitmap.width, bitmap.height)
//        val scaleFactor = if (maxDimension > settings.maxImageSize) {
//            settings.maxImageSize.toFloat() / maxDimension
//        } else {
//            1.0f
//        }
//
//        val optimizedBitmap = if (scaleFactor < 1.0f) {
//            // 2. 缩放图像
//            val newWidth = (bitmap.width * scaleFactor).toInt()
//            val newHeight = (bitmap.height * scaleFactor).toInt()
//
//            Log.d(TAG, "缩放图像到: ${newWidth}x${newHeight} (缩放比例: ${String.format("%.2f", scaleFactor)})")
//
//            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
//        } else {
//            Log.d(TAG, "图像尺寸已符合要求，无需缩放")
//            bitmap
//        }
//
//        val endTime = System.currentTimeMillis()
//        Log.d(TAG, "图像优化完成，耗时: ${endTime - startTime}ms")
//        Log.d(TAG, "优化后尺寸: ${optimizedBitmap.width}x${optimizedBitmap.height}")
//
//        return optimizedBitmap
//    }
    
    /**
     * 使用Gemini进行图像翻译
     * @param bitmap 要翻译的屏幕截图
     * @return 翻译结果，包含原文和译文
     */
    suspend fun translateImage(bitmap: Bitmap): TranslationResult? = withContext(Dispatchers.IO) {
        var optimizedBitmap: Bitmap? = null
        try {
            Log.d(TAG, "开始调用Gemini API进行图像翻译...")
            Log.d(TAG, "原始图像尺寸: ${bitmap.width}x${bitmap.height}")
            
            // 优化图像以提升处理速度
//            optimizedBitmap = optimizeImage(bitmap)
            Log.d(TAG, "图像优化完成")
            
            // 构建翻译指令
            val prompt = settingsManager.buildTranslationPrompt()
            Log.d(TAG, "翻译指令构建完成")
            
            // 构建请求内容 - 使用优化后的图像
            Log.d(TAG, "构建API请求内容...")
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            Log.d(TAG, "API请求内容构建完成，开始发送请求...")
            
            // 调用Gemini API
            val response = generativeModel.generateContent(inputContent)
            Log.d(TAG, "API响应接收完成，开始解析...")
            
            val result = response.text
            
            if (result.isNullOrBlank()) {
                Log.w(TAG, "API返回空结果")
                return@withContext null
            }
            
            Log.d(TAG, "API响应获取成功")
            Log.d(TAG, "响应长度: ${result.length} 字符")
            // Log.d(TAG, "原始响应: $result") // 注释掉，避免重复
            
            // 分析响应内容，检查是否可能不完整
            val lineCount = result.lines().size
            val wordCount = result.split("\\s+".toRegex()).size
            Log.d(TAG, "响应统计: $lineCount 行, $wordCount 词")
            
            if (result.contains("...") || result.contains("省略") || result.contains("truncated")) {
                Log.w(TAG, "检测到可能的不完整响应标志")
            }
            
            // 解析结果，提取原文和译文
            val translationResult = parseTranslationResult(result)
            Log.d(TAG, "翻译结果解析完成")
            return@withContext translationResult
            
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Gemini API调用超时", e)
            return@withContext null
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "网络连接失败，请检查网络状态", e)
            return@withContext null
        } catch (e: java.io.IOException) {
            Log.e(TAG, "网络IO异常", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API调用失败", e)
            Log.e(TAG, "错误类型: ${e.javaClass.simpleName}")
            Log.e(TAG, "错误消息: ${e.message}")
            e.printStackTrace()
            return@withContext null
        } finally {
            // 清理优化后的bitmap（如果和原bitmap不同）
            if (optimizedBitmap != null && optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
                Log.d(TAG, "优化后的bitmap已回收")
            }
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
        val isAvailable = key.isNotBlank() && key != API_KEY && key != "YOUR_GEMINI_API_KEY_HERE"
        Log.d(TAG, "API可用性: $isAvailable")
        return isAvailable
    }
} 