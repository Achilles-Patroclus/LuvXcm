package com.fenji.scoretrace.ui.screen.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.Constants

/**
 * 选科配置页（新高考 3+1+2）。
 *
 * 全屏页，[com.fenji.scoretrace.ui.navigation.AppNavHost] 在该路由隐藏底部导航栏；
 * 标题栏自绘（返回 + 标题 + 重置），底部保存按钮自行避让手势条，不嵌套 Scaffold。
 */
@Composable
fun SubjectConfigScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SubjectConfigViewModel = hiltViewModel(),
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubjectConfigTopBar(onBack = onBack, onReset = viewModel::reset)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InfoCard()
                RequiredSection()
                PrimarySection(selected = selection.primary, onSelect = viewModel::selectPrimary)
                SecondarySection(secondary = selection.secondary, onToggle = viewModel::toggleSecondary)
                PreviewSection(selection)
                Spacer(modifier = Modifier.height(4.dp))
            }

            SaveBar(
                enabled = selection.isValid,
                onSave = {
                    viewModel.save {
                        AppToast.success("选科配置已保存")
                        onSaved()
                    }
                },
            )
        }
    }
}

@Composable
private fun SubjectConfigTopBar(onBack: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "选科配置",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Text(
            text = "重置",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.BrandPrimary,
        )
    }
}

@Composable
private fun InfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.08f))
            .padding(14.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = ScoreTraceColors.BrandPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                append("当前采用")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("新高考「3+1+2」模式")
                }
                append("：语数外必考，物理 / 历史二选一，其余四科任选两科，总分 750。")
            },
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun RequiredSection() {
    Column {
        SectionTitle("必考科目（3门）")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Constants.REQUIRED_SUBJECT_NAMES.forEach { name ->
                RequiredSubjectCard(name = name, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RequiredSubjectCard(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
        Spacer(modifier = Modifier.height(2.dp))
        Text("150 分", fontSize = 12.sp, color = ScoreTraceColors.TextSecondaryLight)
    }
}

@Composable
private fun PrimarySection(selected: String?, onSelect: (String) -> Unit) {
    Column {
        SectionHeaderRow(title = "首选科目", hint = "二选一 · 原始分计入")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Constants.PRIMARY_SUBJECT_NAMES.forEach { name ->
                PrimaryCard(
                    name = name,
                    selected = name == selected,
                    onClick = { onSelect(name) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PrimaryCard(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    ScoreTraceColors.BrandPrimary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(
                width = 1.5.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.CardBorderLight,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(subjectIconOf(name)),
                    contentDescription = null,
                    tint = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextTertiaryLight,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subjectDescOf(name),
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                lineHeight = 15.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        RadioDot(selected = selected)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextTertiaryLight,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ScoreTraceColors.BrandPrimary),
            )
        }
    }
}

@Composable
private fun SecondarySection(secondary: List<String>, onToggle: (String) -> Unit) {
    val atCapacity = secondary.size >= SubjectSelection.SECONDARY_COUNT
    Column {
        SectionHeaderRow(title = "再选科目", hint = "四选二 · 等级赋分")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Constants.SECONDARY_SUBJECT_NAMES.chunked(2).forEach { rowNames ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowNames.forEach { name ->
                        val selected = name in secondary
                        SecondaryCard(
                            name = name,
                            selected = selected,
                            enabled = !atCapacity || selected,
                            onClick = { onToggle(name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowNames.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SecondaryCard(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    ScoreTraceColors.BrandPrimary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(
                width = 1.5.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.CardBorderLight,
                shape = RoundedCornerShape(14.dp),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(subjectIconOf(name)),
                    contentDescription = null,
                    tint = (if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextTertiaryLight)
                        .copy(alpha = alpha),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight.copy(alpha = alpha),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subjectDescOf(name),
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight.copy(alpha = alpha),
                lineHeight = 15.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        CheckDot(selected = selected, enabled = enabled)
    }
}

@Composable
private fun CheckDot(selected: Boolean, enabled: Boolean) {
    val ringColor = (if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextTertiaryLight)
        .copy(alpha = if (enabled) 1f else 0.4f)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) ScoreTraceColors.BrandPrimary else Color.Transparent)
            .border(width = 1.5.dp, color = ringColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = if (selected) Color.White else ringColor,
            modifier = Modifier.size(13.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewSection(selection: SubjectSelection) {
    val chips = previewChips(selection)
    val total = chips.sumOf { it.score }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("已选科目预览", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
            Spacer(modifier = Modifier.weight(1f))
            Text("共 ${chips.size} 门 · $total 分", fontSize = 12.sp, color = ScoreTraceColors.TextSecondaryLight)
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { chip -> SubjectPreviewChip(chip) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        CoverageHint(selection)
    }
}

@Composable
private fun SubjectPreviewChip(chip: PreviewChip) {
    val accent = when (chip.category) {
        ChipCategory.REQUIRED -> ScoreTraceColors.BrandPrimary
        ChipCategory.PRIMARY -> ScoreTraceColors.SchoolPurple
        ChipCategory.SECONDARY -> ScoreTraceColors.SuccessGreen
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "${chip.name} ${chip.score}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
        )
    }
}

@Composable
private fun CoverageHint(selection: SubjectSelection) {
    val boxModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(ScoreTraceColors.PageBackgroundLight)
        .padding(12.dp)

    val primary = selection.primary
    if (primary == null || selection.secondary.size < SubjectSelection.SECONDARY_COUNT) {
        Box(modifier = boxModifier) {
            Text("请选择 1 门首选 + 2 门再选科目", fontSize = 13.sp, color = ScoreTraceColors.TextSecondaryLight)
        }
        return
    }

    val coverage = coverageFor(primary, selection.secondary)
    Row(modifier = boxModifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = ScoreTraceColors.SuccessGreen,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "该组合可报专业覆盖率约 ${coverage.first}%，${coverage.second}。",
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
}

@Composable
private fun SectionHeaderRow(title: String, hint: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
        Spacer(modifier = Modifier.weight(1f))
        Text(hint, fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(
                        if (enabled) {
                            Brush.horizontalGradient(
                                listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFC3CBD8), Color(0xFFC3CBD8)))
                        },
                    )
                    .then(if (enabled) Modifier.clickable(onClick = onSave) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text("保存选科配置", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── 科目元数据 ──

private enum class ChipCategory { REQUIRED, PRIMARY, SECONDARY }

private data class PreviewChip(val name: String, val score: Int, val category: ChipCategory)

/** 语数外各 150，其余（首选 + 再选）各 100，与「750 分」一致。 */
private fun subjectScoreOf(name: String): Int =
    if (name in Constants.REQUIRED_SUBJECT_NAMES) 150 else 100

private fun previewChips(selection: SubjectSelection): List<PreviewChip> = buildList {
    Constants.REQUIRED_SUBJECT_NAMES.forEach { add(PreviewChip(it, subjectScoreOf(it), ChipCategory.REQUIRED)) }
    selection.primary?.let { add(PreviewChip(it, subjectScoreOf(it), ChipCategory.PRIMARY)) }
    Constants.SECONDARY_SUBJECT_NAMES
        .filter { it in selection.secondary }
        .forEach { add(PreviewChip(it, subjectScoreOf(it), ChipCategory.SECONDARY)) }
}

private fun subjectIconOf(name: String): Int = when (name) {
    "物理" -> R.drawable.ic_subject_physics
    "历史" -> R.drawable.ic_book
    "化学" -> R.drawable.ic_subject_chemistry
    "生物" -> R.drawable.ic_subject_biology
    "政治" -> R.drawable.ic_subject_politics
    "地理" -> R.drawable.ic_public
    else -> R.drawable.ic_subject
}

private fun subjectDescOf(name: String): String = when (name) {
    "物理" -> "理工农医多数专业必选"
    "历史" -> "人文社科方向首选"
    "化学" -> "医学 / 材料类要求"
    "生物" -> "医学 / 农学类要求"
    "政治" -> "公安 / 法学类加分"
    "地理" -> "文理兼修，赋分友好"
    else -> ""
}

/**
 * 3+1+2 各组合的专业覆盖率参考值与代表方向示例（约数，用于预览提示，非精确报考数据）。
 * key = "首选|再选（按 Constants.SECONDARY_SUBJECT_NAMES 顺序逗号连接）"。
 */
private val COVERAGE_REFERENCE: Map<String, Pair<Int, String>> = mapOf(
    "物理|化学,生物" to (96 to "含清华大学计算机类"),
    "物理|化学,政治" to (95 to "含中国人民大学法学类"),
    "物理|化学,地理" to (95 to "含同济大学建筑类"),
    "物理|生物,政治" to (92 to "含北京大学法学类"),
    "物理|生物,地理" to (92 to "含北京师范大学地理科学类"),
    "物理|政治,地理" to (88 to "含中国政法大学法学类"),
    "历史|化学,生物" to (87 to "含首都医科大学护理学类"),
    "历史|化学,政治" to (84 to "含华东政法大学法学类"),
    "历史|化学,地理" to (83 to "含南京大学历史学类"),
    "历史|生物,政治" to (82 to "含复旦大学新闻传播学类"),
    "历史|生物,地理" to (80 to "含中山大学旅游管理类"),
    "历史|政治,地理" to (78 to "含北京大学中国语言文学类"),
)

private fun coverageFor(primary: String, secondary: List<String>): Pair<Int, String> {
    val key = primary + "|" + Constants.SECONDARY_SUBJECT_NAMES.filter { it in secondary }.joinToString(",")
    return COVERAGE_REFERENCE[key] ?: (85 to "覆盖多数常见专业")
}
