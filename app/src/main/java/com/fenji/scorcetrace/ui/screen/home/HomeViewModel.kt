package com.fenji.scorcetrace.ui.screen.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.UserPreferences
import com.fenji.scorcetrace.data.local.entity.TargetSchool
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    /** 进入应用是否自动播放音乐（来自 DataStore） */
    val autoPlayMusic: Boolean = true,
    /** 最新设定的目标院校；null 表示尚未设定（首页显示引导态） */
    val targetSchool: TargetSchool? = null,
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
    userPreferences: UserPreferences,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val subjectRepository: SubjectRepository,
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
    ) { autoPlayMusic, targetSchool ->
        HomeUiState(
            autoPlayMusic = autoPlayMusic,
            targetSchool = targetSchool,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

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

    /** 今日 AI 重点（本地占位文案池，后续接 AI 接口） */
    private val _aiTip = MutableStateFlow(AI_TIPS.first())
    val aiTip: StateFlow<String> = _aiTip.asStateFlow()

    fun refreshAiTip() {
        _aiTip.value = AI_TIPS.filter { it != _aiTip.value }.random()
    }

    /** 保存目标院校：[id] 传已有值则覆盖同一条记录，传 0 为新增 */
    fun saveTargetSchool(
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
        id: Long = 0L,
    ) {
        viewModelScope.launch {
            targetSchoolRepository.save(
                schoolName = schoolName,
                majorName = majorName,
                targetScore = targetScore,
                currentScore = currentScore,
                year = year,
                id = id,
            )
        }
    }

    fun deleteTargetSchool(entity: TargetSchool) {
        viewModelScope.launch { targetSchoolRepository.delete(entity) }
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

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_HOUR = 3_600_000L
        const val MILLIS_PER_DAY = 86_400_000L

        const val HOME_RECENT_SCORE_LIMIT = 50

        /** 成绩概览固定展示的六科 */
        val SIX_SUBJECTS = listOf("语文", "数学", "英语", "物理", "化学", "生物")

        /** 进度条配色（按设计稿：语数蓝 / 英物生绿 / 化橙） */
        fun colorForSubject(name: String): Color = when (name) {
            "语文" -> ScoreTraceColors.BrandPrimary
            "数学" -> ScoreTraceColors.BrandPrimaryDark
            "化学" -> ScoreTraceColors.WarningOrange
            else -> ScoreTraceColors.SuccessGreen
        }

        val AI_TIPS = listOf(
            "数学三角函数失分最多，建议专项练 40 分钟。",
            "英语阅读理解正确率下降，每天精读 2 篇。",
            "物理电磁学公式不熟练，整理错题本复习。",
            "化学方程式配平易错，做 15 道专项练习。",
            "语文作文素材积累不足，每周摘抄 3 段。",
        )
    }
}
