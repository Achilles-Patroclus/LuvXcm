package com.fenji.scoretrace.ui.screen.score

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.Constants
import com.fenji.scoretrace.util.DateUtils
import com.fenji.scoretrace.util.aggregateExams
import com.fenji.scoretrace.util.examNameToId
import com.fenji.scoretrace.util.rankRecordFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 单科明细 */
data class SubjectDetail(
    val name: String,
    val score: Int,
    val fullScore: Int,
    /** 得分率 */
    val rate: Float,
    /** 班均分；暂无数据源，为 null 时 UI 显示占位 */
    val classAvg: Int?,
    /** 超越班级百分比；暂无数据源，为 null 时 UI 显示占位 */
    val surpassRate: Float?,
    val color: Color,
)

/** 分数段 */
data class ScoreSegment(
    /** "<450" / "450-500" … */
    val label: String,
    val count: Int,
    /** 是否是「我」所在段 */
    val isMine: Boolean,
)

data class ScoreDetailUiState(
    val isLoading: Boolean = true,
    /** 是否找到对应考试；false 表示该考试已不存在（被删除等） */
    val found: Boolean = false,
    val examName: String = "",
    val examDate: String = "",
    /** 全级人数；暂无数据源 */
    val totalStudents: Int? = null,
    val totalScore: Int = 0,
    val fullScore: Int = 0,
    val totalRate: Float = 0f,
    /** 班级排名；未填写时为 null */
    val classRank: Int? = null,
    /** 年级排名；未填写时为 null */
    val gradeRank: Int? = null,
    /** 较上次变化（正=上升，负=下降）；无上一次考试时为 null */
    val deltaFromLast: Int? = null,
    /** 本班人数；暂无数据源 */
    val classStudentCount: Int? = null,
    val subjects: List<SubjectDetail> = emptyList(),
    /** 本班分数段分布；暂无数据源，恒为空 */
    val segments: List<ScoreSegment> = emptyList(),
)

/**
 * 成绩详情页状态。
 *
 * 当前 [com.fenji.scoretrace.data.local.entity.ScoreRecord] 只有单科成绩，无班均/超越百分比/分数段字段，
 * 这些数据块显示「数据不足」占位，不编造数值；其余（考试名/日期/总分/排名/较上次）全部来自真实数据。
 */
@HiltViewModel
class ScoreDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val subjectRepository: SubjectRepository,
    private val examRecordRepository: ExamRecordRepository,
) : ViewModel() {

    /** 路由传入的考试 id（由 examName 派生），据此反查 examName */
    private val examId: Long = savedStateHandle["examId"] ?: 0L

    val uiState: StateFlow<ScoreDetailUiState> = combine(
        scoreRecordRepository.observeRecords(null),
        subjectRepository.observeSubjects(),
        examRecordRepository.observeAll(),
    ) { records, subjects, exams ->
        val aggregated = aggregateExams(records)
        val targetIndex = aggregated.indexOfFirst { examNameToId(it.name) == examId }
        if (targetIndex < 0) return@combine ScoreDetailUiState(isLoading = false)

        val target = aggregated[targetIndex]
        val previous = aggregated.getOrNull(targetIndex + 1)
        val rank = rankRecordFor(exams, target.name)
        val subjectById = subjects.associateBy { it.id }
        val nameToColor = subjects.associate { it.name to Color(it.color) }

        val details = target.records
            .sortedBy { Constants.DEFAULT_SUBJECT_NAMES.indexOf(subjectById[it.subjectId]?.name) }
            .map { record ->
                val name = subjectById[record.subjectId]?.name.orEmpty()
                SubjectDetail(
                    name = name,
                    score = record.score.toInt(),
                    fullScore = record.fullScore.toInt(),
                    rate = if (record.fullScore > 0) {
                        (record.score / record.fullScore).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    classAvg = null,
                    surpassRate = null,
                    color = nameToColor[name] ?: ScoreTraceColors.BrandPrimary,
                )
            }

        ScoreDetailUiState(
            isLoading = false,
            found = true,
            examName = target.name,
            examDate = DateUtils.formatDate(target.date),
            totalStudents = null,
            totalScore = target.totalScore,
            fullScore = target.fullScore,
            totalRate = if (target.fullScore > 0) {
                target.totalScore.toFloat() / target.fullScore
            } else {
                0f
            },
            classRank = rank?.classRank,
            gradeRank = rank?.gradeRank,
            deltaFromLast = previous?.let { target.totalScore - it.totalScore },
            classStudentCount = null,
            subjects = details,
            segments = emptyList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScoreDetailUiState(),
    )

    /** 删除该次考试的全部单科记录（排名行无删除接口，保留不影响展示）。 */
    fun deleteExam() {
        viewModelScope.launch {
            val target = aggregateExams(scoreRecordRepository.observeRecords(null).first())
                .firstOrNull { examNameToId(it.name) == examId } ?: return@launch
            target.records.forEach { scoreRecordRepository.deleteRecord(it) }
        }
    }
}
