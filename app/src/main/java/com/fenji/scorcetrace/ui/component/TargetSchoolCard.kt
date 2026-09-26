package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fenji.scorcetrace.data.local.entity.TargetSchool
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.ui.theme.pressScale
import com.fenji.scorcetrace.ui.theme.rememberPressSource
import java.util.Calendar

/**
 * 首页「目标院校及专业」卡片。
 *
 * - [targetSchool] 为 null 时渲染引导态「点击设定你的目标院校」，避免首页出现空白占位；
 * - 点击 → 打开编辑面板（预填当前值）；长按 → 触发删除确认；
 * - 当前分 ≥ 目标分用成功色，低于目标分用错误色；
 * - 采用暖橙次级强调（[ScoreTraceColors.AccentAmber]）与倒计时卡的冷青拉开层次，
 *   圆角 / 描边沿用项目既有语汇（[Dimens.CardCorner] + outlineVariant）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TargetSchoolCard(
    targetSchool: TargetSchool?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = rememberPressSource()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .pressScale(interactionSource = interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClickLabel = if (targetSchool == null) "设定目标院校" else "编辑目标院校",
                onLongClickLabel = "删除目标院校",
                onLongClick = onLongClick.takeIf { targetSchool != null },
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Dimens.CardCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            ScoreTraceColors.AccentAmber.copy(alpha = 0.35f),
        ),
        shadowElevation = 2.dp,
    ) {
        if (targetSchool == null) {
            TargetSchoolEmptyContent()
        } else {
            TargetSchoolContent(targetSchool)
        }
    }
}

/** 引导态：淡橙圆角图标底 + 引导文案。 */
@Composable
private fun TargetSchoolEmptyContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.IconBox)
                .clip(RoundedCornerShape(Dimens.SmallCorner))
                .background(ScoreTraceColors.iconContainer(ScoreTraceColors.AccentAmberDeep)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = ScoreTraceColors.AccentAmberDeep,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "点击设定你的目标院校",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "写下目标分数，每天看得见差距",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 已设定态：左侧院校 / 专业 / 年份，右侧「当前分 / 目标分」与差距提示。 */
@Composable
private fun TargetSchoolContent(targetSchool: TargetSchool) {
    val reached = targetSchool.currentScore >= targetSchool.targetScore
    val scoreColor = when {
        reached && isSystemInDarkTheme() -> ScoreTraceColors.SuccessDark
        reached -> ScoreTraceColors.SuccessLight
        else -> MaterialTheme.colorScheme.error
    }
    val gapText = if (reached) {
        "已达标"
    } else {
        "还差 ${targetSchool.targetScore - targetSchool.currentScore} 分"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 暖橙强调条：与倒计时卡的冷青形成区分
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            ScoreTraceColors.AccentAmber,
                            ScoreTraceColors.AccentAmber.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = targetSchool.schoolName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = targetSchool.majorName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${targetSchool.year} 年高考",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "当前 / 目标",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = targetSchool.currentScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    maxLines = 1,
                )
                Text(
                    text = " / ${targetSchool.targetScore}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = gapText,
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * 目标院校编辑面板（[ModalBottomSheet]）。
 *
 * [initial] 为 null 表示新增；非 null 时预填其字段用于编辑（保存时由调用方带上原 id 覆盖）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSchoolEditorSheet(
    initial: TargetSchool?,
    onDismiss: () -> Unit,
    onSave: (
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = rememberCurrentYear()

    var schoolName by rememberSaveable { mutableStateOf(initial?.schoolName.orEmpty()) }
    var majorName by rememberSaveable { mutableStateOf(initial?.majorName.orEmpty()) }
    var targetScoreText by rememberSaveable {
        mutableStateOf(initial?.targetScore?.toString().orEmpty())
    }
    var currentScoreText by rememberSaveable {
        mutableStateOf(initial?.currentScore?.toString().orEmpty())
    }
    var yearText by rememberSaveable {
        mutableStateOf((initial?.year ?: currentYear).toString())
    }

    val targetScore = targetScoreText.toIntOrNull()
    val currentScore = currentScoreText.toIntOrNull()
    val year = yearText.toIntOrNull()
    val canSave = schoolName.isNotBlank() &&
        targetScore != null && targetScore > 0 &&
        currentScore != null && currentScore >= 0 &&
        year != null && year in MIN_YEAR..MAX_YEAR

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Dimens.CardCorner, topEnd = Dimens.CardCorner),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.PageHorizontal)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        ) {
            Text(
                text = if (initial == null) "设定目标院校" else "编辑目标院校",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("院校名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = majorName,
                onValueChange = { majorName = it },
                label = { Text("专业名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                OutlinedTextField(
                    value = targetScoreText,
                    onValueChange = { targetScoreText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("目标分数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = currentScoreText,
                    onValueChange = { currentScoreText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("当前分数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = yearText,
                onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("目标年份") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.TightGap),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(modifier = Modifier.width(Dimens.TightGap))
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            schoolName.trim(),
                            majorName.trim(),
                            targetScore ?: 0,
                            currentScore ?: 0,
                            year ?: currentYear,
                        )
                    },
                ) {
                    Text("保存")
                }
            }
        }
    }
}

/** 年份默认值取系统当前年，避免硬编码。 */
@Composable
private fun rememberCurrentYear(): Int = remember {
    Calendar.getInstance().get(Calendar.YEAR)
}

private const val MIN_YEAR = 2000
private const val MAX_YEAR = 2100
