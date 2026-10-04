package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

/** 考试概览卡：环形得分率 + 考试信息 + 总分 + 排名的三个数据块。 */
@Composable
fun ExamOverviewCard(
    examName: String,
    examDate: String,
    totalStudents: Int,
    totalScore: Int,
    fullScore: Int,
    totalRate: Float,
    classRank: Int,
    gradeRank: Int,
    deltaFromLast: Int,
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
            CircularProgress(progress = totalRate, size = 96)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = examName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$examDate · 全级 $totalStudents 人",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = totalScore.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreTraceColors.BrandPrimary,
                        lineHeight = 40.sp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ $fullScore",
                        fontSize = 16.sp,
                        color = ScoreTraceColors.TextSecondaryLight,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock(label = "班级排名", value = classRank.toString(), modifier = Modifier.weight(1f))
            StatBlock(label = "年级排名", value = gradeRank.toString(), modifier = Modifier.weight(1f))
            DeltaBlock(delta = deltaFromLast, modifier = Modifier.weight(1f))
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

@Composable
private fun DeltaBlock(delta: Int, modifier: Modifier = Modifier) {
    val accent = if (delta >= 0) ScoreTraceColors.SuccessGreen else ScoreTraceColors.ErrorRed
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (delta >= 0) "↑$delta" else "↓${-delta}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("较上次", fontSize = 11.sp, color = ScoreTraceColors.TextSecondaryLight)
    }
}
