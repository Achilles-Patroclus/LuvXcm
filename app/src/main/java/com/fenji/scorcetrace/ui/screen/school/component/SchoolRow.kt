package com.fenji.scorcetrace.ui.screen.school.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.fenji.scorcetrace.data.model.SchoolInfo
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 院校行（卡片内的头部内容，不含卡片边框/背景）。
 * 校徽优先在线加载，失败时回退为院校首字渐变块；
 * 选中态显示蓝色对勾、标签移到名称下方，未选中态标签靠右、末尾带一个空心单选圈。
 */
@Composable
fun SchoolRow(
    school: SchoolInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SchoolLogo(school = school, isSelected = isSelected)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = school.name,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "已选中",
                        tint = ScoreTraceColors.BrandPrimary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = listOf(school.province, school.city).filter { it.isNotBlank() }.joinToString("·"),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
            // 选中态：标签换行到名称下方
            if (isSelected && school.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    school.tags.take(3).forEach { TagChip(it) }
                }
            }
        }

        // 未选中态：标签靠右 + 空心单选圈
        if (!isSelected && school.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                school.tags.take(3).forEach { TagChip(it) }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        if (!isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                    .clickable(onClick = onClick),
            )
        }
    }
}

/** 校徽块：在线加载校徽，失败（无网络/无图）时回退为院校首字。 */
@Composable
private fun SchoolLogo(school: SchoolInfo, isSelected: Boolean) {
    var failed by remember(school.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        listOf(ScoreTraceColors.AccentBlue, ScoreTraceColors.SchoolPurple),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (school.logoUrl.isBlank() || failed) {
            Text(
                text = school.name.take(1),
                color = if (isSelected) Color.White else ScoreTraceColors.TextSecondaryLight,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            AsyncImage(
                model = school.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) failed = true
                },
            )
        }
    }
}

/** 院校标签（985 / 211 / 双一流 / C9），淡紫底、紫字。 */
@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ScoreTraceColors.SchoolPurple.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.SchoolPurple,
        )
    }
}
