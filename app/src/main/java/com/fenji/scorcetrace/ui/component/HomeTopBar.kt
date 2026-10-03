package com.fenji.scorcetrace.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 首页顶部标题栏。
 * 左：ScoreTrace 标题（Score 深色 + Trace 品牌蓝）+ 副标题
 * 右：音乐胶囊（蓝色渐变）+ 通知铃铛（白色圆形，带红点）
 */
@Composable
fun HomeTopBar(
    subtitle: String,
    isMusicPlaying: Boolean,
    onMusicClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧标题区
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = "Score",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Text(
                    text = "Trace",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.BrandPrimary,
                )
            }
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }

        // 右侧操作区
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 音乐胶囊：与通知铃铛同为 36dp 高，保证两者水平中线对齐
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(ScoreTraceColors.BrandGradient))
                    .clickable(onClick = onMusicClick)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = if (isMusicPlaying) "音乐播放中" else "音乐",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // 播放时跳动的音柱
                    AnimatedMusicBars(
                        isPlaying = isMusicPlaying,
                        modifier = Modifier.height(16.dp),
                    )
                }
            }

            // 通知铃铛
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "通知",
                    tint = ScoreTraceColors.TextPrimaryLight,
                    modifier = Modifier.size(18.dp),
                )
                // 红点
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ScoreTraceColors.ErrorRed),
                )
            }
        }
    }
}

private const val BAR_MIN_HEIGHT = 4f
private const val BAR_MAX_HEIGHT = 16f
private const val BAR_PAUSED_HEIGHT = 5f

/**
 * 三根错峰跳动的音柱：每根 700ms 循环（4→16→8→14→4 四个关键帧，模拟真实音频起伏），
 * 相邻音柱错峰 150ms 启动，播放时循环、暂停时静止在 5dp。
 */
@Composable
private fun AnimatedMusicBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
) {
    val transition = rememberInfiniteTransition(label = "musicBars")
    val heights = List(barCount) { index ->
        transition.animateFloat(
            initialValue = BAR_MIN_HEIGHT,
            targetValue = BAR_MIN_HEIGHT,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 700
                    (BAR_MIN_HEIGHT at 0).using(FastOutSlowInEasing)
                    (BAR_MAX_HEIGHT at 200).using(FastOutSlowInEasing)
                    (8f at 400).using(FastOutSlowInEasing)
                    (14f at 550).using(FastOutSlowInEasing)
                    (BAR_MIN_HEIGHT at 700).using(FastOutSlowInEasing)
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(index * 150),
            ),
            label = "bar$index",
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((if (isPlaying) anim.value else BAR_PAUSED_HEIGHT).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White),
            )
        }
    }
}
