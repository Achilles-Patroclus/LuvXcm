package com.fenji.scorcetrace.ui.screen.score

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
import com.fenji.scorcetrace.data.repository.SubjectRepository
import com.fenji.scorcetrace.ui.component.SubjectScore
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
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

/**
 * 一次考试的历史记录（按考试聚合）。
 *
 * TODO: 占位模型，当前 `ScoreRecord` 是单科成绩，无考试总分/排名字段；后续由 `ExamSummary` 实体提供。
 */
data class ExamHistoryItem(
    val id: Long,
    val examName: String,
    val date: LocalDate,
    val totalScore: Int,
    val fullScore: Int,
    /** 较上次变化（正=上升，负=下降） */
    val deltaFromLast: Int,
    val classRank: Int,
    val gradeRank: Int,
)

/** 历史记录分组（本月 / 上月 / 更早） */
data class HistoryGroup(
    val title: String,
    val items: List<ExamHistoryItem>,
)

data class ScorePageUiState(
    /** 最近一次考试名称 */
    val latestExamName: String = "高三10月月考",
    val latestExamDate: String = "2026-10-01",
    // TODO: 硬编码占位，后续新增 ExamSummary 实体后替换
    val latestTotalScore: Int = 562,
    val latestFullScore: Int = 750,
    val deltaFromLast: Int = 12,
    val classRank: Int = 15,
    val gradeRank: Int = 120,
    val totalRate: Float = 0.749f,
    /** 目标分：后续从目标院校换算 */
    val targetScore: Int = 680,
    val trendRange: TrendRange = TrendRange.Recent6,
    val trendPoints: List<TrendPoint> = emptyList(),
    val analysisTab: AnalysisTab = AnalysisTab.Radar,
    val selectedSubject: String = "数学",
    val subjectRates: List<SubjectScore> = emptyList(),
    val historyGroups: List<HistoryGroup> = emptyList(),
)

/** 各科分析的三个可变控制项合并成一个流，便于与其它数据流一起 combine */
private data class AnalysisControls(
    val range: TrendRange,
    val tab: AnalysisTab,
    val subject: String,
)

/** TODO: 占位考试历史，后续由 ExamSummary 实体提供 */
private val DEFAULT_EXAMS = listOf(
    ExamHistoryItem(1, "高三10月月考", LocalDate.of(2026, 10, 1), 562, 750, 12, 15, 120),
    ExamHistoryItem(2, "九月阶段测", LocalDate.of(2026, 9, 18), 550, 750, 8, 22, 168),
    ExamHistoryItem(3, "高三开学考", LocalDate.of(2026, 9, 5), 542, 750, 11, 27, 205),
    ExamHistoryItem(4, "暑期摸底考", LocalDate.of(2026, 8, 12), 531, 750, 23, 30, 231),
    ExamHistoryItem(5, "高二期末考", LocalDate.of(2026, 7, 8), 508, 750, -5, 35, 288),
)

@HiltViewModel
class ScoreViewModelNew @Inject constructor(
    private val scoreRecordRepository: ScoreRecordRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _trendRange = MutableStateFlow(TrendRange.Recent6)
    private val _analysisTab = MutableStateFlow(AnalysisTab.Radar)
    private val _selectedSubject = MutableStateFlow("数学")
    private val _exams = MutableStateFlow(DEFAULT_EXAMS)

    private val _controls = combine(_trendRange, _analysisTab, _selectedSubject) { range, tab, subject ->
        AnalysisControls(range, tab, subject)
    }

    val uiState: StateFlow<ScorePageUiState> = combine(
        scoreRecordRepository.observeRecent(50),
        subjectRepository.observeSubjects(),
        _controls,
        _exams,
    ) { records, subjects, controls, exams ->
        val nameToColor = subjects.associate { it.name to Color(it.color) }
        // 六科得分率：取该科最近一次成绩（records 已按考试日期倒序）
        val sixSubjects = listOf("语文", "数学", "英语", "物理", "化学", "生物")
        val subjectRates = sixSubjects.map { name ->
            val subject = subjects.firstOrNull { it.name == name }
            val record = records.firstOrNull { it.subjectId == subject?.id }
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

        // TODO: 硬编码占位，后续从 ExamSummary 获取每次考试的总分
        val trendPoints = when (controls.range) {
            TrendRange.Recent6 -> listOf(
                TrendPoint("5月", 508), TrendPoint("6月", 515),
                TrendPoint("7月", 531), TrendPoint("8月", 540),
                TrendPoint("9月", 550), TrendPoint("10月", 562),
            )

            TrendRange.Recent10 -> listOf(
                TrendPoint("1月", 480), TrendPoint("2月", 485),
                TrendPoint("3月", 490), TrendPoint("4月", 500),
                TrendPoint("5月", 508), TrendPoint("6月", 515),
                TrendPoint("7月", 531), TrendPoint("8月", 540),
                TrendPoint("9月", 550), TrendPoint("10月", 562),
            )

            TrendRange.All -> listOf(
                TrendPoint("9月", 460), TrendPoint("10月", 470),
                TrendPoint("11月", 480), TrendPoint("12月", 485),
                TrendPoint("1月", 480), TrendPoint("2月", 485),
                TrendPoint("3月", 490), TrendPoint("4月", 500),
                TrendPoint("5月", 508), TrendPoint("6月", 515),
                TrendPoint("7月", 531), TrendPoint("8月", 540),
                TrendPoint("9月", 550), TrendPoint("10月", 562),
            )
        }

        ScorePageUiState(
            subjectRates = subjectRates,
            trendRange = controls.range,
            trendPoints = trendPoints,
            analysisTab = controls.tab,
            selectedSubject = controls.subject,
            historyGroups = groupByMonth(exams),
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

    /** TODO: 仅内存删除占位，接入 ExamSummary 后改为删库 */
    fun deleteExam(id: Long) {
        _exams.value = _exams.value.filterNot { it.id == id }
    }
}

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
