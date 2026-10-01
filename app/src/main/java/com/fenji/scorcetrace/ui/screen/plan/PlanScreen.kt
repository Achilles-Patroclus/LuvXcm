package com.fenji.scorcetrace.ui.screen.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.data.local.entity.StudyTask
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.ui.component.EmptyView
import com.fenji.scorcetrace.ui.component.ListCard
import com.fenji.scorcetrace.ui.component.LoadingView
import com.fenji.scorcetrace.ui.component.ScreenHeader
import com.fenji.scorcetrace.ui.component.SubjectFilterRow
import com.fenji.scorcetrace.ui.component.SubjectPickerRow
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.util.DateUtils
import java.util.Date

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    bottomContentPadding: Dp = Dimens.ContentBottom,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjectNames = state.subjects.associate { it.id to it.name }
    val subjectColors = state.subjects.associate { it.id to Color(it.color) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "学习计划",
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加任务")
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

            state.tasks.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyView(
                    message = "暂无学习任务",
                    subtitle = "把要做的事记下来，进度才看得见",
                    actionLabel = "添加学习任务",
                    onAction = { showAddDialog = true },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.PageHorizontal,
                    end = Dimens.PageHorizontal,
                    top = 4.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                items(items = state.tasks, key = { it.id }) { task ->
                    StudyTaskCard(
                        task = task,
                        subjectName = subjectNames[task.subjectId].orEmpty(),
                        accent = subjectColors[task.subjectId] ?: Color.Unspecified,
                        onToggle = { viewModel.toggleTask(task) },
                        onDelete = { viewModel.deleteTask(task) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddStudyTaskDialog(
            subjects = state.subjects,
            defaultSubjectId = state.selectedSubjectId,
            onDismiss = { showAddDialog = false },
            onConfirm = { subjectId, title, content, dueDate ->
                viewModel.addTask(subjectId, title, content, dueDate)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun StudyTaskCard(
    task: StudyTask,
    subjectName: String,
    accent: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val due = task.dueDate?.let { "截止 " + DateUtils.formatDate(it) }.orEmpty()
    ListCard(
        title = task.title,
        subtitle = listOf(subjectName, due).filter { it.isNotEmpty() }.joinToString(" · ")
            .ifEmpty { null },
        accent = accent,
        titleDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
        leading = {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = ScoreTraceColors.AccentCyan),
            )
        },
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            if (task.content.isNotEmpty()) {
                Text(
                    text = task.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun AddStudyTaskDialog(
    subjects: List<Subject>,
    defaultSubjectId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, title: String, content: String, dueDate: Date?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var dueDateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(Date())) }
    var subjectId by rememberSaveable {
        mutableStateOf(defaultSubjectId ?: subjects.firstOrNull()?.id ?: 0L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加学习任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text("截止日期（yyyy-MM-dd）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "科目", style = MaterialTheme.typography.labelLarge)
                SubjectPickerRow(
                    subjects = subjects,
                    selectedSubjectId = subjectId,
                    onSelect = { subjectId = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && subjectId != 0L,
                onClick = {
                    onConfirm(subjectId, title.trim(), content.trim(), DateUtils.parseDate(dueDateText))
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
