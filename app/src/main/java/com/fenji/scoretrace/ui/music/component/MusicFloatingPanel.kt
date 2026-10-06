package com.fenji.scoretrace.ui.music.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.player.TrackInfo
import com.fenji.scoretrace.ui.music.RepeatMode
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import kotlin.math.roundToInt

/**
 * 展开态的音乐悬浮面板（液态玻璃卡片）。
 *
 * 由顶部音乐胶囊点击唤出，覆盖在首页内容区之上：拖拽条、封面 + 歌名 + 歌手、标签行、
 * 可拖拽进度条、播放控制栏。
 *
 * 关闭方式：拖拽条点击、音乐胶囊再点、**向下拖拽超过 120dp**、外部遮罩、返回键（后两者在
 * 调用方 HomeScreen 处理）。
 *
 * 玻璃观感由「半透明渐变色 + 白色描边 + 大阴影」实现；原生 `Modifier.blur()` 只模糊自身
 * 绘制内容、采不到背板，故这里不依赖它。
 */
@Composable
fun MusicFloatingPanel(
    track: TrackInfo,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 向下拖拽的位移（仅记录正值），松手后回弹或关闭
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "panelDragOffset",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                val dismissThreshold = DRAG_DISMISS_THRESHOLD.toPx()
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) {
                            dragOffsetY += dragAmount
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (dragOffsetY > dismissThreshold) onDismiss()
                        dragOffsetY = 0f
                    },
                )
            },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ),
                )
                .padding(10.dp),
        ) {
            // 顶部拖拽条：点击收起面板
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )
            }

            // 封面 + 歌名/歌手/标签 + 收藏
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(ScoreTraceColors.CoverGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenPlaylist),
                ) {
                    Text(
                        text = track.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        MusicTag(icon = R.drawable.ic_lyrics, text = "歌词")
                        MusicTag(icon = R.drawable.ic_spatial_audio, text = "空间音频")
                        MusicTag(icon = null, text = "32k")
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        ),
                        contentDescription = "收藏",
                        tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // 进度条 + 时间
            Column(modifier = Modifier.padding(top = 6.dp)) {
                var dragValue by remember { mutableFloatStateOf(-1f) }
                val progress = when {
                    duration <= 0L -> 0f
                    dragValue >= 0f -> dragValue
                    else -> (position.toFloat() / duration).coerceIn(0f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .pointerInput(duration) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onHorizontalDrag = { change, _ ->
                                    dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (dragValue >= 0f && duration > 0L) {
                                        onSeekTo((dragValue * duration).toLong())
                                    }
                                    dragValue = -1f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
                        val centerY = size.height / 2f
                        val trackHeight = 3.dp.toPx()
                        val trackRadius = trackHeight / 2f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(0f, centerY - trackRadius),
                            size = Size(size.width, trackHeight),
                            cornerRadius = CornerRadius(trackRadius),
                        )
                        val clamped = progress.coerceIn(0f, 1f)
                        val activeWidth = size.width * clamped
                        if (activeWidth > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
                                    startX = 0f,
                                    endX = size.width,
                                ),
                                topLeft = Offset(0f, centerY - trackRadius),
                                size = Size(activeWidth, trackHeight),
                                cornerRadius = CornerRadius(trackRadius),
                            )
                        }
                        val thumbRadius = 6.dp.toPx()
                        val thumbCenterX = thumbRadius + (size.width - thumbRadius * 2f) * clamped
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius,
                            center = Offset(thumbCenterX, centerY),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = formatTime(position), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "-${formatTime((duration - position).coerceAtLeast(0L))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 控制栏：随机 / 上一首 / 播放暂停 / 下一首 / 循环
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle),
                        contentDescription = "随机播放",
                        tint = if (shuffleEnabled) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "上一首",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))),
                        )
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                        ),
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "下一首",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    val repeatIcon = when (repeatMode) {
                        RepeatMode.OFF -> R.drawable.ic_repeat
                        RepeatMode.ALL -> R.drawable.ic_repeat
                        RepeatMode.ONE -> R.drawable.ic_repeat_one
                    }
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "循环模式",
                        tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** 标签行里的小胶囊：半透明白底 +（可选）小图标 + 文字。 */
@Composable
private fun MusicTag(@DrawableRes icon: Int?, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(text = text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private val DRAG_DISMISS_THRESHOLD = 120.dp
