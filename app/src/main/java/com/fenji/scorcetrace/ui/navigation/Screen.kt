package com.fenji.scorcetrace.ui.navigation

/** 各功能模块的路由 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Plan : Screen("plan")
    data object Score : Screen("score")
    data object Settings : Screen("settings")
}
