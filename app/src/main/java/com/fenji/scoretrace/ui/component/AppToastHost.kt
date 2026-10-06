package com.fenji.scoretrace.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.LocalAppDarkTheme
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.AppToastType
import com.fenji.scoretrace.util.ToastItem

/**
 * 全局轻提示宿主：把 [AppToast.visible] 渲染成底部居中、自下而上堆叠的深色卡片。
 *
 * 挂在根内容层（MainActivity）最上方，任何页面（含隐藏底栏的全屏页）都能看到。
 * 新提示从底部滑入并轻微放大；关闭时向下滑出。点击任意一条可立即关闭。
 *
 * @param bottomPadding 距屏幕底部的额外间距（不含系统导航栏）：底栏可见时调用方传「底栏高度 + 间隔」，
 *   隐藏底栏的全屏页传更小的值。[navigationBarsPadding] 另行避让系统手势条。
 */
@Composable
fun AppToastHost(
    bottomPadding: Dp = 80.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = bottomPadding)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppToast.visible.forEach { item ->
            key(item.id) {
                AnimatedVisibility(
                    visible = !item.leaving,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    ) + fadeIn() + scaleIn(initialScale = 0.92f),
                    exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
                ) {
                    ToastCard(item)
                }
            }
        }
    }
}

@Composable
private fun ToastCard(item: ToastItem) {
    val dark = LocalAppDarkTheme.current
    // 深色主题下用更亮的 slate 灰、并补一条淡描边，避免与深色页底「糊」在一起（阴影在深色上几乎不可见）
    val container = if (dark) Color(0xFF334155) else Color(0xFF1E293B)
    val tint = when (item.type) {
        AppToastType.Success -> ScoreTraceColors.SuccessGreen
        AppToastType.Error -> ScoreTraceColors.ErrorRed
        AppToastType.Warning -> ScoreTraceColors.WarningOrange
        AppToastType.Info -> ScoreTraceColors.BrandPrimary
    }
    val icon = when (item.type) {
        AppToastType.Success -> Icons.Rounded.CheckCircle
        AppToastType.Error -> Icons.Rounded.Close
        AppToastType.Warning -> Icons.Rounded.Warning
        AppToastType.Info -> Icons.Rounded.Info
    }

    Surface(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .clickable { AppToast.dismiss(item.id) },
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = Color.White,
        shadowElevation = 10.dp,
        border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
