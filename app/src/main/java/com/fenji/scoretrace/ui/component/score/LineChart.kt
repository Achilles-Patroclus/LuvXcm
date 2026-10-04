package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.screen.score.TrendPoint
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * Canvas 折线图（不引入第三方图表库）。
 *
 * @param points 数据点（按时间正序）
 * @param targetScore 目标分数，画一条红色虚线
 * @param yMin Y 轴最小值
 * @param yMax Y 轴最大值，[yMin, yMax] 之间四等分画网格线
 */
@Composable
fun LineChart(
    points: List<TrendPoint>,
    targetScore: Int,
    yMin: Int = 400,
    yMax: Int = 700,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = ScoreTraceColors.TextTertiaryLight)
    val valueStyle = TextStyle(fontSize = 12.sp, color = ScoreTraceColors.TextSecondaryLight)
    val targetStyle = TextStyle(fontSize = 11.sp, color = ScoreTraceColors.ErrorRed)
    val gridColor = Color(0xFFE2E8F0)
    val range = (yMax - yMin).coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val chartLeft = 40.dp.toPx()
        val chartRight = size.width - 16.dp.toPx()
        val chartTop = 16.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        fun yFor(value: Int): Float =
            chartBottom - ((value - yMin).toFloat() / range * chartHeight)

        // Y 轴网格线：量程四等分
        val ticks = List(4) { i -> yMin + range * i / 3 }
        ticks.forEach { value ->
            val y = yFor(value)
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            val layout = textMeasurer.measure(value.toString(), labelStyle)
            drawText(layout, topLeft = Offset(8.dp.toPx(), y - layout.size.height / 2f))
        }

        // 目标虚线
        if (targetScore in yMin..yMax) {
            val targetY = yFor(targetScore)
            val dashWidth = 6.dp.toPx()
            val dashGap = 4.dp.toPx()
            var x = chartLeft
            while (x < chartRight) {
                drawLine(
                    color = ScoreTraceColors.ErrorRed,
                    start = Offset(x, targetY),
                    end = Offset(minOf(x + dashWidth, chartRight), targetY),
                    strokeWidth = 2.dp.toPx(),
                )
                x += dashWidth + dashGap
            }
            val layout = textMeasurer.measure("目标 $targetScore", targetStyle)
            drawText(
                layout,
                topLeft = Offset(chartRight - layout.size.width, targetY - layout.size.height - 4.dp.toPx()),
            )
        }

        if (points.isEmpty()) return@Canvas

        val coords = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                chartLeft + chartWidth / 2f
            } else {
                chartLeft + chartWidth * index / (points.size - 1)
            }
            Offset(x, yFor(point.score).coerceIn(chartTop, chartBottom))
        }

        // 渐变填充区
        val fillPath = Path().apply {
            moveTo(coords.first().x, chartBottom)
            coords.forEach { lineTo(it.x, it.y) }
            lineTo(coords.last().x, chartBottom)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                colors = listOf(
                    ScoreTraceColors.BrandPrimary.copy(alpha = 0.15f),
                    ScoreTraceColors.BrandPrimary.copy(alpha = 0.02f),
                ),
                startY = chartTop,
                endY = chartBottom,
            ),
        )

        // 折线
        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, ScoreTraceColors.BrandPrimary, style = Stroke(width = 3.dp.toPx()))

        // 数据点 + 数值标签（每个点都画）；X 轴标签抽样，避免点数多时重叠
        val maxLabels = 6
        val step = maxOf(1, (points.size + maxLabels - 1) / maxLabels)
        coords.forEachIndexed { index, coord ->
            drawCircle(Color.White, radius = 5.dp.toPx(), center = coord)
            drawCircle(
                color = ScoreTraceColors.BrandPrimary,
                radius = 5.dp.toPx(),
                center = coord,
                style = Stroke(width = 2.dp.toPx()),
            )
            if (index == coords.lastIndex) {
                drawCircle(ScoreTraceColors.BrandPrimary, radius = 5.dp.toPx(), center = coord)
            }

            val valueLayout = textMeasurer.measure(points[index].score.toString(), valueStyle)
            val valueX = when {
                index == coords.lastIndex -> coord.x - valueLayout.size.width + 6.dp.toPx()
                index == 0 -> coord.x - 6.dp.toPx()
                else -> coord.x - valueLayout.size.width / 2f
            }
            drawText(
                valueLayout,
                topLeft = Offset(valueX, coord.y - valueLayout.size.height - 8.dp.toPx()),
            )

            if (index == 0 || index == coords.lastIndex || index % step == 0) {
                val xLayout = textMeasurer.measure(points[index].monthLabel, labelStyle)
                val xLabelX = when {
                    index == coords.lastIndex -> coord.x - xLayout.size.width + 4.dp.toPx()
                    index == 0 -> coord.x - 4.dp.toPx()
                    else -> coord.x - xLayout.size.width / 2f
                }
                drawText(xLayout, topLeft = Offset(xLabelX, chartBottom + 8.dp.toPx()))
            }
        }
    }
}
