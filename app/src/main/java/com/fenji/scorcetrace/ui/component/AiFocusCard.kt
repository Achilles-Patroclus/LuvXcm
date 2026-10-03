package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 今日 AI 重点卡。
 * 左侧蓝色竖条 + 机器人图标；右侧标题行（标题 + 「换一条」）+ 正文。
 */
@Composable
fun AiFocusCard(
    title: String,
    content: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            // 左侧蓝色竖条：卡片高度由内容决定，只能用绘制而非 Layout
            .drawBehind {
                drawRoundRect(
                    color = ScoreTraceColors.BrandPrimary,
                    size = size.copy(width = 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 机器人图标
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_smart_toy),
                contentDescription = null,
                tint = ScoreTraceColors.BrandPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        // 右侧：标题行 + 正文
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.weight(1f),
                )
                // 「换一条」放在标题行右侧，避免挤压正文宽度
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScoreTraceColors.PageBackgroundLight)
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "换一条",
                        tint = ScoreTraceColors.TextSecondaryLight,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("换一条", fontSize = 10.sp, color = ScoreTraceColors.TextSecondaryLight)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = content,
                fontSize = 11.sp,
                color = ScoreTraceColors.TextPrimaryLight,
                lineHeight = 16.sp,
            )
        }
    }
}
