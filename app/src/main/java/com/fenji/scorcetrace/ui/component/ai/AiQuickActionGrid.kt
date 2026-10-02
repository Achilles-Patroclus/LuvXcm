package com.fenji.scorcetrace.ui.component.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.ui.screen.ai.AiQuickAction
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * "试试这样问"六宫格快捷入口。
 * 2 列 × 3 行，每个卡片：左上彩色圆角图标 + 标题 + 副标题。
 *
 * 用两列 [Row] 手工排布而非 LazyVerticalGrid——外层是可滚动 Column，
 * 网格需随内容自适应高度，不能是自身可滚动的懒加载网格。
 */
@Composable
fun AiQuickActionGrid(
    actions: List<AiQuickAction>,
    onActionClick: (AiQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "试试这样问",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowActions.forEach { action ->
                        QuickActionCard(
                            action = action,
                            onClick = { onActionClick(action) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: AiQuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(action.iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(action.iconRes),
                contentDescription = action.title,
                tint = action.iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Column {
            Text(
                text = action.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.TextPrimaryLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = action.subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
