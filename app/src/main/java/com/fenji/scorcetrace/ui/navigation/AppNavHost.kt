package com.fenji.scorcetrace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val backdrop = rememberLayerBackdrop()

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                FloatingBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onSelect = { navController.navigateToTab(it) },
                    backdrop = backdrop,
                )
            }
        },
    ) { innerPadding ->
        // 内容不做底部内缩，才能滚到玻璃栏下方被实时模糊；
        // 底部留白改由各页面自己加（值就是即将传入的 bottomContentPadding）。
        val bottomContentPadding = innerPadding.calculateBottomPadding()

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .padding(top = innerPadding.calculateTopPadding()),
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
}

/** 底部 Tab 统一的切换逻辑：单栈、保留各 Tab 的滚动位置。 */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
