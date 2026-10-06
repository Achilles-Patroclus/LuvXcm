package com.fenji.scoretrace.ui.component.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

/** 主题模式取值（与 UserPreferences 的存储值一致）。 */
const val THEME_MODE_LIGHT = "light"
const val THEME_MODE_DARK = "dark"
const val THEME_MODE_SYSTEM = "system"

/** 主题模式的中文名。 */
fun themeModeLabel(mode: String): String = when (mode) {
    THEME_MODE_LIGHT -> "浅色"
    THEME_MODE_DARK -> "深色"
    else -> "跟随系统"
}

/** 主题选择弹层：浅色 / 深色 / 跟随系统 三选一，选中项高亮。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "主题",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.size(12.dp))
            ThemeModeOption(THEME_MODE_LIGHT, current, onSelect, onDismiss)
            ThemeModeOption(THEME_MODE_DARK, current, onSelect, onDismiss)
            ThemeModeOption(THEME_MODE_SYSTEM, current, onSelect, onDismiss)
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: String,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = mode == current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) ScoreTraceColors.BrandPrimary.copy(alpha = 0.08f) else Color.Transparent,
            )
            .clickable {
                onSelect(mode)
                onDismiss()
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = themeModeLabel(mode),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = ScoreTraceColors.BrandPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
