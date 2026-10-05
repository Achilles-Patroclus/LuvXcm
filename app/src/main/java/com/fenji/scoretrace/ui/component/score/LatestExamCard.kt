package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.util.Locale

/** 最近一次考试概览卡：考试名+日期、变化标签、总分、排名与得分率。 */
@Composable
fun LatestExamCard(
    examName: String,
    examDate: String,
    totalScore: Int,
    fullScore: Int,
    /** 较上次变化；null 时不显示变化标签 */
    delta: Int?,
    /** 班级排名；null 显示「—」 */
    classRank: Int?,
    /** 年级排名；null 显示「—」 */
    gradeRank: Int?,
    totalRate: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$examName · $examDate",
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (delta != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (delta >= 0) {
                                ScoreTraceColors.SuccessGreen.copy(alpha = 0.12f)
                            } else {
                                ScoreTraceColors.ErrorRed.copy(alpha = 0.12f)
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (delta >= 0) "↑$delta 分" else "↓${-delta} 分",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (delta >= 0) ScoreTraceColors.SuccessGreen else ScoreTraceColors.ErrorRed,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalScore.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.BrandPrimary,
                lineHeight = 48.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "/ $fullScore",
                fontSize = 18.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatBlock(label = "班级排名", value = classRank?.toString() ?: "—", modifier = Modifier.weight(1f))
            StatBlock(label = "年级排名", value = gradeRank?.toString() ?: "—", modifier = Modifier.weight(1f))
            StatBlock(
                label = "总得分率",
                value = String.format(Locale.US, "%.1f%%", totalRate * 100),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ScoreTraceColors.PageBackgroundLight)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = ScoreTraceColors.TextSecondaryLight)
    }
}
