package com.fenji.scoretrace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 快捷功能项 */
data class QuickAction(
    val label: String,
    val icon: Painter,
    val color: Color,
    val onClick: () -> Unit,
)

/**
 * 快捷功能区：四个圆角功能入口。
 * AI 录成绩（蓝）/ 学习计时器（绿）/ 错题本（橙）/ 学习计划（紫）
 */
@Composable
fun QuickActions(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        actions.forEach { action ->
            QuickActionItem(action)
        }
    }
}

@Composable
private fun QuickActionItem(action: QuickAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = action.onClick),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(action.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = action.icon,
                contentDescription = action.label,
                tint = action.color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            text = action.label,
            fontSize = 9.sp,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}
