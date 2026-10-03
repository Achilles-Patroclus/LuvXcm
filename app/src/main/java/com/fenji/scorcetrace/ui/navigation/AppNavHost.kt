package com.fenji.scorcetrace.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.music.MusicPlayerViewModel
import com.fenji.scorcetrace.ui.screen.ai.AiScreen
import com.fenji.scorcetrace.ui.screen.home.HomeScreen
import com.fenji.scorcetrace.ui.screen.mine.MineScreen
import com.fenji.scorcetrace.ui.screen.plan.PlanScreen
import com.fenji.scorcetrace.ui.screen.score.ScoreDetailScreen
import com.fenji.scorcetrace.ui.screen.score.ScoreScreenNew
import com.fenji.scorcetrace.ui.screen.settings.SettingsScreen

/**
 * 顶层导航。音乐播放器的 ViewModel 在这一层获取——它的 ViewModelStoreOwner 是 Activity，
 * 因此切底部 Tab 不会把它销毁，音乐也就不会被中断。
 *
 * 底栏为普通浅色底栏（[LightBottomBar]）；Scaffold 用 `contentWindowInsets = WindowInsets.statusBars`
 * 处理顶部状态栏，底栏高度通过 `innerPadding` 自动从内容区扣除，底栏自身用 `navigationBarsPadding()` 避让手势条。
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 成绩详情为全屏页（设计稿无底部导航栏），仅在详情路由隐藏底栏
    val isDetailRoute = currentRoute?.startsWith("score_detail") == true

    val tabs = listOf(
        BottomBarTab(Screen.Home, R.string.tab_home, rememberVectorPainter(Icons.Filled.Home)),
        BottomBarTab(Screen.AI, R.string.tab_ai, painterResource(R.drawable.ic_smart_toy)),
        BottomBarTab(Screen.Score, R.string.tab_score, painterResource(R.drawable.ic_bar_chart)),
        BottomBarTab(Screen.Mine, R.string.tab_mine, rememberVectorPainter(Icons.Filled.Person)),
    )

    // 返回键：四个底部 Tab 上连按两次退出应用；其余子页面（成绩详情/学习计划/设置）返回上一页。
    // 用当前路由判定层级，避免依赖子页面的具体 route 前缀。
    val topLevelRoutes = listOf(Screen.Home.route, Screen.AI.route, Screen.Score.route, Screen.Mine.route)
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    BackHandler {
        if (currentRoute in topLevelRoutes) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            }
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isDetailRoute) {
                LightBottomBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onSelect = { navController.navigateToTab(it) },
                )
            }
        },
        // 顶部由 Scaffold 处理状态栏，底部由 bottomBar 槽位自动从内容区扣除
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(280))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(280))
            },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    musicViewModel = musicViewModel,
                    onOpenPlan = { navController.navigate(Screen.Plan.route) },
                    onOpenScore = { navController.navigateToTab(Screen.Score) },
                )
            }
            composable(Screen.AI.route) { AiScreen() }
            composable(Screen.Score.route) {
                ScoreScreenNew(
                    onOpenAi = { navController.navigateToTab(Screen.AI) },
                    onOpenDetail = { examId ->
                        navController.navigate(Screen.ScoreDetail.createRoute(examId))
                    },
                )
            }
            composable(
                route = Screen.ScoreDetail.ROUTE,
                arguments = listOf(
                    navArgument(Screen.ScoreDetail.ARG_EXAM_ID) { type = NavType.LongType },
                ),
            ) {
                ScoreDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Mine.route) { MineScreen() }
            composable(Screen.Plan.route) { PlanScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
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
