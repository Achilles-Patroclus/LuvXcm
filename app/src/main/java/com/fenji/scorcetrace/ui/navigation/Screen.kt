package com.fenji.scorcetrace.ui.navigation

/** 各功能模块的路由 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AI : Screen("ai")
    data object Score : Screen("score")
    data object Mine : Screen("mine")

    /** 学习计划：不在底部 Tab 中，由首页「学习计划」快捷入口进入 */
    data object Plan : Screen("plan")

    /** 设置：不在底部 Tab 中，后续由「我的」页进入 */
    data object Settings : Screen("settings")

    /** 成绩详情：从成绩页历史记录进入，携带考试 ID */
    data class ScoreDetail(val examId: Long) : Screen(ROUTE) {
        companion object {
            const val ROUTE = "score_detail/{examId}"
            const val ARG_EXAM_ID = "examId"
            fun createRoute(examId: Long): String = "score_detail/$examId"
        }
    }
}
