package com.fenji.scorcetrace.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.ui.theme.pressScale
import com.fenji.scorcetrace.ui.theme.rememberPressSource

/**
 * 空状态。
 *
 * 由「手绘插画 + 主文案 + 引导副文案 + 一键操作」组成：
 * - 插画用 Canvas 手绘（渐变圆底 + 卡片剪影 + 强调勾选），不依赖任何图片资源；
 * - 带克制的呼吸动画（缩放 0.96→1.0、柔光 alpha 0.10→0.18，3s 往返）。
 *
 * [message] 与 [action] 保留旧签名语义：只传 message 时退化为「插画 + 主文案」的简洁变体。
 */
@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    message: String = "暂无数据",
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PageHorizontal, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyIllustration()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            GradientPillButton(text = actionLabel, onClick = onAction)
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(12.dp))
            action()
        }
    }
}

/** 手绘插画：渐变柔光 + 卡片剪影 + 勾选强调。整块为装饰性图形，不参与无障碍朗读。 */
@Composable
private fun EmptyIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "emptyBreath")
    // 用 State 持有动画值，在绘制 lambda 内读取，避免每帧重组
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emptyBreath",
    )

    Canvas(
        modifier = modifier
            .size(132.dp)
            .clearAndSetSemantics { }
            .graphicsLayer {
                val s = 0.96f + 0.04f * breath.value
                scaleX = s
                scaleY = s
            },
    ) {
        val w = size.minDimension
        val c = center
        val glow = 0.10f + 0.08f * breath.value

        // 1) 渐变柔光圆底
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ScoreTraceColors.AccentCyan.copy(alpha = glow), Color.Transparent),
                center = c,
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = c,
        )

        // 2) 渐变圆盘
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    ScoreTraceColors.AccentBlue.copy(alpha = 0.26f),
                    ScoreTraceColors.AccentCyan.copy(alpha = 0.26f),
                ),
            ),
            radius = w * 0.33f,
            center = c,
        )

        // 3) 微微倾斜的卡片剪影 + 内容线条
        rotate(degrees = -8f, pivot = c) {
            val sheetW = w * 0.40f
            val sheetH = w * 0.50f
            val topLeft = Offset(c.x - sheetW / 2f, c.y - sheetH / 2f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.94f),
                topLeft = topLeft,
                size = Size(sheetW, sheetH),
                cornerRadius = CornerRadius(w * 0.06f),
            )
            val lineX = topLeft.x + sheetW * 0.18f
            val lineW = sheetW * 0.64f
            val lineH = w * 0.028f
            val gap = sheetH * 0.20f
            val lineColors = listOf(
                ScoreTraceColors.BrandBlue.copy(alpha = 0.55f),
                ScoreTraceColors.BrandBlue.copy(alpha = 0.30f),
                ScoreTraceColors.BrandBlue.copy(alpha = 0.18f),
            )
            lineColors.forEachIndexed { index, color ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(lineX, topLeft.y + sheetH * 0.24f + gap * index),
                    size = Size(lineW * (1f - index * 0.18f), lineH),
                    cornerRadius = CornerRadius(lineH / 2f),
                )
            }
        }

        // 4) 右下角强调勾选
        val dot = Offset(c.x + w * 0.25f, c.y + w * 0.23f)
        drawCircle(color = ScoreTraceColors.AccentCyan, radius = w * 0.11f, center = dot)
        val check = Path().apply {
            moveTo(dot.x - w * 0.05f, dot.y)
            lineTo(dot.x - w * 0.015f, dot.y + w * 0.04f)
            lineTo(dot.x + w * 0.055f, dot.y - w * 0.045f)
        }
        drawPath(
            path = check,
            color = ScoreTraceColors.OnAccentDeep,
            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round),
        )
    }
}

/** 渐变胶囊按钮：≥48dp 触摸热区 + 按压缩放。 */
@Composable
private fun GradientPillButton(text: String, onClick: () -> Unit) {
    val interaction = rememberPressSource()
    Box(
        modifier = Modifier
            .pressScale(interactionSource = interaction)
            .clip(CircleShape)
            .background(Brush.linearGradient(ScoreTraceColors.CoverGradient))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
            )
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = ScoreTraceColors.OnAccentDeep,
            maxLines = 1,
        )
    }
}
