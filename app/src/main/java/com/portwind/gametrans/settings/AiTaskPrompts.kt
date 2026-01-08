package com.portwind.gametrans.settings

/**
 * 多场景AI任务类型
 * 说明：本应用不仅用于翻译，也作为手机端的通用AI助手。
 * 这些模板均默认配合截图/图片输入，面向不同使用场景做过指令工程优化。
 */
enum class AiTask(
    val displayName: String,
    val description: String,
    val category: TaskCategory
) {
    // ========== 翻译 ==========
    TRANSLATE_OPTIMIZED("快速翻译", "面向实时的轻量级翻译，速度优先", TaskCategory.TRANSLATION),
    TRANSLATE_DETAILED("精准翻译", "更全面的识别与翻译，准确度优先", TaskCategory.TRANSLATION),

    // ========== 通用AI ==========
    SUMMARIZE_SCREEN("屏幕摘要", "生成屏幕内容的中文摘要", TaskCategory.GENERAL),
    REPHRASE_TO_CN("中文润色重写", "将图片中的中文文本润色、优化表达", TaskCategory.GENERAL),
    TLDR_KEYPOINTS("TL;DR 要点", "输出三到五条极简要点", TaskCategory.GENERAL),

    // ========== 专业领域 ==========
    ANALYZE_MEDICAL_IMAGE("医疗影像分析", "解读X光/CT等影像（仅供参考）", TaskCategory.PROFESSIONAL),
    ANALYZE_CHART("图表解读", "分析图表数据与趋势", TaskCategory.PROFESSIONAL),
    IDENTIFY_PLANT_ANIMAL("动植物识别", "识别图片中的动植物", TaskCategory.PROFESSIONAL),
    IDENTIFY_DISH_AND_RECIPE("菜肴识别与菜谱", "识别菜肴并生成参考菜谱", TaskCategory.PROFESSIONAL),
    INGREDIENTS_ANALYSIS("配料表分析", "简短说明配料表并提示潜在有害物质", TaskCategory.PROFESSIONAL),
    CALORIE_ANALYSIS("卡路里分析", "从图片/包装信息估算能量与营养构成", TaskCategory.PROFESSIONAL),

    // ========== 创意与趣味 ==========
    REVERSE_PROMPT("提示词反推", "分析画面生成AI绘画提示词", TaskCategory.CREATIVE),
    MEME_EXPLAINER("梗图详解", "解释表情包笑点与来源", TaskCategory.CREATIVE),

    // ========== 知识与学习 ==========
    SOLVE_MATH("数学求解", "识别题目并给出解题步骤", TaskCategory.LEARNING),
    EXPLAIN_CODE("代码解释", "识别代码片段并解释其功能", TaskCategory.LEARNING),

    // ========== 生活助手 ==========
    GAME_HINT("游戏攻略", "分析游戏画面给出建议", TaskCategory.LIFESTYLE),
    TRAVEL_GUIDE("旅游导游", "识别地标介绍景点信息", TaskCategory.LIFESTYLE);

    companion object {
        /** 按分类分组返回 */
        fun groupedByCategory(): Map<TaskCategory, List<AiTask>> {
            return values().groupBy { it.category }
        }
    }
}

/**
 * 任务分类（用于 UI 分组展示）
 */
enum class TaskCategory(val displayName: String) {
    TRANSLATION("翻译"),
    GENERAL("通用"),
    PROFESSIONAL("专业领域"),
    CREATIVE("创意趣味"),
    LEARNING("知识学习"),
    LIFESTYLE("生活助手")
}

/**
 * 提示词模板管理
 * 集中管理所有 AI 任务的提示词模板
 */
object PromptTemplates {

    /**
     * 根据任务类型获取对应的提示词
     */
    fun getPrompt(task: AiTask): String {
        return when (task) {
            // 翻译
            AiTask.TRANSLATE_OPTIMIZED -> optimizedTranslation
            AiTask.TRANSLATE_DETAILED -> detailedTranslation

            // 通用
            AiTask.SUMMARIZE_SCREEN -> summarizeScreen
            AiTask.REPHRASE_TO_CN -> rephraseToCn
            AiTask.TLDR_KEYPOINTS -> tldrKeypoints

            // 专业领域
            AiTask.ANALYZE_MEDICAL_IMAGE -> analyzeMedicalImage
            AiTask.ANALYZE_CHART -> analyzeChart
            AiTask.IDENTIFY_PLANT_ANIMAL -> identifyPlantAnimal
            AiTask.IDENTIFY_DISH_AND_RECIPE -> identifyDishAndRecipe
            AiTask.INGREDIENTS_ANALYSIS -> ingredientsAnalysis
            AiTask.CALORIE_ANALYSIS -> calorieAnalysis

            // 创意趣味
            AiTask.REVERSE_PROMPT -> reversePrompt
            AiTask.MEME_EXPLAINER -> memeExplainer

            // 知识学习
            AiTask.SOLVE_MATH -> solveMath
            AiTask.EXPLAIN_CODE -> explainCode

            // 生活助手
            AiTask.GAME_HINT -> gameHint
            AiTask.TRAVEL_GUIDE -> travelGuide
        }
    }

    // ============================
    // 翻译模板
    // ============================

    val optimizedTranslation = """
        识别图中外语(日/英/韩/繁体)翻译为简体中文。
        跳过已是简体中文的内容。
        输出格式：
        [原文1]
        [译文1]

        [原文2]
        [译文2]
        无外语则回复"未检测到"
    """.trimIndent()

    val detailedTranslation = """
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

    // ============================
    // 通用AI任务模板
    // ============================

    val summarizeScreen = """
        请阅读这张截图并给出中文摘要：
        - 用3-5条要点说明核心信息（TL;DR）
        - 标注重要数值、日期或关键词
        - 若为应用界面，概述其主要功能与当前状态
        - 如缺少关键信息，请指出不确定性
    """.trimIndent()

    val rephraseToCn = """
        将图片中的中文文本进行润色重写：
        - 保留原意与关键信息
        - 改善语法、用词与流畅度
        - 输出更自然的中文表达，保留项目/术语专有名
    """.trimIndent()

    val tldrKeypoints = """
        请对图片内容给出 TL;DR：
        - 3-5条极简要点，每条不超过20字
        - 覆盖目的、状态、关键动作或数据
    """.trimIndent()

    // ============================
    // 专业领域AI任务模板
    // ============================

    val analyzeMedicalImage = """
        【请注意：AI分析仅供参考，不能替代专业医生诊断】

        请扮演一位经验丰富的放射科医生，对这张医疗影像（如X光、CT、MRI）进行初步分析：
        1.  **影像类型与部位**：这是什么类型的影像？检查的是哪个身体部位？
        2.  **主要发现**：描述你观察到的任何异常区域，包括其位置、大小、形状、密度和边界特征。
        3.  **初步见解**：基于发现，提出最可能的几种情况或诊断方向。
        4.  **建议与后续步骤**：建议进行哪些进一步的检查（如增强扫描、活检）或应咨询哪个科室的医生。
        5.  **正常结构**：简要说明看到的正常解剖结构。

        输出格式应清晰、分点，并使用通俗易懂的中文。始终在结尾重申"本分析仅为AI根据图像得出的初步建议，请务必咨询执业医师获取正式诊断报告"。
    """.trimIndent()

    val analyzeChart = """
        请作为一名数据分析师，深入解读这张图表：
        1.  **图表类型与主题**：这是什么图表（如折线图、柱状图）？它在展示什么核心主题？
        2.  **关键数据点**：列出图表中的最高点、最低点、转折点或任何异常值，并说明其数值。
        3.  **核心趋势与模式**：描述数据随时间/分类的变化趋势（例如，持续增长、周期性波动、下降后趋于平稳）。
        4.  **数据洞察**：根据图表信息，你能得出什么结论或洞察？这可能意味着什么？
        5.  **潜在问题**：图表是否存在误导性（如坐标轴起点不为0）？或者缺少哪些信息会影响结论？

        请用简洁的中文分点阐述。
    """.trimIndent()

    val identifyPlantAnimal = """
        请识别图片中的动物或植物：
        1.  **物种名称**：给出最可能的物种名称（学名和中文名）。
        2.  **核心特征**：描述它的主要外观特征，以佐证你的识别。
        3.  **趣味科普**：提供一些关于该物种的有趣信息，如栖息地、习性、保护状况等。
        4.  **置信度**：评估你识别的准确度（高/中/低），如果不确定，请说明原因。
    """.trimIndent()

    val identifyDishAndRecipe = """
        请识别图片中的菜肴，并提供一份家庭简易版菜谱：
        1.  **菜肴名称**：最可能是什么菜？
        2.  **风味特点**：简要描述它的口味（如麻辣、酸甜）。
        3.  **所需食材**：列出主要的食材和调味料。
        4.  **制作步骤**：提供一份清晰、分步的家庭烹饪指南。
        5.  **小贴士**：分享1-2个让这道菜更美味的小技巧。
    """.trimIndent()

    val ingredientsAnalysis = """
        请阅读图片中的食品/化妆品等商品配料表，给出非常简短的中文说明：
        - 先用1-2句话概括配料构成与大致健康度
        - 如含潜在有害/争议成分（如反式脂肪、人工色素、防腐剂、对羟基苯甲酸酯、甲醛释放体、重金属等），务必明确点出并用简短语言解读其风险与常见建议（如"孕妇/儿童慎用"、"长期大量摄入不宜"）
        - 若无明显风险，简要说明"未发现常见高风险成分"
        - 输出简洁、避免长段落
    """.trimIndent()

    val calorieAnalysis = """
        请基于图片中的营养成分表/包装信息，对热量进行非常简短的中文分析：
        - 给出能量（kcal）最简明结论：每100g/每份（如能识别）与图中份量的粗略估算
        - 简述主要能量来源（碳水/脂肪/蛋白质）占比或倾向性（如可识别）
        - 若糖分/饱和脂肪/反式脂肪/钠偏高，请用简短语句标注健康提示（如"控制摄入"）
        - 不确定时请标注"约/可能"，输出控制在2-4行
    """.trimIndent()

    // ============================
    // 创意与趣味任务模板
    // ============================

    val reversePrompt = """
        请仔细分析这张图片，反推出可以生成类似图像的AI绘画提示词（Prompt）：
        1.  **画面描述**：详细描述主体、背景、构图、光影、色彩。
        2.  **艺术风格**：分析画风（如赛博朋克、水彩、油画、写实摄影、二次元等）。
        3.  **关键词提取**：提取关键Tag（如 masterpiece, best quality, 4k, [主体], [动作], [环境]）。
        4.  **Stable Diffusion格式**：组合成一段标准的英文Prompt。
        5.  **Midjourney格式**：组合成一段适用于Midjourney的Prompt（带参数建议）。
    """.trimIndent()

    val memeExplainer = """
        请解释这张表情包/梗图（Meme）：
        1.  **图面内容**：图片里发生了什么？有什么文字？
        2.  **梗的来源**：这个梗出自哪里？（如果知道的话）
        3.  **笑点解析**：为什么这张图好笑？它在表达什么情绪或讽刺什么现象？
        4.  **适用场景**：通常在什么情况下使用这张图？
    """.trimIndent()

    // ============================
    // 知识与学习任务模板
    // ============================

    val solveMath = """
        请识别并解答图片中的数学题：
        1.  **题目识别**：将题目文字/公式转换为清晰的文本格式（LaTeX）。
        2.  **解题思路**：简要说明解题的关键步骤和逻辑。
        3.  **详细步骤**：一步步计算或推导。
        4.  **最终答案**：给出明确的最终结果。
        请确保数学符号准确，逻辑严密。
    """.trimIndent()

    val explainCode = """
        请识别图片中的代码片段并进行解释：
        1.  **语言识别**：这是什么编程语言？
        2.  **功能概括**：这段代码主要在做什么？
        3.  **逐行/逐块解析**：解释关键代码行的作用。
        4.  **潜在问题**：如果有明显的Bug或优化空间，请指出。
        请用通俗易懂的语言解释。
    """.trimIndent()

    // ============================
    // 生活助手任务模板
    // ============================

    val gameHint = """
        请作为资深游戏玩家，分析这张游戏截图并给出攻略建议：
        1.  **当前状态**：识别游戏类型、当前场景或关卡状态。
        2.  **关键信息**：注意血量、资源、任务目标、敌人弱点等。
        3.  **行动建议**：下一步应该做什么？有没有隐藏要素或技巧？
        4.  **解谜提示**：如果涉及解谜，给出提示而非直接答案（除非很明显）。
    """.trimIndent()

    val travelGuide = """
        请作为导游，识别图片中的景点或地标：
        1.  **名称识别**：这是哪里？（具体景点名称、城市、国家）。
        2.  **历史文化**：简要介绍其历史背景或文化意义。
        3.  **游玩亮点**：有哪些值得关注的细节或必做的事？
        4.  **实用贴士**：最佳游玩时间、注意事项等（如适用）。
    """.trimIndent()

    // ============================
    // 默认自定义提示词
    // ============================

    val defaultCustomPrompt = """
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
