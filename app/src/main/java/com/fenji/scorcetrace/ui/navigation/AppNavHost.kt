package com.fenji.scorcetrace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.music.MusicPlayerViewModel
import com.fenji.scorcetrace.ui.screen.home.HomeScreen
import com.fenji.scorcetrace.ui.screen.plan.PlanScreen
import com.fenji.scorcetrace.ui.screen.score.ScoreScreen
import com.fenji.scorcetrace.ui.screen.settings.SettingsScreen
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.tab_home, Icons.Filled.Home),
    BottomNavItem(Screen.Plan, R.string.tab_plan, Icons.Filled.DateRange),
    BottomNavItem(Screen.Score, R.string.tab_score, Icons.Filled.Star),
    BottomNavItem(Screen.Settings, R.string.tab_settings, Icons.Filled.Settings),
)

/**
 * 顶层导航。音乐播放器的 ViewModel 在这一层获取——它的 ViewModelStoreOwner 是 Activity，
 * 因此切底部 Tab 不会把它销毁，音乐也就不会被中断。
 *
 * 底部导航栏是「液态玻璃」：页面内容用 [layerBackdrop] 录进一个图形层，底栏消费它做实时模糊，
 * 因此内容滚到胶囊下方时会被模糊，并叠加边缘折射与高光。
 *
 * 布局结构：悬浮底栏不再放进 `Scaffold.bottomBar`（该槽位对 WindowInsets 的处理会与自定义
 * 悬浮组件冲突，导致 `navigationBarsPadding` 失效、内容被手势条遮挡），而是作为根 [Box] 的
 * 兄弟层叠元素对齐到底部，用 `navigationBarsPadding()` 让整条胶囊避开系统手势条。
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 模糊源必须先铺一层不透明主题底色，否则内容透明区会被模糊扩散成色斑（miuix 官方要求）。
    val surfaceColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    // 底栏避让：取真实导航栏 inset；手势条模式下 inset 可能为 0，退回 28dp 留白。
    val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barBottomPadding = if (navigationBarsBottom > 0.dp) 8.dp + navigationBarsBottom else 28.dp
    // 页面底部留白 = 底栏高度 64dp + 底栏下方留白 + 16dp 列表底部额外呼吸空间。
    val bottomContentPadding = 64.dp + barBottomPadding + 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // 完全不由 Scaffold 接管 Insets：顶部由 statusBarsPadding 处理，底部交给根 Box 里的悬浮底栏。
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .statusBarsPadding(),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        musicViewModel = musicViewModel,
                        bottomContentPadding = bottomContentPadding,
                        onOpenPlan = { navController.navigateToTab(Screen.Plan) },
                        onOpenScore = { navController.navigateToTab(Screen.Score) },
                    )
                }
                composable(Screen.Plan.route) {
                    PlanScreen(bottomContentPadding = bottomContentPadding)
                }
                composable(Screen.Score.route) {
                    ScoreScreen(bottomContentPadding = bottomContentPadding)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(bottomContentPadding = bottomContentPadding)
                }
            }
        }

        // 悬浮底栏：脱离 Scaffold，作为根 Box 的兄弟层叠元素，直接对齐到底部。
        FloatingBottomBar(
            items = bottomNavItems,
            currentRoute = currentRoute,
            onSelect = { navController.navigateToTab(it) },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 28.dp, end = 28.dp, bottom = barBottomPadding),
        )
    }
}

/** 底部 Tab 统一的切换逻辑：单栈、保留各 Tab 的滚动位置。 */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
