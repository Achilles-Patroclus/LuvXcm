package com.fenji.scorcetrace.ui.screen.score

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 单科明细 */
data class SubjectDetail(
    val name: String,
    val score: Int,
    val fullScore: Int,
    /** 得分率 */
    val rate: Float,
    /** 班均分 */
    val classAvg: Int,
    /** 超越班级百分比 */
    val surpassRate: Float,
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
    val examName: String = "高三10月月考",
    val examDate: String = "2026-10-01",
    /** 全级人数 */
    val totalStudents: Int = 620,
    val totalScore: Int = 562,
    val fullScore: Int = 750,
    val totalRate: Float = 0.749f,
    val classRank: Int = 15,
    val gradeRank: Int = 120,
    /** 较上次变化（正=上升，负=下降） */
    val deltaFromLast: Int = 12,
    /** 本班人数 */
    val classStudentCount: Int = 52,
    val subjects: List<SubjectDetail> = emptyList(),
    val segments: List<ScoreSegment> = emptyList(),
)

/**
 * 成绩详情页状态。
 *
 * TODO: 当前 [ScoreRecord] 只有单科成绩，无考试总分/排名/班均/超越百分比/分数段等字段，
 * 因此以下数据全部硬编码占位；后续新增 `ExamSummary` + `ExamSubjectDetail` 实体后按 [examId] 替换。
 */
@HiltViewModel
class ScoreDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    scoreRecordRepository: ScoreRecordRepository,
) : ViewModel() {

    /** 当前详情页对应的考试 ID；占位阶段数据全硬编码，暂未使用 */
    @Suppress("unused")
    private val examId: Long = savedStateHandle["examId"] ?: 0L

    private val hardcodedSubjects = listOf(
        SubjectDetail("语文", 118, 150, 0.79f, 105, 0.72f, Color(0xFF3B82F6)),
        SubjectDetail("数学", 96, 150, 0.64f, 88, 0.61f, Color(0xFF3B82F6)),
        SubjectDetail("英语", 126, 150, 0.84f, 112, 0.81f, Color(0xFF10B981)),
        SubjectDetail("物理", 82, 100, 0.82f, 71, 0.76f, Color(0xFF10B981)),
        SubjectDetail("化学", 52, 100, 0.52f, 63, 0.28f, Color(0xFFF59E0B)),
        SubjectDetail("生物", 88, 100, 0.88f, 74, 0.85f, Color(0xFF10B981)),
    )

    private val hardcodedSegments = listOf(
        ScoreSegment("<450", 2, false),
        ScoreSegment("450-500", 6, false),
        ScoreSegment("500-550", 14, false),
        ScoreSegment("550-600", 18, true),
        ScoreSegment("600-650", 9, false),
        ScoreSegment(">650", 3, false),
    )

    val uiState: StateFlow<ScoreDetailUiState> = combine(
        scoreRecordRepository.observeRecent(50),
    ) { _ ->
        // TODO: 按 examId 取该次考试的各科成绩（records 已按考试日期倒序）
        ScoreDetailUiState(
            isLoading = false,
            subjects = hardcodedSubjects,
            segments = hardcodedSegments,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScoreDetailUiState(),
    )

    /** TODO: 删除该次考试的所有记录（接入 ExamSummary 后实现） */
    fun deleteExam() = Unit
}
