package com.fenji.scorcetrace.ui.music.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.data.player.TrackInfo
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 首页音乐播放器「迷你条」。
 *
 * 单行 64–72dp：左侧小封面、中间单行歌名 + 歌手（可点开播放列表）、右侧「上一首 / 播放暂停 / 下一首」。
 * 保持深色卡片语汇（沿用 [ScoreTraceColors.CardGradientDark]），但刻意压低视觉重量，
 * 让倒计时卡片始终是首页第一焦点。进度条与状态胶囊已移除；
 * 出错时用红色错误文案替换歌名行（不改变卡片高度，避免布局跳动）。
 */
@Composable
fun MusicPlayerCard(
    title: String,
    artist: String,
    isPlaying: Boolean,
    errorMessage: String?,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 播放按钮底色在亮青与纯白之间平滑过渡
    val playButtonColor by animateColorAsState(
        targetValue = if (isPlaying) ScoreTraceColors.AccentCyan else Color.White,
        animationSpec = tween(durationMillis = 320),
        label = "playButtonColor",
    )
    val playIconColor by animateColorAsState(
        targetValue = if (isPlaying) ScoreTraceColors.OnAccentDeep else ScoreTraceColors.OnLightDeep,
        animationSpec = tween(durationMillis = 320),
        label = "playIconColor",
    )

    val titleText = errorMessage ?: title
    val titleColor = if (errorMessage != null) ScoreTraceColors.ErrorSoft else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp, max = 72.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ScoreTraceColors.HairlineBorder),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(ScoreTraceColors.CardGradientDark))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 封面
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(ScoreTraceColors.CoverGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── 歌名 / 错误文案 + 歌手（单行，超出省略；整列可点开播放列表）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.SmallCorner))
                    .clickable(onClick = onTitleClick),
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScoreTraceColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // ── 上一首
            SkipButton(
                icon = painterResource(R.drawable.ic_skip_previous),
                contentDescription = "上一首",
                onClick = onPrevious,
            )

            // ── 播放/暂停：48dp 触摸热区包裹 32dp 视觉按钮（图标 18dp）
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(Dimens.MinTouchTarget),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(playButtonColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = if (isPlaying) {
                            painterResource(R.drawable.ic_pause)
                        } else {
                            rememberVectorPainter(Icons.Rounded.PlayArrow)
                        },
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = playIconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── 下一首
            SkipButton(
                icon = painterResource(R.drawable.ic_skip_next),
                contentDescription = "下一首",
                onClick = onNext,
            )
        }
    }
}

/**
 * 上一首 / 下一首按钮。
 *
 * 用 40dp 的 `Box + clickable` 而不是 [IconButton]：后者会强制 48dp 最小热区，
 * 三枚按钮并排会把中间的歌名挤到折行。这里刻意压到 40dp 以适配 64–72dp 的迷你条。
 */
@Composable
private fun SkipButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = ScoreTraceColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * 曲目列表弹层（[ModalBottomSheet]）。
 *
 * 标题右侧显示曲目数量，当前曲目以强调色高亮 + 跳动音柱 + 「播放中」，项间 0.5dp 分隔线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlaylistSheet(
    tracks: List<TrackInfo>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "播放列表",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${tracks.size} 首",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            tracks.forEachIndexed { index, track ->
                PlaylistRow(
                    track = track,
                    selected = index == currentIndex,
                    onClick = { onSelect(index) },
                )
                if (index < tracks.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    track: TrackInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        ScoreTraceColors.AccentCyan
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                PlayingBarsAnimation()
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = if (selected) {
                    ScoreTraceColors.AccentCyan.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            Text(
                text = "播放中",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.AccentCyan,
                maxLines = 1,
            )
        }
    }
}

/** 「播放中」的跳动音柱：三根青条各自反向缩放，比静态图标更有生命感。 */
@Composable
private fun PlayingBarsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "playingBars")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar1Height.dp)
                .background(ScoreTraceColors.AccentCyan, RoundedCornerShape(1.dp)),
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar2Height.dp)
                .background(ScoreTraceColors.AccentCyan, RoundedCornerShape(1.dp)),
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar3Height.dp)
                .background(ScoreTraceColors.AccentCyan, RoundedCornerShape(1.dp)),
        )
    }
}
