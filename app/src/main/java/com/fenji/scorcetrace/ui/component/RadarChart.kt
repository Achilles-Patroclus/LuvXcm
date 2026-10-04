package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * 六维雷达图（Canvas 自绘，不引入第三方图表库）。
 * @param labels 六个维度的标签（按顺时针从顶部开始）
 * @param values 各维度的值（0~1）
 */
@Composable
fun RadarChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = ScoreTraceColors.TextSecondaryLight)
    val emptyStyle = TextStyle(fontSize = 11.sp, color = ScoreTraceColors.TextTertiaryLight)
    val gridColor = Color(0xFFE2E8F0)
    // 六科全无数据时视为空态：不画数据多边形与数据点，改为在中心提示
    val isEmpty = values.all { it <= 0.01f }

    Canvas(modifier = modifier) {
        val n = labels.size
        if (n == 0) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.33f
        val angleStep = (2 * Math.PI / n).toFloat()
        val startAngle = -Math.PI / 2 // 从顶部开始，顺时针

        // 网格（3 层多边形）
        for (layer in 1..3) {
            val r = radius * layer / 3
            val path = Path()
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
                else path.lineTo(x.toFloat(), y.toFloat())
            }
            path.close()
            drawPath(path, gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // 轴线
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            drawLine(
                color = gridColor,
                start = center,
                end = Offset(x.toFloat(), y.toFloat()),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // 数据多边形 / 空态提示
        if (isEmpty) {
            val textLayout = textMeasurer.measure("暂无成绩", emptyStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    center.x - textLayout.size.width / 2f,
                    center.y - textLayout.size.height / 2f,
                ),
            )
        } else if (values.size == n) {
            val dataPath = Path()
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val r = radius * values[i].coerceIn(0f, 1f)
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                if (i == 0) dataPath.moveTo(x.toFloat(), y.toFloat())
                else dataPath.lineTo(x.toFloat(), y.toFloat())
            }
            dataPath.close()
            drawPath(dataPath, ScoreTraceColors.BrandPrimary.copy(alpha = 0.15f))
            drawPath(dataPath, ScoreTraceColors.BrandPrimary, style = Stroke(width = 2.dp.toPx()))
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val r = radius * values[i].coerceIn(0f, 1f)
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                drawCircle(
                    color = ScoreTraceColors.BrandPrimary,
                    radius = 3.dp.toPx(),
                    center = Offset(x.toFloat(), y.toFloat()),
                )
            }
        }

        // 标签
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val labelRadius = radius + size.minDimension * 0.11f
            val x = center.x + labelRadius * cos(angle)
            val y = center.y + labelRadius * sin(angle)
            val textLayout = textMeasurer.measure(labels[i], labelStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x.toFloat() - textLayout.size.width / 2f,
                    y.toFloat() - textLayout.size.height / 2f,
                ),
            )
        }
    }
}
