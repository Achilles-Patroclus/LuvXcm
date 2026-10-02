package com.fenji.scorcetrace.ui.component.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.ui.screen.score.ScoreSegment
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/** 分数段分布柱状图：「我」所在段蓝→青高亮，其余浅灰。 */
@Composable
fun ScoreDistributionChart(
    segments: List<ScoreSegment>,
    myScore: Int,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = ScoreTraceColors.TextSecondaryLight)
    val countStyle = TextStyle(fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
    val mineLabelStyle = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

    val maxCount = segments.maxOfOrNull { it.count } ?: 1

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val chartLeft = 8.dp.toPx()
        val chartRight = size.width - 8.dp.toPx()
        val chartBottom = size.height - 32.dp.toPx()
        val chartTop = 24.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        val slot = chartWidth / segments.size
        val barWidth = slot * 0.6f
        val barGap = slot * 0.4f

        segments.forEachIndexed { index, segment ->
            val barHeight = (segment.count.toFloat() / maxCount) * chartHeight
            val barLeft = chartLeft + index * slot + barGap / 2
            val barTop = chartBottom - barHeight

            val barColor = if (segment.isMine) {
                Brush.verticalGradient(listOf(ScoreTraceColors.BrandPrimary, Color(0xFF06B6D4)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFEEF2F7), Color(0xFFE2E8F0)))
            }

            drawRoundRect(
                brush = barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )

            if (segment.isMine) {
                val mineLabel = textMeasurer.measure("我 $myScore", mineLabelStyle)
                val labelBgLeft = barLeft + barWidth / 2 - mineLabel.size.width / 2 - 6.dp.toPx()
                drawRoundRect(
                    color = ScoreTraceColors.BrandPrimary,
                    topLeft = Offset(labelBgLeft, barTop - mineLabel.size.height - 8.dp.toPx()),
                    size = Size(mineLabel.size.width + 12.dp.toPx(), mineLabel.size.height + 6.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                drawText(
                    textLayoutResult = mineLabel,
                    topLeft = Offset(
                        barLeft + barWidth / 2 - mineLabel.size.width / 2,
                        barTop - mineLabel.size.height - 5.dp.toPx(),
                    ),
                )
            }

            val countLabel = textMeasurer.measure("${segment.count}人", countStyle)
            drawText(
                textLayoutResult = countLabel,
                topLeft = Offset(
                    barLeft + barWidth / 2 - countLabel.size.width / 2,
                    chartBottom + 4.dp.toPx(),
                ),
            )

            val segLabel = textMeasurer.measure(segment.label, labelStyle)
            drawText(
                textLayoutResult = segLabel,
                topLeft = Offset(
                    barLeft + barWidth / 2 - segLabel.size.width / 2,
                    chartBottom + 20.dp.toPx(),
                ),
            )
        }
    }
}
