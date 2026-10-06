package com.fenji.scoretrace.ui.navigation

/** 各功能模块的路由 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AI : Screen("ai")
    data object Score : Screen("score")
    data object Mine : Screen("mine")

    /** 设置：不在底部 Tab 中，后续由「我的」页进入 */
    data object Settings : Screen("settings")

    /** 目标院校选择/编辑：从首页目标院校卡或「我的」页进入，全屏页 */
    data object TargetSchool : Screen("target_school")

    /** AI 录成绩：从首页快捷功能或 AI 助手「+」菜单进入，全屏页。
     *  [ARG_AUTO_PICK] 为 true 时进入后自动弹出图片类型选择（用于「从相册选图」）。 */
    data object AiScoreInput : Screen("ai_score_input?autoPick={autoPick}") {
        const val ARG_AUTO_PICK = "autoPick"
        fun createRoute(autoPick: Boolean = false): String = "ai_score_input?autoPick=$autoPick"
    }

    /** 手动录成绩：从首页快捷功能进入；携带 [ARG_EXAM_ID]（> 0）进入编辑模式（从详情页「编辑」）。 */
    data object ManualScoreInput : Screen("manual_score_input?examId={examId}") {
        const val ARG_EXAM_ID = "examId"
        const val NO_EXAM_ID = -1L
        fun createRoute(examId: Long = NO_EXAM_ID): String = "manual_score_input?examId=$examId"
    }

    /** 通知中心：从首页铃铛进入，全屏页 */
    data object Notifications : Screen("notifications")

    /** 选科配置：从「我的」页进入，全屏页 */
    data object SubjectConfig : Screen("subject_config")

    /** 学习计时器：从首页快捷功能进入，全屏页 */
    data object StudyTimer : Screen("study_timer")

    /** AI 历史对话：从 AI 助手页进入，全屏页 */
    data object AiHistory : Screen("ai_history")

    /** 成绩详情：从成绩页历史记录进入，携带考试 ID */
    data class ScoreDetail(val examId: Long) : Screen(ROUTE) {
        companion object {
            const val ROUTE = "score_detail/{examId}"
            const val ARG_EXAM_ID = "examId"
            fun createRoute(examId: Long): String = "score_detail/$examId"
        }
    }
}
