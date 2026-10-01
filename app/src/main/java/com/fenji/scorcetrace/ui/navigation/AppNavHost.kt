package com.fenji.scorcetrace.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
)

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
 * 底部导航栏是"液态玻璃"：内容侧用 [haze] 标记为被模糊源，导航栏用 [hazeChild] 消费，
 * 因此内容滚到导航栏下方时会被实时模糊。
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val hazeState = remember { HazeState() }
    val glassStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tints = emptyList(),
        blurRadius = 24.dp,
        noiseFactor = 0f,
    )

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeChild(state = hazeState, style = glassStyle),
            ) {
                // 1dp 顶部高光，强化玻璃"边缘发亮"的质感
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.14f)),
                )
                NavigationBar(containerColor = Color.Transparent) {
                    bottomNavItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = { navController.navigateToTab(item.screen) },
                            icon = { Icon(item.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // 内容不再按底部栏内缩，才能滚到玻璃栏下方被模糊；
        // 底部留白改由各页面自己加（值就是即将传入的 bottomContentPadding）。
        val bottomContentPadding = innerPadding.calculateBottomPadding()

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .haze(state = hazeState)
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
