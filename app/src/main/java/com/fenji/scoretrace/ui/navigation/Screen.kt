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

    /** AI 录成绩：从首页快捷功能进入，全屏页 */
    data object AiScoreInput : Screen("ai_score_input")

    /** 通知中心：从首页铃铛进入，全屏页 */
    data object Notifications : Screen("notifications")

    /** 选科配置：从「我的」页进入，全屏页 */
    data object SubjectConfig : Screen("subject_config")

    /** 成绩详情：从成绩页历史记录进入，携带考试 ID */
    data class ScoreDetail(val examId: Long) : Screen(ROUTE) {
        companion object {
            const val ROUTE = "score_detail/{examId}"
            const val ARG_EXAM_ID = "examId"
            fun createRoute(examId: Long): String = "score_detail/$examId"
        }
    }
}
