package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 成绩详情顶部栏：返回 + 标题 + 编辑 + 删除。 */
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "返回", ScoreTraceColors.TextPrimaryLight, onBack)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "成绩详情",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.weight(1f))
        CircleIconButton(Icons.Filled.Edit, "编辑", ScoreTraceColors.TextPrimaryLight, onEdit)
        Spacer(modifier = Modifier.width(10.dp))
        CircleIconButton(Icons.Filled.Delete, "删除", ScoreTraceColors.ErrorRed, onDelete)
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDesc: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
