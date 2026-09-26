package com.fenji.scorcetrace.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.StudyTask
import com.fenji.scorcetrace.ui.component.EmptyView
import com.fenji.scorcetrace.ui.component.ListCard
import com.fenji.scorcetrace.ui.component.LoadingView
import com.fenji.scorcetrace.ui.component.ScreenHeader
import com.fenji.scorcetrace.ui.component.TargetSchoolCard
import com.fenji.scorcetrace.ui.component.TargetSchoolEditorSheet
import com.fenji.scorcetrace.ui.music.MusicPlayerViewModel
import com.fenji.scorcetrace.ui.music.component.MusicPlayerCard
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.util.DateUtils
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onOpenPlan: () -> Unit = {},
    onOpenScore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    musicViewModel: MusicPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val countdown by viewModel.countdownState.collectAsStateWithLifecycle()
    val isPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    val musicError by musicViewModel.errorMessage.collectAsStateWithLifecycle()
    val subjectNames = state.subjects.associate { it.id to it.name }
    val subjectColors = state.subjects.associate { it.id to Color(it.color) }
    var showTargetEditor by rememberSaveable { mutableStateOf(false) }
    var showTargetDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // 首次进入首页：等偏好设置读出后再决定是否自动播放
    LaunchedEffect(Unit) {
        val ready = viewModel.uiState.filter { !it.isLoading }.first()
        if (ready.autoPlayMusic) musicViewModel.play()
    }

    // 离开首页（切 Tab）时暂停，避免后台继续播放
    DisposableEffect(Unit) {
        onDispose { musicViewModel.pause() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "ScoreTrace")

        if (state.isLoading) {
            LoadingView()
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.PageHorizontal)
                .padding(bottom = Dimens.ContentBottom),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            CountdownCard(state = countdown)

            TargetSchoolCard(
                targetSchool = state.targetSchool,
                onClick = { showTargetEditor = true },
                onLongClick = { showTargetDeleteDialog = true },
            )

            SectionTitle("今日学习任务")
            if (state.todayTasks.isEmpty()) {
                EmptyView(
                    message = "今天暂无学习任务",
                    subtitle = "去「学习计划」安排今天要完成的内容",
                    actionLabel = "添加学习任务",
                    onAction = onOpenPlan,
                )
            } else {
                state.todayTasks.forEach { task ->
                    HomeTaskCard(
                        task = task,
                        subjectName = subjectNames[task.subjectId].orEmpty(),
                        accent = subjectColors[task.subjectId] ?: Color.Unspecified,
                        onToggle = { viewModel.toggleTask(task) },
                    )
                }
            }

            SectionTitle("最近成绩")
            if (state.recentScores.isEmpty()) {
                EmptyView(
                    message = "还没有成绩记录",
                    subtitle = "记录每一次考试，才看得见进步的曲线",
                    actionLabel = "记录成绩",
                    onAction = onOpenScore,
                )
            } else {
                state.recentScores.forEach { record ->
                    HomeScoreCard(
                        record = record,
                        subjectName = subjectNames[record.subjectId].orEmpty(),
                        accent = subjectColors[record.subjectId] ?: Color.Unspecified,
                    )
                }
            }

            // 迷你播放器收在页面最底部：16dp(spacedBy) + 8dp ≈ SectionGap，与上方成绩区块分组；
            // 下方由父 Column 的 bottom padding 兜住 24dp，避免贴住底部导航栏。
            MusicPlayerCard(
                modifier = Modifier.padding(top = Dimens.TightGap),
                title = musicViewModel.currentTrackTitle,
                artist = musicViewModel.currentTrackArtist,
                isPlaying = isPlaying,
                errorMessage = musicError,
                onTogglePlayPause = musicViewModel::togglePlayPause,
            )
        }
    }

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
}

// ─────────────────────────────────────────────────────────────
// 倒计时卡片
// ─────────────────────────────────────────────────────────────

/** 「今年已过 / 全年」的只读派生数据，由系统时间在本地推导，不涉及任何业务状态。 */
private data class YearProgress(val passedDays: Int, val totalDays: Int) {
    val fraction: Float
        get() = if (totalDays <= 0) 0f else (passedDays.toFloat() / totalDays).coerceIn(0f, 1f)
}

@Composable
private fun rememberYearProgress(): YearProgress = remember {
    val calendar = Calendar.getInstance()
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
    YearProgress(passedDays = dayOfYear, totalDays = if (totalDays > 0) totalDays else 365)
}

@Composable
private fun CountdownCard(state: CountdownUiState) {
    // 呼吸动画（幅度克制）：在 drawBehind 里读取，避免每帧重组整张卡片
    val transition = rememberInfiniteTransition(label = "countdownBreath")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val year = rememberYearProgress()
    val yearAnim = remember { Animatable(0f) }
    LaunchedEffect(year.fraction) {
        yearAnim.animateTo(year.fraction, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardCorner),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ScoreTraceColors.CardBorder),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(ScoreTraceColors.HighlightGradientDark))
                .drawBehind {
                    val glow = 0.02f + 0.06f * breath.value
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ScoreTraceColors.AccentCyan.copy(alpha = glow),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.05f),
                            radius = size.maxDimension * 0.9f,
                        ),
                    )
                }
                .padding(Dimens.CardPadding),
        ) {
            if (state.isEnded) {
                Text(
                    text = "高考已结束",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.AccentCyan,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "距高考还有",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScoreTraceColors.TextSecondary,
                    )
                    DatePill(text = state.targetDateText)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CountdownUnit(value = state.days, label = "天", modifier = Modifier.weight(1f))
                    CountdownUnit(value = state.hours, label = "时", modifier = Modifier.weight(1f))
                    CountdownUnit(value = state.minutes, label = "分", modifier = Modifier.weight(1f))
                    CountdownUnit(
                        value = state.seconds,
                        label = "秒",
                        modifier = Modifier.weight(1f),
                        animated = true,
                        highlight = true,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                YearProgressBar(
                    progress = { yearAnim.value },
                    passedDays = year.passedDays,
                    totalDays = year.totalDays,
                )
            }
        }
    }
}

@Composable
private fun DatePill(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(ScoreTraceColors.AccentCyan.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ScoreTraceColors.AccentCyan,
            maxLines = 1,
        )
    }
}

@Composable
private fun CountdownUnit(
    value: Long,
    label: String,
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    highlight: Boolean = false,
) {
    val text = value.toString().padStart(2, '0')
    val blockShape = RoundedCornerShape(Dimens.SubCorner)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(blockShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), blockShape)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (animated) {
                // 秒数变化时做缩放 + 淡入淡出，避免数字直接跳变造成抖动
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.6f))
                            .togetherWith(
                                fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.3f)
                            )
                    },
                    label = "countdownSeconds",
                ) { current ->
                    CountdownNumber(current, highlight)
                }
            } else {
                CountdownNumber(text, highlight)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ScoreTraceColors.TextTertiary,
        )
    }
}

@Composable
private fun CountdownNumber(text: String, highlight: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = if (highlight) ScoreTraceColors.AccentCyan else Color.White,
        textAlign = TextAlign.Center,
    )
}

/** 年度进度条：细线 3dp + 圆角，展示「今年已过天数 / 全年天数」。 */
@Composable
private fun YearProgressBar(
    progress: () -> Float,
    passedDays: Int,
    totalDays: Int,
) {
    val percent = if (totalDays <= 0) 0 else (passedDays * 100f / totalDays).roundToInt()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "今年已过 $passedDays 天 / 全年 $totalDays 天",
                style = MaterialTheme.typography.labelSmall,
                color = ScoreTraceColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = ScoreTraceColors.AccentCyan,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
        ) {
            val radius = size.height / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                cornerRadius = CornerRadius(radius),
            )
            val filled = size.width * progress().coerceIn(0f, 1f)
            if (filled > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(ScoreTraceColors.AccentBlue, ScoreTraceColors.AccentCyan),
                    ),
                    size = Size(filled, size.height),
                    cornerRadius = CornerRadius(min(radius, filled / 2f)),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 列表区块
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            // 16dp(spacedBy) + 8dp ≈ SectionGap(24dp)，形成区块分组感
            .padding(top = Dimens.TightGap)
            .semantics { heading() },
    )
}

@Composable
private fun HomeTaskCard(
    task: StudyTask,
    subjectName: String,
    accent: Color,
    onToggle: () -> Unit,
) {
    val due = task.dueDate?.let { "截止 " + DateUtils.formatDate(it) }.orEmpty()
    ListCard(
        title = task.title,
        subtitle = listOf(subjectName, due).filter { it.isNotEmpty() }.joinToString(" · ")
            .ifEmpty { null },
        accent = accent,
        titleDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
        leading = {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = ScoreTraceColors.AccentCyan),
            )
        },
    )
}

@Composable
private fun HomeScoreCard(record: ScoreRecord, subjectName: String, accent: Color) {
    val date = DateUtils.formatDate(record.examDate)
    ListCard(
        title = record.examName,
        subtitle = if (subjectName.isEmpty()) date else "$subjectName · $date",
        accent = accent,
        trailing = {
            Text(
                text = "${formatScore(record.score)} / ${formatScore(record.fullScore)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(end = 10.dp),
            )
        },
    )
}

/** 去掉 Double 的冗余 ".0"，让「120.0 / 150.0」显示为「120 / 150」。纯展示层处理。 */
private fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
