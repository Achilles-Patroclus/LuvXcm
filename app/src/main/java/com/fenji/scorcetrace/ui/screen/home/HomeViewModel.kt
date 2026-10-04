package com.fenji.scorcetrace.ui.screen.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.UserPreferences
import com.fenji.scorcetrace.data.local.entity.ExamRecord
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.TargetSchool
import com.fenji.scorcetrace.data.remote.deepseek.dto.ChatRequest
import com.fenji.scorcetrace.data.repository.DeepSeekRepository
import com.fenji.scorcetrace.data.repository.ExamRecordRepository
import com.fenji.scorcetrace.data.repository.NotificationRepository
import com.fenji.scorcetrace.data.repository.NotificationType
import com.fenji.scorcetrace.data.repository.SchoolRepository
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
import com.fenji.scorcetrace.data.repository.SubjectRepository
import com.fenji.scorcetrace.data.repository.TargetSchoolRepository
import com.fenji.scorcetrace.ui.component.SubjectScore
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

data class HomeUiState(
    /** 进入应用是否自动播放音乐（来自 DataStore） */
    val autoPlayMusic: Boolean = true,
    /** 最新设定的目标院校；null 表示尚未设定（首页显示引导态） */
    val targetSchool: TargetSchool? = null,
    /** 目标院校校徽地址（按校名从 schools.json 反查）；未知院校为 null，UI 回退首字 */
    val targetSchoolLogoUrl: String? = null,
    /** 最近一次考试总分；null 表示暂无成绩 */
    val latestTotalScore: Int? = null,
    /** 班级排名文案（如「班级第15」）；无排名时为空串 */
    val latestRankText: String = "",
    /** 年级排名文案（如「年级第3」）；无排名时为空串 */
    val latestGradeRankText: String = "",
    /** 与上一次考试的总分差；null 表示无从计算 */
    val scoreDelta: Int? = null,
    val isLoading: Boolean = true,
)

/** 高考倒计时状态 */
data class CountdownUiState(
    val isEnded: Boolean = false,
    val days: Long = 0L,
    val hours: Long = 0L,
    val minutes: Long = 0L,
    val seconds: Long = 0L,
    val targetDateText: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val subjectRepository: SubjectRepository,
    private val schoolRepository: SchoolRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val deepSeekRepository: DeepSeekRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    /** 立即发射一次当前时间，之后每秒发射一次，驱动秒级倒计时 */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(MILLIS_PER_SECOND)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferences.autoPlayMusic,
        targetSchoolRepository.observeLatest(),
        scoreRecordRepository.observeRecent(RECENT_SCORE_LIMIT),
        examRecordRepository.observeAll(),
    ) { autoPlayMusic, targetSchool, scores, exams ->
        val summary = summarizeScores(scores, exams)
        HomeUiState(
            autoPlayMusic = autoPlayMusic,
            targetSchool = targetSchool,
            targetSchoolLogoUrl = targetSchool?.let { schoolRepository.findByName(it.schoolName)?.logoUrl },
            latestTotalScore = summary.latestTotal,
            latestRankText = summary.rankText,
            latestGradeRankText = summary.gradeRankText,
            scoreDelta = summary.delta,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** 未读通知数：驱动首页铃铛红点的显示/隐藏 */
    val unreadCount: StateFlow<Int> = notificationRepository.observeUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /**
     * 年度进度：以「目标高考日前一年」为备考开始日，计算到高考日已完成的百分比。
     * 与倒计时共用同一目标时间戳，改高考日期后自动跟随。
     */
    val yearPassedPercent: StateFlow<Int> = userPreferences.gaokaoTimestamp
        .map { computeYearPassedPercent(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = computeYearPassedPercent(DateUtils.defaultGaokaoTimestamp()),
        )

    /** 今日 AI 重点（每天生成一次，缓存到 DataStore） */
    private val _aiFocus = MutableStateFlow(DEFAULT_AI_FOCUS)
    val aiFocus: StateFlow<String> = _aiFocus.asStateFlow()

    init {
        viewModelScope.launch { loadOrGenerateAiFocus() }
    }

    /** 目标时间戳来自 DataStore，变化后会自动重新计算倒计时 */
    val countdownState: StateFlow<CountdownUiState> = combine(
        userPreferences.gaokaoTimestamp,
        ticker,
    ) { targetTimestamp, now ->
        calculateCountdown(targetTimestamp, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateCountdown(
            DateUtils.defaultGaokaoTimestamp(),
            System.currentTimeMillis(),
        ),
    )

    /** 备考天数：取倒计时天数并按天去重，避免秒级 tick 触发整页重组 */
    val studyDayCount: StateFlow<Int> = countdownState
        .map { it.days.toInt() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /** 六科得分率：取最近一次该科目成绩的 score / fullScore，无成绩记 0 */
    val subjectRates: StateFlow<List<SubjectScore>> = combine(
        scoreRecordRepository.observeRecent(HOME_RECENT_SCORE_LIMIT),
        subjectRepository.observeSubjects(),
    ) { records, subjects ->
        val idByName = subjects.associate { it.name to it.id }
        SIX_SUBJECTS.map { name ->
            val subjectId = idByName[name]
            val record = records
                .filter { it.subjectId == subjectId }
                .maxByOrNull { it.examDate }
            val rate = if (record != null && record.fullScore > 0) {
                (record.score / record.fullScore).toFloat()
            } else {
                0f
            }
            SubjectScore(
                name = name,
                rate = rate.coerceIn(0f, 1f),
                color = colorForSubject(name),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SIX_SUBJECTS.map { SubjectScore(it, 0f, colorForSubject(it)) },
    )

    fun deleteTargetSchool(entity: TargetSchool) {
        viewModelScope.launch { targetSchoolRepository.delete(entity) }
    }

    /**
     * 今日 AI 重点：当天已生成则直接用缓存；否则调用 DeepSeek 依据最近成绩生成一条，
     * 写入 DataStore 并生成一条「AI 建议」通知（当天只生成一次）。
     */
    private suspend fun loadOrGenerateAiFocus() {
        val today = LocalDate.now().toString()
        val cachedDate = userPreferences.aiFocusDate.first()
        val cached = userPreferences.aiFocus.first()
        if (cachedDate == today && !cached.isNullOrBlank()) {
            _aiFocus.value = cached
            return
        }
        val scoreSummary = buildScoreSummary()
        val targetSchoolName = targetSchoolRepository.observeLatest().first()?.schoolName
        val prompt = """
            你是高考备考助手。根据以下最近成绩，给出一条今日学习重点建议（不超过 50 字）：
            最近成绩：$scoreSummary
            目标院校：${targetSchoolName ?: "未设定"}
            要求：指出最需要提升的科目或知识点，给出具体、可执行的行动建议。
        """.trimIndent()
        val result = deepSeekRepository.chat(listOf(ChatRequest.Message(role = "user", content = prompt)))
        val focus = result.getOrNull()
            ?.choices?.firstOrNull()?.message?.content?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: cached?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AI_FOCUS
        _aiFocus.value = focus
        userPreferences.saveAiFocus(focus, today)
        if (result.isSuccess) {
            // 覆盖当天旧记录，避免同一天生成多条「今日 AI 重点」
            notificationRepository.addReplacingToday(
                type = NotificationType.AI,
                title = "今日 AI 重点",
                content = focus,
            )
        }
    }

    /** 拼「最近一次考试」的成绩摘要，供 AI 生成建议使用。 */
    private suspend fun buildScoreSummary(): String {
        val records = scoreRecordRepository.observeRecent(RECENT_SCORE_LIMIT).first()
        val subjects = subjectRepository.observeSubjects().first()
        if (records.isEmpty()) return "暂无成绩数据"
        val idToName = subjects.associate { it.id to it.name }
        val latestName = records.maxByOrNull { it.examDate }?.examName
        val latest = records.filter { it.examName == latestName }
        val total = latest.sumOf { it.score }.toInt()
        val detail = latest.joinToString("，") { "${idToName[it.subjectId].orEmpty()}${it.score.toInt()}" }
        return "最近一次「$latestName」总分 $total（$detail）"
    }

    /** 把单科成绩按考试名聚合出总分/排名/较上次变化。 */
    private fun summarizeScores(scores: List<ScoreRecord>, exams: List<ExamRecord>): ScoreSummary {
        if (scores.isEmpty()) return ScoreSummary()
        val byExam = scores.groupBy { it.examName }
            .map { (name, records) ->
                ExamAggregate(
                    name = name,
                    date = records.maxOf { it.examDate },
                    total = records.sumOf { it.score }.toInt(),
                )
            }
            .sortedByDescending { it.date }
        val latest = byExam.first()
        val previous = byExam.getOrNull(1)
        val exam = exams.firstOrNull { it.examName == latest.name }
        return ScoreSummary(
            latestTotal = latest.total,
            rankText = exam?.classRank?.let { "班级第$it" } ?: "",
            gradeRankText = exam?.gradeRank?.let { "年级第$it" } ?: "",
            delta = previous?.let { latest.total - it.total },
        )
    }

    private fun calculateCountdown(targetTimestamp: Long, now: Long): CountdownUiState {
        val targetDateText = DateUtils.formatDate(Date(targetTimestamp))
        val diff = targetTimestamp - now
        if (diff <= 0L) {
            return CountdownUiState(isEnded = true, targetDateText = targetDateText)
        }
        return CountdownUiState(
            isEnded = false,
            days = diff / MILLIS_PER_DAY,
            hours = diff % MILLIS_PER_DAY / MILLIS_PER_HOUR,
            minutes = diff % MILLIS_PER_HOUR / MILLIS_PER_MINUTE,
            seconds = diff % MILLIS_PER_MINUTE / MILLIS_PER_SECOND,
            targetDateText = targetDateText,
        )
    }

    private data class ExamAggregate(val name: String, val date: Date, val total: Int)

    private data class ScoreSummary(
        val latestTotal: Int? = null,
        val rankText: String = "",
        val gradeRankText: String = "",
        val delta: Int? = null,
    )

    /** 已完成百分比 = （今天 - 备考开始日）/（高考日 - 备考开始日），截断在 0~100。 */
    private fun computeYearPassedPercent(targetTimestamp: Long): Int {
        val examDate = Instant.ofEpochMilli(targetTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = examDate.minusYears(1)
        val totalDays = ChronoUnit.DAYS.between(startDate, examDate)
        if (totalDays <= 0L) return 0
        val elapsedDays = ChronoUnit.DAYS.between(startDate, LocalDate.now())
        return (elapsedDays * 100f / totalDays).roundToInt().coerceIn(0, 100)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_HOUR = 3_600_000L
        const val MILLIS_PER_DAY = 86_400_000L

        const val HOME_RECENT_SCORE_LIMIT = 50
        const val RECENT_SCORE_LIMIT = 200

        const val DEFAULT_AI_FOCUS = "保持每日学习节奏，重点突破薄弱科目。"

        /** 成绩概览固定展示的六科 */
        val SIX_SUBJECTS = listOf("语文", "数学", "英语", "物理", "化学", "生物")

        /** 进度条配色（按设计稿：语数蓝 / 英语绿 / 物理深绿 / 化学橙 / 生物青） */
        fun colorForSubject(name: String): Color = when (name) {
            "语文" -> ScoreTraceColors.BrandPrimary
            "数学" -> ScoreTraceColors.BrandPrimaryDark
            "英语" -> ScoreTraceColors.SuccessGreen
            "物理" -> ScoreTraceColors.SubjectPhysicsGreen
            "化学" -> ScoreTraceColors.WarningOrange
            "生物" -> ScoreTraceColors.SubjectBiologyTeal
            else -> ScoreTraceColors.SuccessGreen
        }
    }
}
