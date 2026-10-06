package com.fenji.scoretrace.ui.screen.score

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.entity.ExamRecord
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import com.fenji.scoretrace.ui.component.SubjectScore
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.Constants
import com.fenji.scoretrace.util.DateUtils
import com.fenji.scoretrace.util.aggregateExams
import com.fenji.scoretrace.util.examNameToId
import com.fenji.scoretrace.util.rankRecordFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/** 总分趋势时间范围 */
enum class TrendRange(val label: String) {
    Recent6("近6次"),
    Recent10("近10次"),
    All("全部"),
}

/** 各科分析 Tab */
enum class AnalysisTab(val label: String) {
    Radar("雷达图"),
    Trend("单科趋势"),
    StrongWeak("强弱科"),
}

/** 一次考试的总分趋势数据点 */
data class TrendPoint(
    val monthLabel: String,
    val score: Int,
)

/** 单科趋势：某科目历次成绩（时间正序）及满分/最新值。 */
data class SubjectTrend(
    val points: List<TrendPoint> = emptyList(),
    val fullScore: Int = 0,
    val latest: Int? = null,
)

/** 一次考试的历史记录（按 examName 聚合）。 */
data class ExamHistoryItem(
    /** 由 examName 派生的稳定 id，供导航到详情页使用 */
    val id: Long,
    val examName: String,
    val date: LocalDate,
    val totalScore: Int,
    val fullScore: Int,
    /** 较上次变化（正=上升，负=下降）；无上一次考试时为 null */
    val deltaFromLast: Int?,
    /** 班级排名；未填写时为 null */
    val classRank: Int?,
    /** 年级排名；未填写时为 null */
    val gradeRank: Int?,
)

/** 历史记录分组（本月 / 上月 / 更早） */
data class HistoryGroup(
    val title: String,
    val items: List<ExamHistoryItem>,
)

data class ScorePageUiState(
    val isLoading: Boolean = true,
    /** 是否已录入任何成绩；false 时页面展示空态 */
    val hasScores: Boolean = false,
    val latestExamName: String = "",
    val latestExamDate: String = "",
    val latestTotalScore: Int = 0,
    val latestFullScore: Int = 0,
    /** 较上一次考试的总分差；无上一次时为 null，UI 隐藏变化标签 */
    val deltaFromLast: Int? = null,
    val classRank: Int? = null,
    val gradeRank: Int? = null,
    val totalRate: Float = 0f,
    /** 目标分：与首页同源（目标院校目标分）；null 表示未设定 */
    val targetScore: Int? = null,
    val trendRange: TrendRange = TrendRange.Recent6,
    val trendPoints: List<TrendPoint> = emptyList(),
    val analysisTab: AnalysisTab = AnalysisTab.Radar,
    val selectedSubject: String = "数学",
    val subjectRates: List<SubjectScore> = emptyList(),
    val subjectTrend: SubjectTrend = SubjectTrend(),
    val historyGroups: List<HistoryGroup> = emptyList(),
)

/** 各科分析的三个可变控制项合并成一个流，便于与其它数据流一起 combine */
private data class AnalysisControls(
    val range: TrendRange,
    val tab: AnalysisTab,
    val subject: String,
)

/** 成绩数据源（不含目标院校）：与目标院校流一起 combine，避免超过 5 元组合上限 */
private data class CoreInputs(
    val records: List<ScoreRecord>,
    val subjects: List<Subject>,
    val exams: List<ExamRecord>,
    val selectedSubjects: List<String>,
    val controls: AnalysisControls,
)

@HiltViewModel
class ScoreViewModelNew @Inject constructor(
    private val scoreRecordRepository: ScoreRecordRepository,
    private val subjectRepository: SubjectRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _trendRange = MutableStateFlow(TrendRange.Recent6)
    private val _analysisTab = MutableStateFlow(AnalysisTab.Radar)
    private val _selectedSubject = MutableStateFlow("数学")

    private val _controls = combine(_trendRange, _analysisTab, _selectedSubject) { range, tab, subject ->
        AnalysisControls(range, tab, subject)
    }

    val uiState: StateFlow<ScorePageUiState> = combine(
        targetSchoolRepository.observeLatest(),
        combine(
            scoreRecordRepository.observeRecords(null),
            subjectRepository.observeSubjects(),
            examRecordRepository.observeAll(),
            userPreferences.selectedSubjects,
            _controls,
        ) { records, subjects, exams, selectedSubjects, controls ->
            CoreInputs(records, subjects, exams, selectedSubjects, controls)
        },
    ) { targetSchool, core ->
        val records = core.records
        val subjects = core.subjects
        val exams = core.exams
        val selectedSubjects = core.selectedSubjects
        val controls = core.controls
        val aggregated = aggregateExams(records)
        val idByName = subjects.associate { it.name to it.id }
        val nameToColor = subjects.associate { it.name to Color(it.color) }

        // 各科得分率：取该科最近一次成绩（records 未保证顺序，显式取日期最大者）
        val subjectRates = (Constants.REQUIRED_SUBJECT_NAMES + selectedSubjects).map { name ->
            val subjectId = idByName[name]
            val record = records.filter { it.subjectId == subjectId }.maxByOrNull { it.examDate }
            val rate = if (record != null && record.fullScore > 0) {
                (record.score / record.fullScore).toFloat()
            } else {
                0f
            }
            SubjectScore(
                name = name,
                rate = rate.coerceIn(0f, 1f),
                color = nameToColor[name] ?: ScoreTraceColors.BrandPrimary,
            )
        }

        // 单科趋势：所选科目历次成绩，按考试日期正序
        val subjectRecords = records
            .filter { it.subjectId == idByName[controls.subject] }
            .sortedBy { it.examDate }
        val subjectTrend = SubjectTrend(
            points = subjectRecords.map { TrendPoint(monthLabel(it.examDate), it.score.toInt()) },
            fullScore = subjectRecords.lastOrNull()?.fullScore?.toInt() ?: 0,
            latest = subjectRecords.lastOrNull()?.score?.toInt(),
        )

        // 总分趋势：按 range 从真实考试序列截取（正序）
        val ascending = aggregated.asReversed()
        val ranged = when (controls.range) {
            TrendRange.Recent6 -> ascending.takeLast(6)
            TrendRange.Recent10 -> ascending.takeLast(10)
            TrendRange.All -> ascending
        }
        val trendPoints = ranged.map { TrendPoint(monthLabel(it.date), it.totalScore) }

        // 历史记录：聚合 -> 计算与上一次考试的差值（aggregated 为日期倒序）
        val history = aggregated.mapIndexed { index, exam ->
            val previous = aggregated.getOrNull(index + 1)
            val rank = rankRecordFor(exams, exam.name)
            ExamHistoryItem(
                id = examNameToId(exam.name),
                examName = exam.name,
                date = exam.date.toLocalDate(),
                totalScore = exam.totalScore,
                fullScore = exam.fullScore,
                deltaFromLast = previous?.let { exam.totalScore - it.totalScore },
                classRank = rank?.classRank,
                gradeRank = rank?.gradeRank,
            )
        }

        val latest = aggregated.firstOrNull()
        val previous = aggregated.getOrNull(1)
        val latestRank = latest?.let { rankRecordFor(exams, it.name) }

        ScorePageUiState(
            isLoading = false,
            hasScores = latest != null,
            latestExamName = latest?.name.orEmpty(),
            latestExamDate = latest?.date?.let { DateUtils.formatDate(it) }.orEmpty(),
            latestTotalScore = latest?.totalScore ?: 0,
            latestFullScore = latest?.fullScore ?: 0,
            deltaFromLast = previous?.let { (latest?.totalScore ?: 0) - it.totalScore },
            classRank = latestRank?.classRank,
            gradeRank = latestRank?.gradeRank,
            totalRate = latest?.let { exam ->
                if (exam.fullScore > 0) exam.totalScore.toFloat() / exam.fullScore else 0f
            } ?: 0f,
            targetScore = targetSchool?.targetScore,
            trendRange = controls.range,
            trendPoints = trendPoints,
            analysisTab = controls.tab,
            selectedSubject = controls.subject,
            subjectRates = subjectRates,
            subjectTrend = subjectTrend,
            historyGroups = groupByMonth(history),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScorePageUiState(),
    )

    fun setTrendRange(range: TrendRange) {
        _trendRange.value = range
    }

    fun setAnalysisTab(tab: AnalysisTab) {
        _analysisTab.value = tab
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    /**
     * 删除一次考试：按 [examId]（由考试名派生）定位考试名，再删掉该考试名下的全部单科记录。
     * 传 id 而非原始名字，从根本上避免空值/重名造成的越界删除；删除后提示剩余考试数。
     */
    fun deleteExam(examId: Long) {
        viewModelScope.launch {
            val examName = aggregateExams(scoreRecordRepository.observeRecords(null).first())
                .firstOrNull { examNameToId(it.name) == examId }
                ?.name
                ?: return@launch
            scoreRecordRepository.deleteExamByName(examName)
            val remaining = aggregateExams(scoreRecordRepository.observeRecords(null).first()).size
            AppLogger.i("ScoreDelete", "examId=$examId exam=$examName remaining=$remaining")
            AppToast.success("已删除 1 条，剩 $remaining 条记录")
        }
    }
}

/** 考试日期 → X 轴标签（如「10月」）。 */
private fun monthLabel(date: Date): String =
    "${date.toInstant().atZone(ZoneId.systemDefault()).monthValue}月"

private fun Date.toLocalDate(): LocalDate =
    toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

/** 按考试日期相对当前月份分组：本月 / 上月 / 更早。 */
private fun groupByMonth(exams: List<ExamHistoryItem>): List<HistoryGroup> {
    val now = LocalDate.now()
    val lastMonthDate = now.withDayOfMonth(1).minusMonths(1)
    val thisMonth = exams.filter { it.date.year == now.year && it.date.monthValue == now.monthValue }
    val lastMonth = exams.filter {
        it.date.year == lastMonthDate.year && it.date.monthValue == lastMonthDate.monthValue
    }
    val earlier = exams.filterNot { it in thisMonth || it in lastMonth }
    return buildList {
        if (thisMonth.isNotEmpty()) add(HistoryGroup("本月", thisMonth))
        if (lastMonth.isNotEmpty()) add(HistoryGroup("上月", lastMonth))
        if (earlier.isNotEmpty()) add(HistoryGroup("更早", earlier))
    }
}
