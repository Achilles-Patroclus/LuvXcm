package com.fenji.scorcetrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.ui.screen.score.TrendPoint
import com.fenji.scorcetrace.ui.screen.score.TrendRange
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/** 总分趋势卡：时间范围切换 + 折线图 + 目标虚线 + 底部统计。 */
@Composable
fun TotalTrendCard(
    points: List<TrendPoint>,
    targetScore: Int,
    selectedRange: TrendRange,
    onRangeChange: (TrendRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "总分趋势",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrendRange.values().forEach { range ->
                    val selected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) ScoreTraceColors.BrandPrimary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onRangeChange(range) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = range.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LineChart(points = points, targetScore = targetScore)

        Spacer(modifier = Modifier.height(8.dp))

        val totalGain = if (points.size >= 2) points.last().score - points.first().score else 0
        val gapToTarget = if (points.isEmpty()) 0 else (targetScore - points.last().score).coerceAtLeast(0)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${points.size}次累计 ${if (totalGain >= 0) "+" else ""}$totalGain 分",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.SuccessGreen,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "距目标还差 $gapToTarget 分",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.WarningOrange,
            )
        }
    }
}
