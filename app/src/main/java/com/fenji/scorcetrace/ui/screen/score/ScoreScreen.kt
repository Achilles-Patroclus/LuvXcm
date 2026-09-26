package com.fenji.scorcetrace.ui.screen.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.ui.component.EmptyView
import com.fenji.scorcetrace.ui.component.ListCard
import com.fenji.scorcetrace.ui.component.LoadingView
import com.fenji.scorcetrace.ui.component.OptionalLabelPickerRow
import com.fenji.scorcetrace.ui.component.ScreenHeader
import com.fenji.scorcetrace.ui.component.SubjectFilterRow
import com.fenji.scorcetrace.ui.component.SubjectPickerRow
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.util.Constants
import com.fenji.scorcetrace.util.DateUtils
import java.util.Date

@Composable
fun ScoreScreen(viewModel: ScoreViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjectNames = state.subjects.associate { it.id to it.name }
    val subjectColors = state.subjects.associate { it.id to Color(it.color) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "成绩记录",
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加成绩")
                }
            },
        )

        SubjectFilterRow(
            subjects = state.subjects,
            selectedSubjectId = state.selectedSubjectId,
            onSelect = viewModel::selectSubject,
        )

        when {
            state.isLoading -> LoadingView()

            state.records.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyView(
                    message = "暂无成绩记录",
                    subtitle = "记录每一次考试，才看得见进步的曲线",
                    actionLabel = "记录成绩",
                    onAction = { showAddDialog = true },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.PageHorizontal,
                    end = Dimens.PageHorizontal,
                    top = 4.dp,
                    bottom = Dimens.ContentBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                items(items = state.records, key = { it.id }) { record ->
                    ScoreCard(
                        record = record,
                        subjectName = subjectNames[record.subjectId].orEmpty(),
                        accent = subjectColors[record.subjectId] ?: Color.Unspecified,
                        onDelete = { viewModel.deleteRecord(record) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScoreDialog(
            subjects = state.subjects,
            defaultSubjectId = state.selectedSubjectId,
            onDismiss = { showAddDialog = false },
            onConfirm = { subjectId, score, fullScore, examName, examDate, primarySubject, secondarySubject ->
                viewModel.addRecord(
                    subjectId,
                    score,
                    fullScore,
                    examName,
                    examDate,
                    primarySubject,
                    secondarySubject,
                )
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ScoreCard(
    record: ScoreRecord,
    subjectName: String,
    accent: Color,
    onDelete: () -> Unit,
) {
    val date = DateUtils.formatDate(record.examDate)
    ListCard(
        title = record.examName,
        subtitle = buildScoreSubtitle(subjectName, date, record.primarySubject, record.secondarySubject),
        accent = accent,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatScore(record.score)} / ${formatScore(record.fullScore)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun AddScoreDialog(
    subjects: List<Subject>,
    defaultSubjectId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String?,
        secondarySubject: String?,
    ) -> Unit,
) {
    var examName by rememberSaveable { mutableStateOf("") }
    var scoreText by rememberSaveable { mutableStateOf("") }
    var fullScoreText by rememberSaveable { mutableStateOf(Constants.DEFAULT_FULL_SCORE.toInt().toString()) }
    var examDateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(Date())) }
    var subjectId by rememberSaveable {
        mutableStateOf(defaultSubjectId ?: subjects.firstOrNull()?.id ?: 0L)
    }

    var primarySubject by rememberSaveable { mutableStateOf("") }
    var secondarySubject by rememberSaveable { mutableStateOf("") }

    val score = scoreText.toDoubleOrNull()
    val fullScore = fullScoreText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加成绩") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text("考试名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("得分") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fullScoreText,
                    onValueChange = { fullScoreText = it },
                    label = { Text("满分") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = examDateText,
                    onValueChange = { examDateText = it },
                    label = { Text("考试日期（yyyy-MM-dd）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "科目", style = MaterialTheme.typography.labelLarge)
                SubjectPickerRow(
                    subjects = subjects,
                    selectedSubjectId = subjectId,
                    onSelect = { subjectId = it },
                )
                Text(
                    text = "首选科目（物理/历史）",
                    style = MaterialTheme.typography.labelLarge,
                )
                OptionalLabelPickerRow(
                    labels = Constants.PRIMARY_SUBJECT_NAMES,
                    selected = primarySubject.ifBlank { null },
                    onSelect = { primarySubject = it.orEmpty() },
                    accent = ScoreTraceColors.BrandBlue,
                )
                Text(
                    text = "再选科目（化学/生物/政治/地理）",
                    style = MaterialTheme.typography.labelLarge,
                )
                OptionalLabelPickerRow(
                    labels = Constants.SECONDARY_SUBJECT_NAMES,
                    selected = secondarySubject.ifBlank { null },
                    onSelect = { secondarySubject = it.orEmpty() },
                    accent = ScoreTraceColors.BrandBlue,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = examName.isNotBlank() &&
                    score != null &&
                    fullScore != null &&
                    score > 0 &&
                    fullScore > 0 &&
                    subjectId != 0L,
                onClick = {
                    onConfirm(
                        subjectId,
                        score ?: 0.0,
                        fullScore ?: Constants.DEFAULT_FULL_SCORE,
                        examName.trim(),
                        DateUtils.parseDate(examDateText) ?: Date(),
                        primarySubject.ifBlank { null },
                        secondarySubject.ifBlank { null },
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 去掉 Double 的冗余 ".0"，让「120.0 / 150.0」显示为「120 / 150」。纯展示层处理。 */
private fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/**
 * 拼列表副标题：科目名 · 首选 · 再选 · 日期，空值不参与拼接，因此不会出现多余分隔符。
 * 例：「数学 · 物理 · 首选 · 化学 · 再选 · 2026-09-26」。
 */
private fun buildScoreSubtitle(
    subjectName: String,
    date: String,
    primarySubject: String?,
    secondarySubject: String?,
): String = buildList {
    if (subjectName.isNotEmpty()) add(subjectName)
    primarySubject?.takeIf { it.isNotBlank() }?.let { add("$it · 首选") }
    secondarySubject?.takeIf { it.isNotBlank() }?.let { add("$it · 再选") }
    add(date)
}.joinToString(" · ")
