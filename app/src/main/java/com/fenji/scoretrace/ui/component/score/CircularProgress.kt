package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.util.Locale

/** Canvas 环形进度条：蓝→青渐变圆弧 + 中心得分率。 */
@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Int = 96,
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.dp.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(ScoreTraceColors.BrandPrimary, Color(0xFF06B6D4)),
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f%%", progress * 100),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.BrandPrimary,
            )
            Text(
                text = "得分率",
                fontSize = 10.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
    }
}
