package com.fenji.scoretrace.ui.component.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * AI 助手页面顶部标题栏。
 * 左：圆形渐变 AI 头像 + "AI 助手" + 动态网络状态标签（在线绿点 / 离线灰点）
 * 右：新建对话、历史记录两个白色圆形按钮
 */
@Composable
fun AiTopBar(
    isOnline: Boolean,
    onNewChat: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "AI 助手",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.width(10.dp))
        OnlineBadge(isOnline = isOnline)

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TopBarCircleButton(
                iconRes = R.drawable.ic_chat_add,
                contentDescription = "新建对话",
                onClick = onNewChat,
            )
            TopBarCircleButton(
                iconRes = R.drawable.ic_history,
                contentDescription = "历史记录",
                onClick = onHistoryClick,
            )
        }
    }
}

/** 顶部圆形渐变 AI 头像，与欢迎区大图标同款蓝青渐变。 */
@Composable
private fun AiAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(ScoreTraceColors.BrandGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_smart_toy),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 网络状态徽章：在线绿点 + 「在线」，离线灰点 + 「离线」。 */
@Composable
private fun OnlineBadge(isOnline: Boolean) {
    val accent = if (isOnline) ScoreTraceColors.SuccessGreen else ScoreTraceColors.TextTertiaryLight
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOnline) "在线" else "离线",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
        )
    }
}

@Composable
private fun TopBarCircleButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.size(20.dp),
        )
    }
}
