package com.fenji.scoretrace.ui.screen.score

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.ui.component.score.ScoreFormSubject
import com.fenji.scoretrace.ui.component.score.scoreFormSubjects
import com.fenji.scoretrace.util.aggregateExams
import com.fenji.scoretrace.util.examNameToId
import com.fenji.scoretrace.util.rankRecordFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** 编辑已有成绩时，用于预填表单的初始值。 */
data class ManualFormPrefill(
    val examName: String,
    val examDate: Date,
    val classRank: String,
    val gradeRank: String,
    /** 科目名 → 分数文本（如 "84"）。 */
    val scores: Map<String, String>,
)

/**
 * 手动录成绩页状态。
 *
 * 路由参数 [examId] 大于 0 时进入「编辑」模式：读取该考试的旧值与排名预填表单；
 * 否则为新建。保存时先删除同名旧记录再写入，避免重录产生重复行。
 */
@HiltViewModel
class ManualScoreInputViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userPreferences: UserPreferences,
    private val subjectRepository: SubjectRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val examId: Long = savedStateHandle["examId"] ?: -1L

    /** 表单科目：语数英 + 用户选考三科 */
    val formSubjects: StateFlow<List<ScoreFormSubject>> = userPreferences.selectedSubjects
        .map { scoreFormSubjects(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = scoreFormSubjects(emptyList()),
        )

    private val _prefill = MutableStateFlow<ManualFormPrefill?>(null)
    val prefill: StateFlow<ManualFormPrefill?> = _prefill.asStateFlow()

    init {
        if (examId > 0L) {
            viewModelScope.launch { loadPrefill() }
        }
    }

    private suspend fun loadPrefill() {
        val records = scoreRecordRepository.observeRecords(null).first()
        val target = aggregateExams(records).firstOrNull { examNameToId(it.name) == examId } ?: return
        val nameById = subjectRepository.observeSubjects().first().associate { it.id to it.name }
        val rank = rankRecordFor(examRecordRepository.observeAll().first(), target.name)
        _prefill.value = ManualFormPrefill(
            examName = target.name,
            examDate = target.date,
            classRank = rank?.classRank?.toString().orEmpty(),
            gradeRank = rank?.gradeRank?.toString().orEmpty(),
            scores = target.records.mapNotNull { record ->
                nameById[record.subjectId]?.let { it to formatScore(record.score) }
            }.toMap(),
        )
    }

    /** 保存一次考试（覆盖同名旧记录）。只写有分的科目。 */
    fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
        entries: List<ScoreEntry>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            // 覆盖式写入：先清同名旧记录，避免重录/编辑产生重复科目行
            scoreRecordRepository.deleteExamByName(examName)
            val idByName = subjectRepository.observeSubjects().first().associate { it.name to it.id }
            var total = 0.0
            entries.forEach { entry ->
                val subjectId = idByName[entry.name] ?: return@forEach
                if (entry.score > 0) {
                    scoreRecordRepository.addRecord(
                        subjectId = subjectId,
                        score = entry.score,
                        fullScore = entry.fullScore,
                        examName = examName,
                        examDate = examDate,
                    )
                    total += entry.score
                }
            }
            examRecordRepository.save(
                examName = examName,
                examDate = examDate,
                classRank = classRank,
                gradeRank = gradeRank,
            )
            notificationRepository.add(
                type = NotificationType.SCORE,
                title = "新成绩已录入",
                content = "$examName · 总分 ${total.toInt()}",
            )
            onSaved()
        }
    }

    private companion object {
        fun formatScore(score: Double): String =
            if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
    }
}
