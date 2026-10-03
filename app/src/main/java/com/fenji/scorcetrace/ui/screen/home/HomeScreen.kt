package com.fenji.scorcetrace.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.component.AiFocusCard
import com.fenji.scorcetrace.ui.component.CountdownCard
import com.fenji.scorcetrace.ui.component.HomeTopBar
import com.fenji.scorcetrace.ui.component.QuickAction
import com.fenji.scorcetrace.ui.component.QuickActions
import com.fenji.scorcetrace.ui.component.ScoreOverviewCard
import com.fenji.scorcetrace.ui.component.SubjectScore
import com.fenji.scorcetrace.ui.component.TargetSchoolCardNew
import com.fenji.scorcetrace.ui.component.TargetSchoolEditorSheet
import com.fenji.scorcetrace.ui.music.MusicPlayerViewModel
import com.fenji.scorcetrace.ui.music.component.MusicFloatingPanel
import com.fenji.scorcetrace.ui.music.component.MusicPlaylistSheet
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    musicViewModel: MusicPlayerViewModel,
    bottomContentPadding: Dp = Dimens.ContentBottom,
    onOpenPlan: () -> Unit = {},
    onOpenScore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 只持有 State 对象、不在函数体读值：倒计时每秒 tick 不会重组整页
    val countdown = viewModel.countdownState.collectAsStateWithLifecycle()
    val isPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTrackIndex by musicViewModel.currentIndex.collectAsStateWithLifecycle()
    val subjectRates by viewModel.subjectRates.collectAsStateWithLifecycle()
    val aiTip by viewModel.aiTip.collectAsStateWithLifecycle()
    val studyDay by viewModel.studyDayCount.collectAsStateWithLifecycle()

    var showTargetEditor by rememberSaveable { mutableStateOf(false) }
    var showTargetDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showPlaylist by rememberSaveable { mutableStateOf(false) }
    var showMusicPanel by rememberSaveable { mutableStateOf(false) }

    // 返回键优先关面板，而不是退出应用（AppNavHost 的返回处理在面板关闭后才生效）
    BackHandler(enabled = showMusicPanel) { showMusicPanel = false }

    // 每次进入 App 只触发一次自动播放；切回首页不再 play()，所以不会打断正在播放的音乐
    LaunchedEffect(Unit) {
        val ready = viewModel.uiState.filter { !it.isLoading }.first()
        musicViewModel.autoPlayOnce(enabled = ready.autoPlayMusic)
    }

    // 悬浮面板：进度/循环/随机/收藏都在这里订阅——面板未展开时这段不组合，
    // 因此每秒刷新的进度不会带着整页重组。
    val musicPanel: @Composable () -> Unit = {
        val track by musicViewModel.currentTrack.collectAsStateWithLifecycle()
        val position by musicViewModel.playbackPosition.collectAsStateWithLifecycle()
        val duration by musicViewModel.duration.collectAsStateWithLifecycle()
        val repeatMode by musicViewModel.repeatMode.collectAsStateWithLifecycle()
        val shuffleMode by musicViewModel.shuffleMode.collectAsStateWithLifecycle()
        val favorites by musicViewModel.favorites.collectAsStateWithLifecycle()

        MusicFloatingPanel(
            track = track,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleMode,
            isFavorite = favorites.contains(track.id),
            onTogglePlayPause = musicViewModel::togglePlayPause,
            onPrevious = musicViewModel::skipToPrevious,
            onNext = musicViewModel::skipToNext,
            onSeekTo = musicViewModel::seekTo,
            onToggleRepeat = musicViewModel::toggleRepeat,
            onToggleShuffle = musicViewModel::toggleShuffle,
            onToggleFavorite = { musicViewModel.toggleFavorite(track.id) },
            onDismiss = { showMusicPanel = false },
            onOpenPlaylist = { showPlaylist = true },
        )
    }

    HomeScreenContent(
        state = state,
        countdown = countdown,
        isPlaying = isPlaying,
        studyDay = studyDay,
        subjectRates = subjectRates,
        aiTip = aiTip,
        bottomContentPadding = bottomContentPadding,
        showMusicPanel = showMusicPanel,
        musicPanel = musicPanel,
        onTargetClick = { showTargetEditor = true },
        onTargetLongClick = { showTargetDeleteDialog = true },
        onMusicClick = { showMusicPanel = !showMusicPanel },
        onDismissMusicPanel = { showMusicPanel = false },
        onNotificationClick = {},
        onDetailClick = onOpenScore,
        onRefreshAiTip = viewModel::refreshAiTip,
        onOpenPlan = onOpenPlan,
    )

    if (showTargetEditor) {
        TargetSchoolEditorSheet(
            initial = state.targetSchool,
            onDismiss = { showTargetEditor = false },
            onSave = { schoolName, majorName, targetScore, currentScore, year ->
                viewModel.saveTargetSchool(
                    schoolName = schoolName,
                    majorName = majorName,
                    targetScore = targetScore,
                    currentScore = currentScore,
                    year = year,
                    // 已有记录时带上原 id 覆盖，保证始终只有一条目标
                    id = state.targetSchool?.id ?: 0L,
                )
                showTargetEditor = false
            },
        )
    }

    val targetSchoolToDelete = state.targetSchool
    if (showTargetDeleteDialog && targetSchoolToDelete != null) {
        AlertDialog(
            onDismissRequest = { showTargetDeleteDialog = false },
            title = { Text("删除目标院校？") },
            text = { Text("删除后首页不再展示目标分数对比，可随时重新设定。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTargetSchool(targetSchoolToDelete)
                        showTargetDeleteDialog = false
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    if (showPlaylist) {
        MusicPlaylistSheet(
            tracks = musicViewModel.playlist,
            currentIndex = currentTrackIndex,
            onSelect = { index ->
                musicViewModel.playTrack(index)
                showPlaylist = false
            },
            onDismiss = { showPlaylist = false },
        )
    }
}

/**
 * 内容区叠加层：可滚动内容 + 关闭遮罩 + 悬浮音乐面板。
 *
 * 单独抽成函数是为了让 `AnimatedVisibility` 处在一个纯 `Box` 作用域里——若直接写在
 * `HomeScreenContent` 的 `Column` 内层 `Box` 中，会被解析成 `ColumnScope` 重载而编译失败
 * （Kotlin 拒绝跨作用域使用隐式接收者）。`modifier`（含 `weight`）由 Column 作用域的调用方传入。
 */
@Composable
private fun HomeContentLayer(
    modifier: Modifier = Modifier,
    showMusicPanel: Boolean,
    onScrimClick: () -> Unit,
    musicPanel: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()

        // 点击遮罩关闭
        AnimatedVisibility(
            visible = showMusicPanel,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(onClick = onScrimClick),
            )
        }

        // 悬浮音乐面板：弹性下滑 + 0.92→1.0 放大 + 淡入；收起反之
        AnimatedVisibility(
            visible = showMusicPanel,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250, easing = FastOutSlowInEasing),
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
        ) {
            musicPanel()
        }
    }
}

/** 首页内容（无状态）：状态与回调由 [HomeScreen] 注入，便于 Preview 与职责分离。 */
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    countdown: State<CountdownUiState>,
    isPlaying: Boolean,
    studyDay: Int,
    subjectRates: List<SubjectScore>,
    aiTip: String,
    bottomContentPadding: Dp,
    showMusicPanel: Boolean,
    musicPanel: @Composable () -> Unit,
    onTargetClick: () -> Unit,
    onTargetLongClick: () -> Unit,
    onMusicClick: () -> Unit,
    onDismissMusicPanel: () -> Unit,
    onNotificationClick: () -> Unit,
    onDetailClick: () -> Unit,
    onRefreshAiTip: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val yearPassedPercent = rememberYearPassedPercent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScoreTraceColors.PageBackgroundLight),
    ) {
        // 1. 顶部标题栏（固定在滚动区之外，滚动时不跟随内容移动）
        HomeTopBar(
            subtitle = "早上好，备考第 $studyDay 天",
            isMusicPlaying = isPlaying,
            onMusicClick = onMusicClick,
            onNotificationClick = onNotificationClick,
        )

        // 2. 内容区：可滚动列表 + 关闭遮罩 + 悬浮音乐面板
        HomeContentLayer(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            showMusicPanel = showMusicPanel,
            onScrimClick = onDismissMusicPanel,
            musicPanel = musicPanel,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomContentPadding),
            ) {
                // 倒计时卡
                CountdownCard(
                    countdown = countdown,
                    yearPassedPercent = yearPassedPercent,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 目标院校卡
                TargetSchoolCardNew(
                    targetSchool = state.targetSchool,
                    onClick = onTargetClick,
                    onLongClick = onTargetLongClick,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 成绩概览卡
                ScoreOverviewCard(
                    subjects = subjectRates,
                    // TODO: 总分/排名/较上次变化需从成绩汇总实体获取，当前为硬编码占位（数据模型暂无排名字段）
                    totalScore = "562",
                    rankText = "班级第15",
                    deltaText = "较上次 ↑12 分",
                    deltaPositive = true,
                    onDetailClick = onDetailClick,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // AI 重点卡
                AiFocusCard(
                    title = "今日 AI 重点",
                    content = aiTip,
                    onRefresh = onRefreshAiTip,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 快捷功能
                Text(
                    text = "快捷功能",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextPrimaryLight,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                QuickActions(
                    actions = listOf(
                        QuickAction(
                            label = "AI录成绩",
                            icon = painterResource(R.drawable.ic_photo_camera),
                            color = ScoreTraceColors.QuickActionBlue,
                        ) {},
                        QuickAction(
                            label = "学习计时器",
                            icon = painterResource(R.drawable.ic_timer),
                            color = ScoreTraceColors.QuickActionGreen,
                        ) {},
                        QuickAction(
                            label = "错题本",
                            icon = painterResource(R.drawable.ic_book),
                            color = ScoreTraceColors.QuickActionOrange,
                        ) {},
                        QuickAction(
                            label = "学习计划",
                            icon = painterResource(R.drawable.ic_calendar_month),
                            color = ScoreTraceColors.QuickActionPurple,
                            onClick = onOpenPlan,
                        ),
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/** 「今年已过百分比」，由系统时间本地推导，不涉及业务状态。 */
@Composable
private fun rememberYearPassedPercent(): Int = remember {
    val calendar = Calendar.getInstance()
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
    if (totalDays > 0) (dayOfYear * 100f / totalDays).roundToInt() else 0
}
