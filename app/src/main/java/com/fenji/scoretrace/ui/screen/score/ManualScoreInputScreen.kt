package com.fenji.scoretrace.ui.screen.score

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.ui.component.score.SaveBar
import com.fenji.scoretrace.ui.component.score.ScoreEntryForm
import com.fenji.scoretrace.ui.component.score.ScoreInputTopBar
import com.fenji.scoretrace.ui.component.score.scoreFormHasOverflow
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.util.Date

/**
 * 手动录成绩页：纯手填，无 AI 识别区。
 *
 * 路由可携带 `examId` 进入编辑模式（预填旧值与排名）。表单字段与 AI 录成绩页共用
 * [ScoreEntryForm]；日期为可点选的中文日期，含班级/年级排名输入。
 */
@Composable
fun ManualScoreInputScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ManualScoreInputViewModel = hiltViewModel(),
) {
    val formSubjects by viewModel.formSubjects.collectAsStateWithLifecycle()
    val prefill by viewModel.prefill.collectAsStateWithLifecycle()

    var examName by rememberSaveable { mutableStateOf("") }
    var examDateMillis by rememberSaveable { mutableStateOf(Date().time) }
    var classRank by rememberSaveable { mutableStateOf("") }
    var gradeRank by rememberSaveable { mutableStateOf("") }
    val scoreTexts = remember { mutableStateMapOf<String, String>() }
    var prefillApplied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefill) {
        val loaded = prefill ?: return@LaunchedEffect
        if (prefillApplied) return@LaunchedEffect
        prefillApplied = true
        examName = loaded.examName
        examDateMillis = loaded.examDate.time
        classRank = loaded.classRank
        gradeRank = loaded.gradeRank
        loaded.scores.forEach { (name, text) -> scoreTexts[name] = text }
    }

    val texts = formSubjects.map { scoreTexts[it.name].orEmpty() }
    val hasOverflow = scoreFormHasOverflow(formSubjects, texts)
    val entries = formSubjects.mapIndexed { index, subject ->
        ScoreEntry(
            name = subject.name,
            score = texts[index].toDoubleOrNull() ?: 0.0,
            fullScore = subject.fullScore,
        )
    }
    val canSave = examName.isNotBlank() && !hasOverflow && entries.any { it.score > 0 }

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreInputTopBar(title = "手动录成绩", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ScoreEntryForm(
                    examName = examName,
                    onExamNameChange = { examName = it },
                    examDate = Date(examDateMillis),
                    onExamDateChange = { examDateMillis = it.time },
                    classRank = classRank,
                    onClassRankChange = { classRank = it },
                    gradeRank = gradeRank,
                    onGradeRankChange = { gradeRank = it },
                    subjects = formSubjects,
                    scoreTexts = texts,
                    onScoreChange = { index, value -> scoreTexts[formSubjects[index].name] = value },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            SaveBar(
                enabled = canSave,
                onSave = {
                    viewModel.save(
                        examName = examName.trim(),
                        examDate = Date(examDateMillis),
                        classRank = classRank.toIntOrNull(),
                        gradeRank = gradeRank.toIntOrNull(),
                        entries = entries.filter { it.score > 0 },
                        onSaved = onSaved,
                    )
                },
            )
        }
    }
}
