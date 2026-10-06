package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.screen.score.FilterSort
import com.fenji.scoretrace.ui.screen.score.FilterTimeRange
import com.fenji.scoretrace.ui.screen.score.ScoreFilterState
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

private const val SUBJECT_ALL = "全部"

/**
 * 成绩页筛选 BottomSheet：科目 / 时间范围 / 排序三组单选 + 「重置」/「确定」。
 *
 * 只在点「确定」时通过 [onApply] 回传草稿；重置把草稿恢复为默认值。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreFilterSheet(
    subjects: List<String>,
    initial: ScoreFilterState,
    onApply: (ScoreFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScoreTraceColors.CardBackgroundLight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "筛选",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "重置",
                    fontSize = 14.sp,
                    color = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { draft = ScoreFilterState() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            FilterGroup(title = "科目") {
                FilterChipRow(
                    options = listOf(SUBJECT_ALL) + subjects,
                    selected = draft.subject ?: SUBJECT_ALL,
                    onSelect = { label ->
                        draft = draft.copy(subject = label.takeIf { it != SUBJECT_ALL })
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilterGroup(title = "时间范围") {
                FilterChipRow(
                    options = FilterTimeRange.values().map { it.label },
                    selected = draft.timeRange.label,
                    onSelect = { label ->
                        draft = draft.copy(timeRange = FilterTimeRange.values().first { it.label == label })
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilterGroup(title = "排序方式") {
                FilterChipRow(
                    options = FilterSort.values().map { it.label },
                    selected = draft.sort.label,
                    onSelect = { label ->
                        draft = draft.copy(sort = FilterSort.values().first { it.label == label })
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                        ),
                    )
                    .clickable { onApply(draft) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "确定",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun FilterChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) ScoreTraceColors.BrandPrimary
                        else ScoreTraceColors.PageBackgroundLight
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Color.White else ScoreTraceColors.TextPrimaryLight,
                    maxLines = 1,
                )
            }
        }
    }
}
