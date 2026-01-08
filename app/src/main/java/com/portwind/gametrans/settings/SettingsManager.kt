package com.portwind.gametrans.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

/**
 * 设置数据类
 */
data class AppSettings(
    val promptMode: PromptMode = PromptMode.OPTIMIZED,
    val maxImageSize: Int = 1024,  // 优化性能：降低默认图像尺寸以加快API调用速度
    val compressionQuality: Int = 85,  // 平衡质量与性能的压缩质量设置
    val customPrompt: String = "",  // 新增：自定义提示词
    val language: AppLanguage = AppLanguage.CHINESE,  // 新增：语言设置
    val modelProvider: ModelProvider = ModelProvider.GEMINI  // 新增：模型提供商
)

/**
 * 应用语言
 */
enum class AppLanguage(val code: String, val displayName: String) {
    CHINESE("zh", "中文"),
    ENGLISH("en", "English")
}

/** 模型提供商 */
enum class ModelProvider(val displayName: String) {
    GEMINI("Gemini"),
    QWEN("Qwen3-Omni")
}

/**
 * 提示词模式
 */
enum class PromptMode(val displayName: String, val description: String) {
    OPTIMIZED("优化模式", "快速处理，适合实时翻译"),
    DETAILED("详细模式", "准确度更高，处理时间稍长"),
    CUSTOM("自定义模式", "使用自定义提示词")  // 新增
}

/**
 * 设置管理器
 * 负责存储和管理应用的配置设置
 */
class SettingsManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "gametrans_settings"
        
        // 设置键名
        private const val KEY_PROMPT_MODE = "prompt_mode"
        private const val KEY_MAX_IMAGE_SIZE = "max_image_size"
        private const val KEY_COMPRESSION_QUALITY = "compression_quality"
        private const val KEY_CUSTOM_PROMPT = "custom_prompt"  // 新增
        private const val KEY_LANGUAGE = "language"  // 新增：语言设置
        private const val KEY_MODEL_PROVIDER = "model_provider"  // 新增：模型提供商
        
        // 默认值 - 平衡性能与质量的优化设置
        private const val DEFAULT_MAX_IMAGE_SIZE = 1024  // 优化为1024px以加快API处理速度
        private const val DEFAULT_COMPRESSION_QUALITY = 85  // 85%压缩质量平衡文件大小与清晰度
        
    }
    
    /** 设置模型提供商 */
    fun setModelProvider(provider: ModelProvider) {
        sharedPrefs.edit().putString(KEY_MODEL_PROVIDER, provider.name).apply()
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取当前设置
     */
    fun getSettings(): AppSettings {
        return AppSettings(
            promptMode = getPromptMode(),
            maxImageSize = getMaxImageSize(),
            compressionQuality = getCompressionQuality(),
            customPrompt = getCustomPrompt(),  // 新增
            language = getLanguage(),  // 新增：语言设置
            modelProvider = getModelProvider()  // 新增：模型提供商
        )
    }
    
    /**
     * 保存设置
     */
    fun saveSettings(settings: AppSettings) {
        sharedPrefs.edit().apply {
            putString(KEY_PROMPT_MODE, settings.promptMode.name)
            putInt(KEY_MAX_IMAGE_SIZE, settings.maxImageSize)
            putInt(KEY_COMPRESSION_QUALITY, settings.compressionQuality)
            putString(KEY_CUSTOM_PROMPT, settings.customPrompt)  // 新增
            putString(KEY_LANGUAGE, settings.language.name)  // 新增：语言设置
            putString(KEY_MODEL_PROVIDER, settings.modelProvider.name)  // 新增：模型提供商
            apply()
        }
        Log.d(TAG, "设置已保存: $settings")
    }
    
    /**
     * 重置为默认设置
     */
    fun resetToDefaults() {
        val defaultSettings = AppSettings()
        saveSettings(defaultSettings)
        Log.d(TAG, "设置已重置为默认值")
    }
    
    /**
     * 获取提示词模式
     */
    fun getPromptMode(): PromptMode {
        val modeName = sharedPrefs.getString(KEY_PROMPT_MODE, PromptMode.OPTIMIZED.name)
        return try {
            PromptMode.valueOf(modeName ?: PromptMode.OPTIMIZED.name)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "无效的提示词模式: $modeName, 使用默认值")
            PromptMode.OPTIMIZED
        }
    }
    
    /**
     * 获取最大图像尺寸
     */
    fun getMaxImageSize(): Int {
        return sharedPrefs.getInt(KEY_MAX_IMAGE_SIZE, DEFAULT_MAX_IMAGE_SIZE)
    }
    
    /**
     * 获取压缩质量
     */
    fun getCompressionQuality(): Int {
        return sharedPrefs.getInt(KEY_COMPRESSION_QUALITY, DEFAULT_COMPRESSION_QUALITY)
    }
    
    /**
     * 获取自定义提示词
     */
    fun getCustomPrompt(): String {
        return sharedPrefs.getString(KEY_CUSTOM_PROMPT, PromptTemplates.defaultCustomPrompt) 
            ?: PromptTemplates.defaultCustomPrompt
    }
    
    /**
     * 获取语言设置
     */
    fun getLanguage(): AppLanguage {
        val languageName = sharedPrefs.getString(KEY_LANGUAGE, AppLanguage.CHINESE.name)
        return try {
            AppLanguage.valueOf(languageName ?: AppLanguage.CHINESE.name)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "无效的语言设置: $languageName, 使用默认值")
            AppLanguage.CHINESE
        }
    }
    
    /**
     * 设置应用语言
     */
    fun setLanguage(language: AppLanguage) {
        sharedPrefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        applyLanguage(language)
    }
    
    /**
     * 应用语言设置
     */
    fun applyLanguage(language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    

    
    /**
     * 构建翻译提示词（根据当前设置的模式）
     */
    fun buildTranslationPrompt(): String {
        return when (getPromptMode()) {
            PromptMode.OPTIMIZED -> PromptTemplates.optimizedTranslation
            PromptMode.DETAILED -> PromptTemplates.detailedTranslation
            PromptMode.CUSTOM -> getCustomPrompt()
        }
    }

    /** 获取模型提供商 */
    fun getModelProvider(): ModelProvider {
        val name = sharedPrefs.getString(KEY_MODEL_PROVIDER, ModelProvider.GEMINI.name)
        return try {
            ModelProvider.valueOf(name ?: ModelProvider.GEMINI.name)
        } catch (_: Exception) {
            ModelProvider.GEMINI
        }
    }

    /**
     * 统一构建：按任务类型返回相应 Prompt
     * 委托给 PromptTemplates
     */
    fun buildPrompt(task: AiTask): String {
        return PromptTemplates.getPrompt(task)
    }
} 