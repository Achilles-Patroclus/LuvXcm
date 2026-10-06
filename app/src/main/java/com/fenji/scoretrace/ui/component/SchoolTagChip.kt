package com.fenji.scoretrace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 院校层次标签胶囊：985 / 211 / 双一流 紫、本科 蓝、专科 灰蓝，浅深主题下均保证对比度。 */
@Composable
fun SchoolTagChip(text: String, modifier: Modifier = Modifier) {
    val accent: Color = when (text) {
        "本科" -> ScoreTraceColors.TagUndergraduate
        "专科" -> ScoreTraceColors.TagVocational
        else -> ScoreTraceColors.TagElite
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
        )
    }
}
