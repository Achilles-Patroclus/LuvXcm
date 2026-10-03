package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
            // 音乐胶囊
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(ScoreTraceColors.BrandGradient))
                    .clickable(onClick = onMusicClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    // 频谱图标用三个竖条模拟
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Box(
                            Modifier
                                .size(width = 3.dp, height = 8.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp)),
                        )
                        Box(
                            Modifier
                                .size(width = 3.dp, height = 12.dp)
                                .background(Color.White, RoundedCornerShape(2.dp)),
                        )
                        Box(
                            Modifier
                                .size(width = 3.dp, height = 6.dp)
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp)),
                        )
                    }
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
