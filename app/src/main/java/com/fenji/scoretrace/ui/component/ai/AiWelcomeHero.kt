package com.fenji.scoretrace.ui.component.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * AI 助手欢迎区中央视觉。
 * 蓝色渐变圆角方形机器人图标 + 多层光晕 + 欢迎标题 + 副标题。
 */
@Composable
fun AiWelcomeHero(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawBehind {
                    // 外层光晕：超大半径、极低透明度，形成柔和扩散（无硬边界）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ScoreTraceColors.BrandPrimary.copy(alpha = 0.15f),
                                ScoreTraceColors.BrandPrimaryLight.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.maxDimension / 2,
                        ),
                    )
                    // 内层光晕：紧贴图标，稍高透明度
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ScoreTraceColors.BrandPrimaryLight.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.maxDimension / 3,
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(ScoreTraceColors.BrandGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_smart_toy),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "你好，我是你的高考 AI 助手",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "解题、规划、分析，随时问我",
            fontSize = 14.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            textAlign = TextAlign.Center,
        )
    }
}
