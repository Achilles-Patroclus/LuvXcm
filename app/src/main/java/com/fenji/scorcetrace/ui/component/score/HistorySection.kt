package com.fenji.scorcetrace.ui.component.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.ui.screen.score.ExamHistoryItem
import com.fenji.scorcetrace.ui.screen.score.HistoryGroup
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/** 历史记录区域：标题行 + 按月分组卡片 + 底部录入引导卡。 */
@Composable
fun HistorySection(
    groups: List<HistoryGroup>,
    onDelete: (ExamHistoryItem) -> Unit,
    onItemClick: (ExamHistoryItem) -> Unit,
    onGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCount = groups.sumOf { it.items.size }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "历史记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "共 $totalCount 次 · 左滑删除",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextTertiaryLight,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        groups.forEach { group ->
            Text(
                text = group.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextTertiaryLight,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.items.forEach { item ->
                    HistoryRecordCard(
                        item = item,
                        onDelete = { onDelete(item) },
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        RecordGuideCard(
            onClick = onGuideClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
