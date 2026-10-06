package com.fenji.scoretrace.ui.screen.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * 学习计时器（正计时 / 倒计时）。全屏页，底部导航栏由 AppNavHost 隐藏。
 */
@Composable
fun StudyTimerScreen(
    onBack: () -> Unit,
    viewModel: StudyTimerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { AppToast.show(it) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TimerTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeSwitcher(mode = state.mode, onModeChange = viewModel::setMode)

                TimerRingCard(state = state)

                if (state.mode == TimerMode.Countdown) {
                    PresetRow(
                        targetSeconds = state.targetSeconds,
                        onSelect = { viewModel.setTargetMinutes(it) },
                        onCustom = { showCustomDialog = true },
                    )
                }

                Controls(
                    state = state,
                    onStart = viewModel::start,
                    onPause = viewModel::pause,
                    onReset = viewModel::reset,
                    onSave = viewModel::saveSession,
                )

                StatsCard(state = state)

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showCustomDialog) {
        CustomDurationDialog(
            initialMinutes = state.targetSeconds / 60,
            onDismiss = { showCustomDialog = false },
            onConfirm = {
                viewModel.setTargetMinutes(it)
                showCustomDialog = false
            },
        )
    }
}

@Composable
private fun TimerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "学习计时器",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun ModeSwitcher(mode: TimerMode, onModeChange: (TimerMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.CardBorderLight.copy(alpha = 0.4f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TimerMode.values().forEach { item ->
            val selected = item == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onModeChange(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
                )
            }
        }
    }
}

@Composable
private fun TimerRingCard(state: TimerUiState) {
    val isCountdown = state.mode == TimerMode.Countdown
    val progress = if (isCountdown) {
        if (state.targetSeconds > 0) (state.elapsedSeconds.toFloat() / state.targetSeconds).coerceIn(0f, 1f) else 0f
    } else {
        (state.elapsedSeconds % 60) / 60f
    }
    val displaySeconds = if (isCountdown) {
        (state.targetSeconds - state.elapsedSeconds).coerceAtLeast(0)
    } else {
        state.elapsedSeconds
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            TimerRing(progress = progress)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatClock(displaySeconds),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCountdown) "倒计时 · 共 ${formatFocus(state.targetSeconds)}" else "正计时",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = when {
                state.isRunning -> "专注进行中，加油"
                state.elapsedSeconds > 0 -> "已暂停"
                else -> "准备好了就开始吧"
            },
            fontSize = 13.sp,
            color = ScoreTraceColors.TextTertiaryLight,
        )
    }
}

@Composable
private fun TimerRing(progress: Float) {
    val track = ScoreTraceColors.CardBorderLight
    val accent = ScoreTraceColors.BrandPrimary
    Canvas(modifier = Modifier.size(230.dp)) {
        val strokeWidth = 16.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun PresetRow(
    targetSeconds: Int,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val presetSeconds = COUNTDOWN_PRESETS.map { it * 60 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        COUNTDOWN_PRESETS.forEach { minutes ->
            PresetChip(
                label = "$minutes 分钟",
                selected = targetSeconds == minutes * 60,
                onClick = { onSelect(minutes) },
            )
        }
        PresetChip(
            label = "自定义",
            selected = targetSeconds !in presetSeconds,
            onClick = onCustom,
        )
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.CardBorderLight,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
        )
    }
}

@Composable
private fun Controls(
    state: TimerUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.5.dp,
                        color = ScoreTraceColors.BrandPrimary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(26.dp),
                    )
                    .clickable(onClick = onReset),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "重置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScoreTraceColors.BrandPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (state.isRunning) {
                            Brush.horizontalGradient(
                                listOf(ScoreTraceColors.BrandPrimaryDark, ScoreTraceColors.BrandPrimaryDark),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                            )
                        },
                    )
                    .clickable { if (state.isRunning) onPause() else onStart() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.isRunning) "暂停" else if (state.elapsedSeconds > 0) "继续" else "开始",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        if (!state.isRunning && state.elapsedSeconds > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "结束并保存本次专注",
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onSave)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.SuccessGreen,
            )
        }
    }
}

@Composable
private fun StatsCard(state: TimerUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "专注统计",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatBlock(label = "今日专注", value = formatFocus(state.todaySeconds), modifier = Modifier.weight(1f))
            StatBlock(label = "累计专注", value = formatFocus(state.totalSeconds), modifier = Modifier.weight(1f))
            StatBlock(label = "专注次数", value = "${state.sessionCount} 次", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ScoreTraceColors.PageBackgroundLight)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = ScoreTraceColors.TextSecondaryLight)
    }
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialMinutes.toString()) }
    val minutes = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时长") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter(Char::isDigit) },
                    label = { Text("分钟（1-240）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = minutes != null && minutes in 1..240,
                onClick = { if (minutes != null) onConfirm(minutes) },
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
