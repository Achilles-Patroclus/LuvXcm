package com.fenji.scorcetrace.ui.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/** 一科的成绩概览数据 */
data class SubjectScore(
    val name: String,
    /** 得分率 0~1 */
    val rate: Float,
    val color: Color,
)

/**
 * 成绩概览卡。
 * 左：六维雷达图
 * 右：六科进度条 + 百分比
 * 底部：总分·排名 + 较上次变化标签
 */
@Composable
fun ScoreOverviewCard(
    subjects: List<SubjectScore>,
    totalScore: String,
    rankText: String,
    deltaText: String,
    deltaPositive: Boolean,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // 标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "成绩概览",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(onClick = onDetailClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("详情", fontSize = 14.sp, color = ScoreTraceColors.BrandPrimary)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 雷达图 + 进度条
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadarChart(
                labels = subjects.map { it.name },
                values = subjects.map { it.rate },
                modifier = Modifier.size(140.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subjects.forEach { subject ->
                    SubjectProgressRow(subject)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 底部总分栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ScoreTraceColors.PageBackgroundLight)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "总分 $totalScore · $rankText",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (deltaPositive) {
                            ScoreTraceColors.SuccessGreen.copy(alpha = 0.12f)
                        } else {
                            ScoreTraceColors.ErrorRed.copy(alpha = 0.12f)
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = deltaText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (deltaPositive) ScoreTraceColors.SuccessGreen else ScoreTraceColors.ErrorRed,
                )
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(subject: SubjectScore) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = subject.name,
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.width(32.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ScoreTraceColors.CardBorderLight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(subject.rate.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(subject.color),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(subject.rate * 100).toInt()}%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = subject.color,
            modifier = Modifier.width(36.dp),
        )
    }
}
