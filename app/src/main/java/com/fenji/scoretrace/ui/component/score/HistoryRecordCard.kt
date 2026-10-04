package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.screen.score.ExamHistoryItem
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.time.format.DateTimeFormatter

/** 单条考试历史卡片，支持左滑露出删除按钮。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRecordCard(
    item: ExamHistoryItem,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScoreTraceColors.ErrorRed),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outline),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("删除", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
        },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.date.format(DateTimeFormatter.ofPattern("MM-dd")),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Text(
                    text = item.date.year.toString(),
                    fontSize = 11.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.examName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.TextPrimaryLight,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "班 ${item.classRank} · 年 ${item.gradeRank}",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.totalScore.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.BrandPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.deltaFromLast >= 0) "↑${item.deltaFromLast}" else "↓${-item.deltaFromLast}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.deltaFromLast >= 0) ScoreTraceColors.SuccessGreen else ScoreTraceColors.ErrorRed,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = ScoreTraceColors.TextTertiaryLight,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
