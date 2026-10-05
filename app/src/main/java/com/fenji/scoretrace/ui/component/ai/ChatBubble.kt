package com.fenji.scoretrace.ui.component.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.screen.ai.ChatMessage
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 气泡与头像的形状/笔刷提为顶层常量，避免每次重组重新分配。 */
private val UserBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
private val AiBubbleShape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
private val AiAvatarShape = RoundedCornerShape(10.dp)
private val UserBubbleBrush = Brush.linearGradient(ScoreTraceColors.BrandGradient)
private val AiAvatarBrush = Brush.linearGradient(ScoreTraceColors.AiGradient)

/**
 * 单条对话气泡。
 * 用户消息靠右、品牌蓝底白字；AI 消息靠左、白底深字并带机器人头像。
 * AI 有深度思考内容时在其上方显示可折叠面板（当前 AI 不产出该字段，面板默认不出现）。
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == "user"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            AiBubbleAvatar()
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (!isUser && !message.thinking.isNullOrBlank()) {
                ThinkingPanel(thinking = message.thinking)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Box(
                modifier = Modifier
                    .clip(if (isUser) UserBubbleShape else AiBubbleShape)
                    .background(
                        brush = if (isUser) UserBubbleBrush else SolidColor(ScoreTraceColors.CardBackgroundLight),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (isUser) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color.White,
                    )
                } else {
                    AssistantContent(message = message)
                }
            }
        }
    }
}

/** AI 气泡正文：流式为纯文本追加闪烁光标，收尾后一次渲染（Markdown 渲染在模块 3 接入）。 */
@Composable
private fun AssistantContent(message: ChatMessage) {
    Column {
        if (message.content.isEmpty() && message.isStreaming) {
            Text(
                text = "思考中…",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        } else {
            MarkdownText(markdown = message.content)
        }

        if (message.isStreaming) {
            Spacer(modifier = Modifier.height(2.dp))
            BlinkingCursor()
        }
    }
}

/** 流式尾部闪烁光标。alpha 在绘制阶段读取，仅失效 layer、不触发重组。 */
@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha = transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Text(
        text = "▍",
        fontSize = 14.sp,
        color = ScoreTraceColors.TextPrimaryLight,
        modifier = Modifier.graphicsLayer { alpha = cursorAlpha.value },
    )
}

/** AI 头像：蓝青渐变圆角方块 + 头像 glyph。 */
@Composable
private fun AiBubbleAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(AiAvatarShape)
            .background(AiAvatarBrush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ai_logo),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 深度思考可折叠面板（占位）：[thinking] 为空时不渲染，待 AI 支持 thinking 字段后自动启用。 */
@Composable
private fun ThinkingPanel(thinking: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.06f))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "深度思考",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.BrandPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "收起" else "展开",
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Text(
                text = thinking,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
