package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.screen.score.ScoreSegment
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.util.Locale

/** 班级分数段分布卡：标题 + 柱状图 + 说明文字。 */
@Composable
fun ScoreDistributionCard(
    segments: List<ScoreSegment>,
    myScore: Int,
    /** 本班人数；null 时不显示人数 */
    totalClassStudents: Int?,
    modifier: Modifier = Modifier,
) {
    val mySegment = segments.firstOrNull { it.isMine }
    val surpassCount = segments.takeWhile { !it.isMine }.sumOf { it.count }
    val surpassRate = if (totalClassStudents != null && totalClassStudents > 0) {
        surpassCount.toFloat() / totalClassStudents
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row {
            Text(
                text = "本班分数段分布",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (totalClassStudents != null) {
                Text(
                    text = "共 $totalClassStudents 人",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ScoreTraceColors.PageBackgroundLight)
                    .padding(12.dp),
            ) {
                Text(
                    text = "数据不足",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            }
        } else {
            ScoreDistributionChart(segments = segments, myScore = myScore)

            if (mySegment != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScoreTraceColors.PageBackgroundLight)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "你位于 ${mySegment.label} 分数段（本班人数最多的一档），超过全班 " +
                            "${String.format(Locale.US, "%.0f", surpassRate * 100)}% 的同学。",
                        fontSize = 13.sp,
                        color = ScoreTraceColors.TextSecondaryLight,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}
