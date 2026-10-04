package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.data.local.entity.TargetSchool
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import java.util.Locale

/**
 * 目标院校卡（设计稿版）。
 * 顶部：紫色渐变校徽方块 + 学校名 + 985 标签 + 专业 + 右箭头
 * 中部：三列分数（目标分/当前分/还差）
 * 底部：达成率进度条 + 百分比
 *
 * 各文本显式指定 lineHeight：真机系统字体是手写体、行高天然偏大，
 * 不指定会把卡片撑高。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TargetSchoolCardNew(
    targetSchool: TargetSchool?,
    logoUrl: String?,
    /** 当前分：取最近一次考试总分，随新成绩导入实时更新；无成绩时回退到录入时的快照 */
    currentScore: Int,
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (targetSchool == null) {
            TargetSchoolEmptyContent()
        } else {
            TargetSchoolContent(targetSchool, logoUrl, currentScore)
        }
    }
}

@Composable
private fun TargetSchoolEmptyContent() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(ScoreTraceColors.SchoolPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.SchoolPurple,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "设定目标院校",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Text(
                text = "点击设定你的目标大学和专业",
                fontSize = 15.sp,
                lineHeight = 18.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
    }
}

@Composable
private fun TargetSchoolContent(targetSchool: TargetSchool, logoUrl: String?, currentScore: Int) {
    // 顶部行：校徽 + 学校信息 + 箭头
    Row(verticalAlignment = Alignment.CenterVertically) {
        SchoolLogoBlock(schoolName = targetSchool.schoolName, logoUrl = logoUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = targetSchool.schoolName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.width(5.dp))
                // TODO: 985/211 标签需从 TargetSchool.tag 字段获取，当前硬编码（避免数据库迁移）
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(ScoreTraceColors.SchoolPurple.copy(alpha = 0.12f))
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "985",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScoreTraceColors.SchoolPurple,
                    )
                }
            }
            Text(
                text = targetSchool.majorName,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "编辑",
            tint = ScoreTraceColors.TextTertiaryLight,
            modifier = Modifier.size(20.dp),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 三列分数：等宽均分，数字大小统一
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreColumn(
            label = "目标分",
            value = targetSchool.targetScore.toString(),
            color = ScoreTraceColors.BrandPrimary,
            modifier = Modifier.weight(1f),
        )
        Divider()
        ScoreColumn(
            label = "当前分",
            value = currentScore.toString(),
            color = ScoreTraceColors.WarningOrange,
            modifier = Modifier.weight(1f),
        )
        Divider()
        // 三态：还差 / 刚好达标 / 已超
        val diff = targetSchool.targetScore - currentScore
        val (diffLabel, diffValue, diffColor) = when {
            diff > 0 -> Triple("还差", "$diff 分", ScoreTraceColors.ErrorRed)
            diff == 0 -> Triple("达成", "刚好达标", ScoreTraceColors.SuccessGreen)
            else -> Triple("已超", "${-diff} 分", ScoreTraceColors.SuccessGreen)
        }
        ScoreColumn(
            label = diffLabel,
            value = diffValue,
            color = diffColor,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 达成率（当前分超过目标分时封顶 100%）
    val rate = if (targetSchool.targetScore > 0) {
        (currentScore.toFloat() / targetSchool.targetScore * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "达成率",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = String.format(Locale.getDefault(), "%.1f%%", rate),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.BrandPrimary,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ScoreTraceColors.CardBorderLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((rate / 100f).coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.BrandPrimaryLight),
                    ),
                ),
        )
    }
}

/** 三列分数之间的竖直分隔线 */
@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}

/** 校徽：在线加载，失败（无网络 / 无图）时回退为院校首字渐变块。 */
@Composable
private fun SchoolLogoBlock(schoolName: String, logoUrl: String?) {
    var failed by remember(schoolName) { mutableStateOf(false) }

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
        if (logoUrl.isNullOrBlank() || failed) {
            Text(
                text = schoolName.take(1),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        } else {
            AsyncImage(
                model = logoUrl,
                contentDescription = schoolName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state -> if (state is AsyncImagePainter.State.Error) failed = true },
            )
        }
    }
}

@Composable
private fun ScoreColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}
