package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.data.local.entity.TargetSchool
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import java.util.Locale

/**
 * 目标院校卡（设计稿版）。
 * 顶部：紫色渐变校徽方块 + 学校名 + 985 标签 + 专业 + 右箭头
 * 中部：三列分数（目标分/当前分/还差）
 * 底部：达成率进度条 + 百分比
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TargetSchoolCardNew(
    targetSchool: TargetSchool?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClickLabel = if (targetSchool == null) "设定目标院校" else "编辑目标院校",
                onLongClickLabel = "删除目标院校",
                onLongClick = onLongClick.takeIf { targetSchool != null },
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        if (targetSchool == null) {
            TargetSchoolEmptyContent()
        } else {
            TargetSchoolContent(targetSchool)
        }
    }
}

@Composable
private fun TargetSchoolEmptyContent() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ScoreTraceColors.SchoolPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.SchoolPurple,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "设定目标院校",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Text(
                text = "点击设定你的目标大学和专业",
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
    }
}

@Composable
private fun TargetSchoolContent(targetSchool: TargetSchool) {
    // 顶部行：校徽 + 学校信息 + 箭头
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(ScoreTraceColors.SchoolPurple, ScoreTraceColors.SchoolPurpleLight),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = targetSchool.schoolName.take(1),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = targetSchool.schoolName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.width(6.dp))
                // TODO: 985/211 标签需从 TargetSchool.tag 字段获取，当前硬编码（避免数据库迁移）
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ScoreTraceColors.SchoolPurple.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "985",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScoreTraceColors.SchoolPurple,
                    )
                }
            }
            Text(
                text = targetSchool.majorName,
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "编辑",
            tint = ScoreTraceColors.TextTertiaryLight,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 三列分数
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ScoreColumn(
            label = "目标分",
            value = targetSchool.targetScore.toString(),
            color = ScoreTraceColors.BrandPrimary,
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(ScoreTraceColors.CardBorderLight),
        )
        ScoreColumn(
            label = "当前分",
            value = targetSchool.currentScore.toString(),
            color = ScoreTraceColors.WarningOrange,
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(ScoreTraceColors.CardBorderLight),
        )
        val diff = (targetSchool.targetScore - targetSchool.currentScore).coerceAtLeast(0)
        ScoreColumn(
            label = "还差",
            value = "$diff 分",
            color = ScoreTraceColors.ErrorRed,
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 达成率
    val rate = if (targetSchool.targetScore > 0) {
        (targetSchool.currentScore.toFloat() / targetSchool.targetScore * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("达成率", fontSize = 13.sp, color = ScoreTraceColors.TextSecondaryLight)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = String.format(Locale.getDefault(), "%.1f%%", rate),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.BrandPrimary,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ScoreTraceColors.CardBorderLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((rate / 100f).coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.BrandPrimaryLight),
                    ),
                ),
        )
    }
}

@Composable
private fun ScoreColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = ScoreTraceColors.TextSecondaryLight)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
