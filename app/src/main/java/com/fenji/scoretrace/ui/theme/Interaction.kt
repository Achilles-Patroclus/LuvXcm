package com.fenji.scoretrace.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 按下时轻微缩放（OriginOS 式微动效）。
 *
 * 只改写 [graphicsLayer] 的缩放，不改变布局参数，因此不会触发布局或大范围重组。
 *
 * 如果需要和 `clickable` / `combinedClickable` 联动，务必把**同一个** [interactionSource]
 * 同时传给二者（用 [rememberPressSource] 创建），否则按压状态各归各的，缩放不会响应。
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.98f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** 与 [pressScale] 配合使用的交互源，方便在 `clickable` 与缩放之间共享按压状态。 */
@Composable
fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }
