package com.fenji.scoretrace.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.player.TrackInfo
import com.fenji.scoretrace.ui.theme.Dimens
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import kotlin.math.min

// 胶囊必须宽于高才读得出 pill 形：40×36 只差 4dp、圆角又是高度一半，几何上近乎正圆。
private val CapsuleWidth = 56.dp
private val CapsuleHeight = 36.dp
private val CapsuleCorner = 18.dp
private val PanelHeight = 200.dp
private const val BreathMax = 1.15f

/**
 * 首页右上角「灵动岛」音乐胶囊（收起态）。
 *
 * 深色半透明药丸（与倒计时卡同色系），中间一枚青蓝音符；播放中做轻微呼吸缩放，
 * 暂停时降到半透明。整个组件恒用深色，不随浅/深主题切换——倒计时卡本身也恒为深色。
 *
 * 呼吸缩放与暂停透明度都在 [graphicsLayer] 绘制层读取，不触发重组。
 * 点击胶囊展开/收起由调用方持有的 [expanded] 状态驱动。
 */
@Composable
fun DynamicIslandMusic(
    isPlaying: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 播放时才做呼吸缩放（周期 1.5s）；暂停时立即归位、不跑动画
    val breath = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                breath.animateTo(
                    targetValue = BreathMax,
                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                )
                breath.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                )
            }
        } else {
            breath.snapTo(1f)
        }
    }
    val idleAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "idleAlpha",
    )

    Box(
        modifier = modifier
            .size(width = CapsuleWidth, height = CapsuleHeight)
            .clip(RoundedCornerShape(CapsuleCorner))
            .background(
                Brush.linearGradient(
                    ScoreTraceColors.HighlightGradientDark.map { it.copy(alpha = 0.92f) },
                ),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = if (expanded) "收起音乐控制" else "展开音乐控制",
                onClick = { onExpandedChange(!expanded) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = if (isPlaying) "音乐播放中" else "音乐已暂停",
            tint = ScoreTraceColors.AccentCyan,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = breath.value
                    scaleY = breath.value
                    alpha = idleAlpha
                },
        )
    }
}

/**
 * 首页灵动岛展开后的大面板。
 *
 * 全宽深色卡片（与倒计时卡同色系）：顶部收起指示条 + 封面/歌名信息 + 进度条 + 三键控制。
 * 面板整体消费自身点击（不收起），仅顶部指示条可点收起；封面/歌名区域点开播放列表。
 * 进度与时长以 [State] 下传，只在进度条叶子内读取，避免每次进度更新重组整页。
 */
@Composable
fun DynamicIslandMusicPanel(
    track: TrackInfo,
    isPlaying: Boolean,
    playbackPosition: State<Long>,
    duration: State<Long>,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(PanelHeight)
            // 吞掉面板内空白点击（阻止外层「点外部收起」），但不写语义，避免合并子控件可访问性
            .pointerInput(Unit) { detectTapGestures {} },
        shape = RoundedCornerShape(Dimens.CardCorner),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ScoreTraceColors.CardBorder),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(ScoreTraceColors.HighlightGradientDark))
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 收起指示条
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
                    .clickable(role = Role.Button, onClick = onCollapse),
            )

            // ── 封面 + 歌名信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.SmallCorner))
                    .clickable(role = Role.Button, onClick = onOpenPlaylist),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(ScoreTraceColors.CoverGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        fontSize = 13.sp,
                        color = ScoreTraceColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── 进度条 + 时间（抽成独立 composable：进度更新只重组这一小块）
            IslandProgressRow(playbackPosition = playbackPosition, duration = duration)

            // ── 控制按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "上一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ScoreTraceColors.AccentCyan)
                        .clickable(role = Role.Button, onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = if (isPlaying) {
                            painterResource(R.drawable.ic_pause)
                        } else {
                            rememberVectorPainter(Icons.Rounded.PlayArrow)
                        },
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = ScoreTraceColors.OnAccentDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(onClick = onNext, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** 进度条 + 当前/总时长：进度与时长只在函数内读取，进度更新只重组这一小块。 */
@Composable
private fun IslandProgressRow(
    playbackPosition: State<Long>,
    duration: State<Long>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        IslandProgressBar(playbackPosition = playbackPosition, duration = duration)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(playbackPosition.value),
                fontSize = 11.sp,
                color = ScoreTraceColors.TextTertiary,
                maxLines = 1,
            )
            Text(
                text = formatTime(duration.value),
                fontSize = 11.sp,
                color = ScoreTraceColors.TextTertiary,
                maxLines = 1,
            )
        }
    }
}

/** 纯展示型进度条：已播部分青蓝、未播半透明白。进度在绘制层读取，只触发重绘。 */
@Composable
private fun IslandProgressBar(
    playbackPosition: State<Long>,
    duration: State<Long>,
) {
    val fraction by remember {
        derivedStateOf {
            val total = duration.value
            if (total <= 0L) 0f else (playbackPosition.value.toFloat() / total).coerceIn(0f, 1f)
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                contentDescription = "播放进度"
            },
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            cornerRadius = CornerRadius(radius),
        )
        val filled = size.width * fraction
        if (filled > 0f) {
            drawRoundRect(
                color = ScoreTraceColors.AccentCyan,
                size = Size(filled, size.height),
                cornerRadius = CornerRadius(min(radius, filled / 2f)),
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
