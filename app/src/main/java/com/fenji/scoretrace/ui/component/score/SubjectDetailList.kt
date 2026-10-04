package com.fenji.scoretrace.ui.component.score

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.screen.score.SubjectDetail
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.util.Locale

/** 各科明细卡：标题行 + 六科列表（分数/满分/得分率/班均/超越百分比/进度条）。 */
@Composable
fun SubjectDetailList(
    subjects: List<SubjectDetail>,
    onWrongQuestionsClick: () -> Unit,
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
                text = "各科明细",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(onClick = onWrongQuestionsClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "错题",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScoreTraceColors.BrandPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            subjects.forEach { subject ->
                SubjectDetailRow(subject)
            }
        }
    }
}

@Composable
private fun SubjectDetailRow(subject: SubjectDetail) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subject.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.width(36.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subject.score.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Text(
                text = " / ${subject.fullScore}",
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(subject.rate * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = subject.color,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "班均 ${subject.classAvg}",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ScoreTraceColors.CardBorderLight),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(subject.rate.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (subject.name == "语文" || subject.name == "数学") {
                                Brush.horizontalGradient(
                                    listOf(ScoreTraceColors.BrandPrimary, Color(0xFF06B6D4)),
                                )
                            } else {
                                SolidColor(subject.color)
                            },
                        ),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            val surpassColor = if (subject.surpassRate >= 0.5f) {
                ScoreTraceColors.SuccessGreen
            } else {
                ScoreTraceColors.ErrorRed
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(surpassColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "超越 ${String.format(Locale.US, "%.0f", subject.surpassRate * 100)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = surpassColor,
                )
            }
        }
    }
}
