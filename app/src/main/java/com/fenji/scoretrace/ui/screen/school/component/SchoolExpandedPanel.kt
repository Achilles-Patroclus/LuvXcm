package com.fenji.scoretrace.ui.screen.school.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.model.Major
import com.fenji.scoretrace.data.model.MajorTreeData
import com.fenji.scoretrace.data.model.SchoolInfo
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 选中院校后的展开面板：只读目标分数、目标专业（学科门类 → 专业类 → 专业 三级选择）、录取数据提示。
 * 只负责面板内容，卡片边框/背景由外层承担。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolExpandedPanel(
    school: SchoolInfo,
    targetScore: Int,
    selectedMajor: Major?,
    currentScore: Int,
    majorTree: MajorTreeData?,
    onMajorSelect: (Major) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── 目标分数（只读）
        Column {
            FieldLabel("目标分数")
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ScoreTraceColors.BrandPrimary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, ScoreTraceColors.BrandPrimary.copy(alpha = 0.2f)),
                ) {
                    Text(
                        text = if (targetScore > 0) targetScore.toString() else "暂无数据",
                        fontSize = if (targetScore > 0) 26.sp else 15.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreTraceColors.BrandPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "/ 750 分",
                    fontSize = 14.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
            }
        }

        // ── 目标专业（三级选择）
        Column {
            FieldLabel("目标专业")
            Spacer(modifier = Modifier.height(8.dp))
            if (majorTree == null) {
                Text(
                    text = "专业数据加载中…",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            } else {
                MajorSelector(
                    majorTree = majorTree,
                    selectedMajor = selectedMajor,
                    onMajorSelect = onMajorSelect,
                )
            }
        }

        // ── 录取数据提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ScoreTraceColors.AccentBlue.copy(alpha = 0.08f))
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildTip(school, targetScore, selectedMajor, currentScore),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF4B5563),
                )
            }
        }
    }
}

/**
 * 三级专业选择：学科门类 → 专业类 → 专业。
 * 前两级用横向滚动标签，第三级用 FlowRow 展示「6 位代码 + 名称」的专业。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MajorSelector(
    majorTree: MajorTreeData,
    selectedMajor: Major?,
    onMajorSelect: (Major) -> Unit,
) {
    // 选中专业存在时，打开面板即定位到它所属的门类/专业类
    val initialCategory = remember(selectedMajor) {
        majorTree.categories.firstOrNull { cat ->
            cat.subCategories.any { sub -> sub.majors.any { it.code == selectedMajor?.code } }
        }?.code
    }
    val initialSub = remember(selectedMajor) {
        majorTree.categories.flatMap { it.subCategories }
            .firstOrNull { sub -> sub.majors.any { it.code == selectedMajor?.code } }?.code
    }
    var categoryCode by remember(initialCategory) { mutableStateOf(initialCategory) }
    var subCategoryCode by remember(initialSub) { mutableStateOf(initialSub) }

    val category = majorTree.categories.firstOrNull { it.code == categoryCode }
    val subCategory = category?.subCategories?.firstOrNull { it.code == subCategoryCode }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 第一级：学科门类
        Text("学科门类", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextSecondaryLight)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            majorTree.categories.forEach { cat ->
                SelectableChip(
                    text = cat.name,
                    selected = cat.code == categoryCode,
                    onClick = {
                        categoryCode = cat.code
                        subCategoryCode = null
                    },
                )
            }
        }

        // 第二级：专业类
        if (category != null) {
            Text("专业类", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextSecondaryLight)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                category.subCategories.forEach { sub ->
                    SelectableChip(
                        text = sub.name,
                        selected = sub.code == subCategoryCode,
                        onClick = { subCategoryCode = sub.code },
                    )
                }
            }
        }

        // 第三级：专业（带代码）
        if (subCategory != null) {
            Text("专业", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextSecondaryLight)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subCategory.majors.forEach { major ->
                    MajorChip(
                        major = major,
                        selected = major.code == selectedMajor?.code,
                        onClick = { onMajorSelect(major) },
                    )
                }
            }
        } else {
            Text(
                text = "请先选择学科门类与专业类",
                fontSize = 13.sp,
                color = ScoreTraceColors.TextTertiaryLight,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = ScoreTraceColors.TextPrimaryLight,
    )
}

/** 门类 / 专业类选择标签：选中态蓝底白字，未选态浅底灰字。 */
@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) ScoreTraceColors.BrandPrimary else Color(0xFFF3F4F6))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (selected) Color.White else Color(0xFF4B5563),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 专业标签（单选）：选中态蓝底白字 + 对勾，并显示 6 位专业代码。 */
@Composable
private fun MajorChip(major: Major, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) ScoreTraceColors.BrandPrimary else Color(0xFFF3F4F6))
            .border(
                width = 1.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else Color(0xFFE5E7EB),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = major.code,
                fontSize = 10.sp,
                color = if (selected) Color.White.copy(alpha = 0.85f) else ScoreTraceColors.TextTertiaryLight,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = major.name,
                fontSize = 13.sp,
                color = if (selected) Color.White else Color(0xFF4B5563),
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

/** 提示文案：近三年最低分范围 + 当前分差（差距数字用橙色强调）。 */
private fun buildTip(
    school: SchoolInfo,
    targetScore: Int,
    selectedMajor: Major?,
    currentScore: Int,
): androidx.compose.ui.text.AnnotatedString {
    val majorLabel = selectedMajor?.display ?: "物理类"
    return buildAnnotatedString {
        if (school.minScore3Year <= 0) {
            append("${school.name}$majorLabel 暂无本省录取数据，可参考设定目标分数。")
            return@buildAnnotatedString
        }
        append("${school.name}$majorLabel 近三年在本省录取最低分约 ")
        append("${school.minScore3Year}-${school.maxScore3Year}，")
        if (currentScore > 0) {
            append("当前 $currentScore 分还差 ")
            withStyle(SpanStyle(color = ScoreTraceColors.WarningOrange, fontWeight = FontWeight.Bold)) {
                append("${(targetScore - currentScore).coerceAtLeast(0)}")
            }
            append(" 分。")
        } else {
            append("目标分数建议参考近三年最低分。")
        }
    }
}
