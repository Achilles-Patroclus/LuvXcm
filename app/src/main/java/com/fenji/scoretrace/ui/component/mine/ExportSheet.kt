package com.fenji.scoretrace.ui.component.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.ScoreExporter

/** 导出成绩弹层：选择导出格式（CSV / JSON）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    onSelectFormat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "导出成绩",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "文件将保存到「下载/ScoreTrace」目录",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextTertiaryLight,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExportFormatOption(
                title = "CSV",
                subtitle = "可用 Excel / 表格软件打开",
                onClick = { onSelectFormat(ScoreExporter.FORMAT_CSV) },
            )
            ExportFormatOption(
                title = "JSON",
                subtitle = "结构化数据，便于程序处理",
                onClick = { onSelectFormat(ScoreExporter.FORMAT_JSON) },
            )
        }
    }
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextTertiaryLight,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
