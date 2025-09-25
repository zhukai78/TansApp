package com.portwind.gametrans

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
 * 多场景AI任务类型
 * 说明：本应用不仅用于翻译，也作为手机端的通用AI助手。
 * 这些模板均默认配合截图/图片输入，面向不同使用场景做过指令工程优化。
 */
enum class AiTask(val displayName: String, val description: String) {
    // 翻译相关（复用现有构建逻辑）
    TRANSLATE_OPTIMIZED("快速翻译", "面向实时的轻量级翻译，速度优先"),
    TRANSLATE_DETAILED("精准翻译", "更全面的识别与翻译，准确度优先"),

    // 新增：通用AI场景
    SUMMARIZE_SCREEN("屏幕摘要", "对当前截图内容做中文要点总结"),
    REPHRASE_TO_CN("中文润色重写", "将图片中的中文文本润色、优化表达"),
    TLDR_KEYPOINTS("TL;DR 要点", "输出三到五条极简要点"),

    // 新增：专业领域AI任务
    ANALYZE_MEDICAL_IMAGE("医疗影像分析", "解读X光/CT等影像（仅供参考）"),
    ANALYZE_CHART("图表解读", "分析图表数据与趋势"),
    IDENTIFY_PLANT_ANIMAL("动植物识别", "识别图片中的动植物"),
    IDENTIFY_DISH_AND_RECIPE("菜肴识别与菜谱", "识别菜肴并生成参考菜谱"),
    INGREDIENTS_ANALYSIS("配料表分析", "简短说明配料表并提示潜在有害物质"),
    CALORIE_ANALYSIS("卡路里分析", "从图片/包装信息估算能量与营养构成")
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
        
        // 默认自定义提示词（不能使用const val，因为是多行字符串）
        private val DEFAULT_CUSTOM_PROMPT = """
请仔细扫描这张图片，识别外语文本并翻译为简体中文：

【翻译规则】：
- 仅翻译外语：日文、英文、韩文、繁体字
- 翻译目标语言：简体中文
- 跳过简体中文内容
- 跳过时间格式内容

输出格式：
[原文1]
[中文译文1]

[原文2]
[中文译文2]
""".trimIndent()
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
        return sharedPrefs.getString(KEY_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT) ?: DEFAULT_CUSTOM_PROMPT
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
     * 构建翻译提示词
     */
    fun buildTranslationPrompt(): String {
        return when (getPromptMode()) {
            PromptMode.OPTIMIZED -> buildOptimizedPrompt()
            PromptMode.DETAILED -> buildDetailedPrompt()
            PromptMode.CUSTOM -> getCustomPrompt()  // 新增：使用自定义提示词
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
     * 统一构建：按任务类型返回相应Prompt
     * 不更改现有调用者：翻译仍走 buildTranslationPrompt()
     */
    fun buildPrompt(task: AiTask): String {
        return when (task) {
            // 翻译（沿用既有实现）
            AiTask.TRANSLATE_OPTIMIZED -> buildOptimizedPrompt()
            AiTask.TRANSLATE_DETAILED -> buildDetailedPrompt()

            // 通用AI任务
            AiTask.SUMMARIZE_SCREEN -> buildSummarizeScreenPrompt()
            AiTask.REPHRASE_TO_CN -> buildRephraseToCnPrompt()
            AiTask.TLDR_KEYPOINTS -> buildTldrPrompt()

            // 专业AI任务
            AiTask.ANALYZE_MEDICAL_IMAGE -> buildAnalyzeMedicalImagePrompt()
            AiTask.ANALYZE_CHART -> buildAnalyzeChartPrompt()
            AiTask.IDENTIFY_PLANT_ANIMAL -> buildIdentifyPlantAnimalPrompt()
            AiTask.IDENTIFY_DISH_AND_RECIPE -> buildIdentifyDishAndRecipePrompt()
            AiTask.INGREDIENTS_ANALYSIS -> buildIngredientsAnalysisPrompt()
            AiTask.CALORIE_ANALYSIS -> buildCalorieAnalysisPrompt()
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
            - 跳过时间格式内容

            语言判断标准：
            - 日文特征：ひらがな（あいうえお）、カタカナ（アイウエオ）、日式汉字用法
            - 英文特征：English alphabet letters
            - 中文特征：汉字但使用简体中文语法（跳过这些）
            
            输出格式：
            [原文]
            ---------
            [中文译文]
        
            
            执行要求：
            - 必须翻译为简体中文，绝对不要翻译为英文
            - 严格按语言特征判断，不要将中文当外语
            - 只输出确认为外语的文本及其中文翻译
            - 如无外语文本，回复"未检测到需要翻译的文本"
            - 跳过时间格式内容

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
            3. 小字体内容：提示信息
            4. 交互元素：对话框、弹窗、下拉菜单、选项卡
            5. 边缘内容：页脚、侧边栏、浮动元素
            6. 游戏专属：技能名、道具名、NPC对话

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

    // ============================
    // 通用AI任务模板（新增）
    // ============================

    /** 屏幕摘要：面向中文用户的要点总结 */
    private fun buildSummarizeScreenPrompt(): String {
        return """
            请阅读这张截图并给出中文摘要：
            - 用3-5条要点说明核心信息（TL;DR）
            - 标注重要数值、日期或关键词
            - 若为应用界面，概述其主要功能与当前状态
            - 如缺少关键信息，请指出不确定性
        """.trimIndent()
    }

    /** 中文润色：改善表达与可读性 */
    private fun buildRephraseToCnPrompt(): String {
        return """
            将图片中的中文文本进行润色重写：
            - 保留原意与关键信息
            - 改善语法、用词与流畅度
            - 输出更自然的中文表达，保留项目/术语专有名
        """.trimIndent()
    }

    /** TL;DR 简报：极简要点 */
    private fun buildTldrPrompt(): String {
        return """
            请对图片内容给出 TL;DR：
            - 3-5条极简要点，每条不超过20字
            - 覆盖目的、状态、关键动作或数据
        """.trimIndent()
    }

    // ============================
    // 专业领域AI任务模板（新增）
    // ============================

    /** 医疗影像分析：解读X光/CT等影像（带免责声明） */
    private fun buildAnalyzeMedicalImagePrompt(): String {
        return """
            【请注意：AI分析仅供参考，不能替代专业医生诊断】

            请扮演一位经验丰富的放射科医生，对这张医疗影像（如X光、CT、MRI）进行初步分析：
            1.  **影像类型与部位**：这是什么类型的影像？检查的是哪个身体部位？
            2.  **主要发现**：描述你观察到的任何异常区域，包括其位置、大小、形状、密度和边界特征。
            3.  **初步见解**：基于发现，提出最可能的几种情况或诊断方向。
            4.  **建议与后续步骤**：建议进行哪些进一步的检查（如增强扫描、活检）或应咨询哪个科室的医生。
            5.  **正常结构**：简要说明看到的正常解剖结构。

            输出格式应清晰、分点，并使用通俗易懂的中文。始终在结尾重申“本分析仅为AI根据图像得出的初步建议，请务必咨询执业医师获取正式诊断报告”。
        """.trimIndent()
    }

    /** 图表解读：深入分析数据、趋势和洞察 */
    private fun buildAnalyzeChartPrompt(): String {
        return """
            请作为一名数据分析师，深入解读这张图表：
            1.  **图表类型与主题**：这是什么图表（如折线图、柱状图）？它在展示什么核心主题？
            2.  **关键数据点**：列出图表中的最高点、最低点、转折点或任何异常值，并说明其数值。
            3.  **核心趋势与模式**：描述数据随时间/分类的变化趋势（例如，持续增长、周期性波动、下降后趋于平稳）。
            4.  **数据洞察**：根据图表信息，你能得出什么结论或洞察？这可能意味着什么？
            5.  **潜在问题**：图表是否存在误导性（如坐标轴起点不为0）？或者缺少哪些信息会影响结论？

            请用简洁的中文分点阐述。
        """.trimIndent()
    }

    /** 动植物识别与科普 */
    private fun buildIdentifyPlantAnimalPrompt(): String {
        return """
            请识别图片中的动物或植物：
            1.  **物种名称**：给出最可能的物种名称（学名和中文名）。
            2.  **核心特征**：描述它的主要外观特征，以佐证你的识别。
            3.  **趣味科普**：提供一些关于该物种的有趣信息，如栖息地、习性、保护状况等。
            4.  **置信度**：评估你识别的准确度（高/中/低），如果不确定，请说明原因。
        """.trimIndent()
    }

    /** 菜肴识别与菜谱生成 */
    private fun buildIdentifyDishAndRecipePrompt(): String {
        return """
            请识别图片中的菜肴，并提供一份家庭简易版菜谱：
            1.  **菜肴名称**：最可能是什么菜？
            2.  **风味特点**：简要描述它的口味（如麻辣、酸甜）。
            3.  **所需食材**：列出主要的食材和调味料。
            4.  **制作步骤**：提供一份清晰、分步的家庭烹饪指南。
            5.  **小贴士**：分享1-2个让这道菜更美味的小技巧。
        """.trimIndent()
    }

    /** 配料表分析：简短输出与风险提示（新增） */
    private fun buildIngredientsAnalysisPrompt(): String {
        return """
            请阅读图片中的食品/化妆品等商品配料表，给出非常简短的中文说明：
            - 先用1-2句话概括配料构成与大致健康度
            - 如含潜在有害/争议成分（如反式脂肪、人工色素、防腐剂、对羟基苯甲酸酯、甲醛释放体、重金属等），务必明确点出并用简短语言解读其风险与常见建议（如“孕妇/儿童慎用”、“长期大量摄入不宜”）
            - 若无明显风险，简要说明“未发现常见高风险成分”
            - 输出简洁、避免长段落
        """.trimIndent()
    }

    /** 卡路里分析：简洁能量与营养估算（新增） */
    private fun buildCalorieAnalysisPrompt(): String {
        return """
            请基于图片中的营养成分表/包装信息，对热量进行非常简短的中文分析：
            - 给出能量（kcal）最简明结论：每100g/每份（如能识别）与图中份量的粗略估算
            - 简述主要能量来源（碳水/脂肪/蛋白质）占比或倾向性（如可识别）
            - 若糖分/饱和脂肪/反式脂肪/钠偏高，请用简短语句标注健康提示（如“控制摄入”）
            - 不确定时请标注“约/可能”，输出控制在2-4行
        """.trimIndent()
    }
} 