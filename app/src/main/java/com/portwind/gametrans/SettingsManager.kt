package com.portwind.gametrans

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 设置数据类
 */
data class AppSettings(
    val promptMode: PromptMode = PromptMode.OPTIMIZED,
    val maxImageSize: Int = 2048,  // 进一步提高默认图像尺寸以改善翻译完整性
    val compressionQuality: Int = 85,  // 进一步提高默认压缩质量以保持更多细节
    val customPrompt: String = ""  // 新增：自定义提示词
)

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
        
        // 默认值 - 进一步提高图像质量以改善翻译完整性
        private const val DEFAULT_MAX_IMAGE_SIZE = 2048  // 提高到2048px以获得更好的文字识别
        private const val DEFAULT_COMPRESSION_QUALITY = 95  // 提高到95%以保持更多细节
        
        // 默认自定义提示词（不能使用const val，因为是多行字符串）
        private val DEFAULT_CUSTOM_PROMPT = """
请仔细扫描这张图片，识别外语文本并翻译为简体中文：

【翻译规则】：
- 仅翻译外语：日文、英文、韩文、繁体字
- 翻译目标语言：简体中文
- 跳过简体中文内容

输出格式：
[原文1]
[中文译文1]

[原文2]
[中文译文2]
""".trimIndent()
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
            customPrompt = getCustomPrompt()  // 新增
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
        return sharedPrefs.getString(KEY_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT) ?: DEFAULT_CUSTOM_PROMPT
    }
    

    
    /**
     * 构建翻译提示词
     */
    fun buildTranslationPrompt(): String {
        return when (getPromptMode()) {
            PromptMode.OPTIMIZED -> buildOptimizedPrompt()
            PromptMode.DETAILED -> buildDetailedPrompt()
            PromptMode.CUSTOM -> getCustomPrompt()  // 新增：使用自定义提示词
        }
    }
    
    /**
     * 构建优化的翻译指令（快速模式）
     */
    private fun buildOptimizedPrompt(): String {
        return """
            请仔细扫描这张图片，识别外语文本并翻译为简体中文：
            
            【严格翻译规则】：
            - 仅翻译外语：日文（ひらがな、カタカナ、日文汉字）、英文、韩文（한글）、繁体字
            - 翻译目标语言：简体中文（不是英文！）
            - 绝对不翻译简体中文：包括但不限于"正在截图"、"设置"、"搜索"、"时间显示"等
            - 中文数字、中文标点、中文应用名不要翻译
            
            语言判断标准：
            - 日文特征：ひらがな（あいうえお）、カタカナ（アイウエオ）、日式汉字用法
            - 英文特征：English alphabet letters
            - 韩文特征：한글 characters
            - 中文特征：汉字但使用简体中文语法（跳过这些）
            
            输出格式：
            [原文1]
            ---------
            [中文译文1]
            
            [原文2]
            ---------
            [中文译文2]
            
            执行要求：
            - 必须翻译为简体中文，绝对不要翻译为英文
            - 严格按语言特征判断，不要将中文当外语
            - 只输出确认为外语的文本及其中文翻译
            - 如无外语文本，回复"未检测到需要翻译的文本"
        """.trimIndent()
    }
    
    /**
     * 构建详细的翻译指令（准确模式）
     */
    private fun buildDetailedPrompt(): String {
        return """
            请以最高精度分析这张截图，识别外语文本并翻译为简体中文：

            【核心翻译规则】：
            - 仅翻译外语：日文（ひらがな、カタカナ、日式汉字）、英文、韩文（한글）、繁体字
            - 翻译目标语言：简体中文（绝对不要翻译为英文！）
            - 绝对禁止翻译简体中文：包括"正在截图"、"设置"、"搜索"、"时间"、"GameTrans"等
            - 对于汉字，必须根据语法和用法判断是中文还是日文

            精确语言识别：
            - 日文特征：
              * 假名：ひらがな（あいうえお）、カタカナ（アイウエオ）
              * 日式汉字用法：如"道路交通情報"、"フォロワー"、"自動車"
              * 日式语法结构
            - 英文特征：English alphabet, abbreviations
            - 韩文特征：한글 characters（如 한국어）
            - 简体中文特征：汉字 + 简体语法（这些要跳过）

            全面扫描区域：
            1. 主要内容：标题、正文、按钮文字、菜单项
            2. 界面元素：状态栏、导航栏、工具栏、标签页
            3. 小字体内容：提示信息、版权信息、时间戳、版本号
            4. 交互元素：对话框、弹窗、下拉菜单、选项卡
            5. 图标文字：徽章数字、图标标签、快捷方式名称
            6. 边缘内容：页脚、侧边栏、浮动元素
            7. 游戏专属：血量、等级、技能名、道具名、NPC对话

            输出格式：
            [完整原文1]
            [简体中文译文1]

            [完整原文2]
            [简体中文译文2]

            执行要求：
            - 必须翻译为简体中文，严禁翻译为英文
            - 严格语言识别，绝对不要将中文当外语翻译
            - 按图片顺序排列（从上到下，从左到右）
            - 保持原文完整性，不分割或合并文本
            - 游戏术语使用标准中文翻译
            - 专有名词保持准确性
            - 确保识别所有外语文字
            - 如无外语文字，回复"未检测到可翻译的文本"

        """.trimIndent()
    }
} 