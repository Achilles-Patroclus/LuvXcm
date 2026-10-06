package com.fenji.scoretrace.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.music.MusicPlayerViewModel
import com.fenji.scoretrace.ui.screen.ai.AiHistoryScreen
import com.fenji.scoretrace.ui.screen.ai.AiScreen
import com.fenji.scoretrace.ui.screen.home.HomeScreen
import com.fenji.scoretrace.ui.screen.mine.AboutScreen
import com.fenji.scoretrace.ui.screen.mine.FeedbackScreen
import com.fenji.scoretrace.ui.screen.mine.MineScreen
import com.fenji.scoretrace.ui.screen.notification.NotificationScreen
import com.fenji.scoretrace.ui.screen.school.TargetSchoolScreen
import com.fenji.scoretrace.ui.screen.score.AiScoreInputScreen
import com.fenji.scoretrace.ui.screen.score.ManualScoreInputScreen
import com.fenji.scoretrace.ui.screen.score.ScoreDetailScreen
import com.fenji.scoretrace.ui.screen.score.ScoreScreenNew
import com.fenji.scoretrace.ui.screen.subject.SubjectConfigScreen
import com.fenji.scoretrace.ui.screen.timer.StudyTimerScreen
import com.fenji.scoretrace.util.AppToast

/**
 * 顶层导航。音乐播放器的 ViewModel 在这一层获取——它的 ViewModelStoreOwner 是 Activity，
 * 因此切底部 Tab 不会把它销毁，音乐也就不会被中断。
 *
 * 过渡动画：底部 Tab 之间用淡入淡出（[tabEnter]/[tabExit]），二级页面用水平滑动（NavHost 默认）。
 * 轻提示由 [com.fenji.scoretrace.util.AppToast] 全局队列管理，宿主 `AppToastHost` 挂在 MainActivity。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
    onBottomBarVisibleChange: (Boolean) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 键盘弹出时隐藏底栏，让 AI 输入框紧贴键盘（配合 AiScreen 的 imePadding）
    val imeVisible = WindowInsets.isImeVisible
    // 首页悬浮音乐面板状态提升到此处：点击任意底部 Tab 时一并收起（面板是跨 Tab 的全局浮层）
    var showMusicPanel by rememberSaveable { mutableStateOf(false) }
    // 成绩详情「AI分析本次考试」注入的问题：切到 AI Tab 后由 AiScreen 消费并自动发起对话
    var pendingAiPrompt by rememberSaveable { mutableStateOf<String?>(null) }
    // 全屏页（设计稿无底部导航栏）：成绩详情、目标院校、AI录成绩、通知中心均隐藏底栏
    val hideBottomBar = currentRoute?.startsWith("score_detail") == true ||
        currentRoute == Screen.TargetSchool.route ||
        currentRoute == Screen.AiScoreInput.route ||
        currentRoute?.startsWith("manual_score_input") == true ||
        currentRoute == Screen.Notifications.route ||
        currentRoute == Screen.SubjectConfig.route ||
        currentRoute == Screen.StudyTimer.route ||
        currentRoute == Screen.AiHistory.route ||
        currentRoute == Screen.About.route ||
        currentRoute == Screen.Feedback.route ||
        (currentRoute == Screen.AI.route && imeVisible)

    LaunchedEffect(hideBottomBar) { onBottomBarVisibleChange(!hideBottomBar) }

    val tabs = listOf(
        BottomBarTab(Screen.Home, R.string.tab_home, rememberVectorPainter(Icons.Filled.Home)),
        BottomBarTab(Screen.AI, R.string.tab_ai, painterResource(R.drawable.ic_ai_logo)),
        BottomBarTab(Screen.Score, R.string.tab_score, painterResource(R.drawable.ic_bar_chart)),
        BottomBarTab(Screen.Mine, R.string.tab_mine, rememberVectorPainter(Icons.Filled.Person)),
    )

    // 返回键：四个底部 Tab 上连按两次退出应用；其余子页面（成绩详情/目标院校/设置等）返回上一页。
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
                AppToast.info("再按一次退出应用")
            }
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!hideBottomBar) {
                LightBottomBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onSelect = {
                        // 切 Tab 前先收起音乐面板
                        showMusicPanel = false
                        navController.navigateToTab(it)
                    },
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
            // 四个底部 Tab 用淡入淡出，避免水平滑动在平级切换时的突兀感
            composable(
                route = Screen.Home.route,
                enterTransition = { tabEnter() },
                exitTransition = { tabExit() },
                popEnterTransition = { tabEnter() },
                popExitTransition = { tabExit() },
            ) {
                HomeScreen(
                    musicViewModel = musicViewModel,
                    showMusicPanel = showMusicPanel,
                    onMusicPanelChange = { showMusicPanel = it },
                    onOpenScore = { navController.navigateToTab(Screen.Score) },
                    onOpenTargetSchool = { navController.navigate(Screen.TargetSchool.route) },
                    onOpenAiScore = { navController.navigate(Screen.AiScoreInput.createRoute()) },
                    onOpenManualScore = { navController.navigate(Screen.ManualScoreInput.createRoute()) },
                    onOpenNotification = { navController.navigate(Screen.Notifications.route) },
                    onOpenTimer = { navController.navigate(Screen.StudyTimer.route) },
                )
            }
            composable(
                route = Screen.AI.route,
                enterTransition = { tabEnter() },
                exitTransition = { tabExit() },
                popEnterTransition = { tabEnter() },
                popExitTransition = { tabExit() },
            ) { entry ->
                val loadConversationId by entry.savedStateHandle
                    .getStateFlow(KEY_LOAD_CONVERSATION, -1L)
                    .collectAsStateWithLifecycle()
                val newChatTick by entry.savedStateHandle
                    .getStateFlow(KEY_NEW_CHAT, 0L)
                    .collectAsStateWithLifecycle()
                AiScreen(
                    onOpenHistory = { navController.navigate(Screen.AiHistory.route) },
                    onOpenAiScore = { pickImage ->
                        navController.navigate(Screen.AiScoreInput.createRoute(pickImage))
                    },
                    loadConversationId = loadConversationId,
                    newChatTick = newChatTick,
                    onCommandConsumed = {
                        entry.savedStateHandle[KEY_LOAD_CONVERSATION] = -1L
                        entry.savedStateHandle[KEY_NEW_CHAT] = 0L
                    },
                    pendingPrompt = pendingAiPrompt,
                    onPromptConsumed = { pendingAiPrompt = null },
                )
            }
            composable(
                route = Screen.Score.route,
                enterTransition = { tabEnter() },
                exitTransition = { tabExit() },
                popEnterTransition = { tabEnter() },
                popExitTransition = { tabExit() },
            ) {
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
            ) { entry ->
                ScoreDetailScreen(
                    examId = entry.arguments?.getLong(Screen.ScoreDetail.ARG_EXAM_ID) ?: 0L,
                    onBack = { navController.popBackStack() },
                    onAiAnalyze = { prompt ->
                        pendingAiPrompt = prompt
                        navController.navigateToTab(Screen.AI)
                    },
                    onEdit = { examId ->
                        navController.navigate(Screen.ManualScoreInput.createRoute(examId))
                    },
                )
            }
            composable(
                route = Screen.Mine.route,
                enterTransition = { tabEnter() },
                exitTransition = { tabExit() },
                popEnterTransition = { tabEnter() },
                popExitTransition = { tabExit() },
            ) {
                MineScreen(
                    onOpenTargetSchool = { navController.navigate(Screen.TargetSchool.route) },
                    onOpenSubjectConfig = { navController.navigate(Screen.SubjectConfig.route) },
                    onOpenAbout = { navController.navigate(Screen.About.route) },
                    onOpenFeedback = { navController.navigate(Screen.Feedback.route) },
                    onNavigateHome = { navController.navigateToTab(Screen.Home) },
                )
            }
            composable(Screen.TargetSchool.route) {
                TargetSchoolScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.AiScoreInput.route,
                arguments = listOf(
                    navArgument(Screen.AiScoreInput.ARG_AUTO_PICK) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                AiScoreInputScreen(
                    autoPickImage = entry.arguments?.getBoolean(Screen.AiScoreInput.ARG_AUTO_PICK) ?: false,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.ManualScoreInput.route,
                arguments = listOf(
                    navArgument(Screen.ManualScoreInput.ARG_EXAM_ID) {
                        type = NavType.LongType
                        defaultValue = Screen.ManualScoreInput.NO_EXAM_ID
                    },
                ),
            ) {
                ManualScoreInputScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Screen.Notifications.route) {
                NotificationScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.StudyTimer.route) {
                StudyTimerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.AiHistory.route) {
                AiHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenConversation = { id ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle?.set(KEY_LOAD_CONVERSATION, id)
                        navController.popBackStack()
                    },
                    onNewChat = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle?.set(KEY_NEW_CHAT, System.currentTimeMillis())
                        navController.popBackStack()
                    },
                )
            }
            composable(Screen.SubjectConfig.route) {
                SubjectConfigScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Feedback.route) {
                FeedbackScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private const val KEY_LOAD_CONVERSATION = "loadConversationId"
private const val KEY_NEW_CHAT = "newChatTick"

/** 底部 Tab 统一的切换逻辑：单栈、保留各 Tab 的滚动位置。 */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Tab 进入：淡入 200ms（无位移，符合平级切换直觉）。 */
private fun tabEnter(): EnterTransition = fadeIn(animationSpec = tween(200))

/** Tab 退出：淡出 200ms。 */
private fun tabExit(): ExitTransition = fadeOut(animationSpec = tween(200))


