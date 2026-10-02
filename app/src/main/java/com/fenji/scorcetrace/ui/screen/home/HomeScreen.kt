package com.fenji.scorcetrace.ui.screen.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
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

    // 每次进入 App 只触发一次自动播放；切回首页不再 play()，所以不会打断正在播放的音乐
    LaunchedEffect(Unit) {
        val ready = viewModel.uiState.filter { !it.isLoading }.first()
        musicViewModel.autoPlayOnce(enabled = ready.autoPlayMusic)
    }

    HomeScreenContent(
        state = state,
        countdown = countdown,
        isPlaying = isPlaying,
        studyDay = studyDay,
        subjectRates = subjectRates,
        aiTip = aiTip,
        bottomContentPadding = bottomContentPadding,
        onTargetClick = { showTargetEditor = true },
        onTargetLongClick = { showTargetDeleteDialog = true },
        onMusicClick = { showPlaylist = true },
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
    onTargetClick: () -> Unit,
    onTargetLongClick: () -> Unit,
    onMusicClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onDetailClick: () -> Unit,
    onRefreshAiTip: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val yearPassedPercent = rememberYearPassedPercent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScoreTraceColors.PageBackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomContentPadding),
    ) {
        // 1. 顶部标题栏
        HomeTopBar(
            subtitle = "早上好，备考第 $studyDay 天",
            isMusicPlaying = isPlaying,
            onMusicClick = onMusicClick,
            onNotificationClick = onNotificationClick,
        )

        // 2. 倒计时卡
        CountdownCard(
            countdown = countdown,
            yearPassedPercent = yearPassedPercent,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(Dimens.CardGap))

        // 3. 目标院校卡
        TargetSchoolCardNew(
            targetSchool = state.targetSchool,
            onClick = onTargetClick,
            onLongClick = onTargetLongClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(Dimens.CardGap))

        // 4. 成绩概览卡
        ScoreOverviewCard(
            subjects = subjectRates,
            // TODO: 总分/排名/较上次变化需从成绩汇总实体获取，当前为硬编码占位（数据模型暂无排名字段）
            totalScore = "562",
            rankText = "班级第15",
            deltaText = "较上次 ↑12 分",
            deltaPositive = true,
            onDetailClick = onDetailClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(Dimens.CardGap))

        // 5. AI 重点卡
        AiFocusCard(
            title = "今日 AI 重点",
            content = aiTip,
            onRefresh = onRefreshAiTip,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(Dimens.SectionGap))

        // 6. 快捷功能
        Text(
            text = "快捷功能",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
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
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(Dimens.SectionGap))
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
