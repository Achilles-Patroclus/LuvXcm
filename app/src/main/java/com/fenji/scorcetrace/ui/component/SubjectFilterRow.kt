package com.fenji.scorcetrace.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.pressScale
import com.fenji.scorcetrace.ui.theme.rememberPressSource

/**
 * 可横向滑动的科目筛选条，subjectId 为 null 表示「全部」。
 *
 * 用 [LazyRow] + 稳定 key 承载长科目列表；选中态走颜色过渡（220ms），
 * 未选中态用科目色细描边，触摸热区 ≥48dp。
 */
@Composable
fun SubjectFilterRow(
    subjects: List<Subject>,
    selectedSubjectId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = Dimens.PageHorizontal,
            vertical = 6.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            SubjectFilterChip(
                label = "全部",
                selected = selectedSubjectId == null,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onSelect(null) },
            )
        }
        items(items = subjects, key = { it.id }) { subject ->
            SubjectFilterChip(
                label = subject.name,
                selected = selectedSubjectId == subject.id,
                accent = Color(subject.color),
                onClick = { onSelect(subject.id) },
            )
        }
    }
}

/** 表单里的科目选择，必须选中一个具体科目。视觉与筛选条一致，只是不提供「全部」。 */
@Composable
fun SubjectPickerRow(
    subjects: List<Subject>,
    selectedSubjectId: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = subjects, key = { it.id }) { subject ->
            SubjectFilterChip(
                label = subject.name,
                selected = selectedSubjectId == subject.id,
                accent = Color(subject.color),
                onClick = { onSelect(subject.id) },
            )
        }
    }
}

/**
 * 名称字符串的**可选**选择条：用于新高考 3+1+2 的「首选 / 再选」这类可留空的标签。
 *
 * - [labels] 为候选名称（如 `Constants.PRIMARY_SUBJECT_NAMES`）。
 * - 首个 chip 是 [noneLabel]（默认「不填」），selected 为 null 时选中它；点已选中的 chip 也可取消（回到「不填」）。
 * - 视觉与 [SubjectFilterRow] 一致，触摸热区 ≥48dp，横向可滑动。
 */
@Composable
fun OptionalLabelPickerRow(
    labels: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    noneLabel: String = "不填",
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "__none__") {
            SubjectFilterChip(
                label = noneLabel,
                selected = selected == null,
                accent = accent,
                onClick = { onSelect(null) },
            )
        }
        items(items = labels, key = { it }) { label ->
            SubjectFilterChip(
                label = label,
                selected = selected == label,
                accent = accent,
                onClick = { onSelect(if (selected == label) null else label) },
            )
        }
    }
}

@Composable
private fun SubjectFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "chipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else accent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "chipContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else accent.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "chipBorder",
    )
    val interaction = rememberPressSource()

    Box(
        modifier = Modifier
            .pressScale(interactionSource = interaction)
            .clip(CircleShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = accent),
                onClick = onClick,
            )
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
        )
    }
}
