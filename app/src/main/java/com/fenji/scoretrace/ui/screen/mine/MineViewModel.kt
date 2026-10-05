package com.fenji.scoretrace.ui.screen.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.repository.NetworkRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.StudyTaskRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class MineUiState(
    val isLoading: Boolean = true,
    /** 届别（由高考日期年份推导，如「2027」） */
    val examYear: String = "",
    /** 高考日期文本（yyyy-MM-dd） */
    val gaokaoDateText: String = "",
    /** 目标院校名；未设置时为空 */
    val targetSchool: String = "",
    /** 连续打卡天数（占位，后续由打卡记录计算） */
    val streakDays: Int = 36,
    /** 学习天数（占位，后续由使用记录计算） */
    val studyDays: Int = 247,
    /** 成绩记录条数 */
    val scoreRecordCount: Int = 0,
    /** 已完成任务数 */
    val taskCompletedCount: Int = 0,
    val darkTheme: Boolean = false,
    val autoPlayMusic: Boolean = true,
    /** 字体大小档位（占位，后续全局生效） */
    val fontSize: String = "标准",
    /** 当前选科（3+1+2：1 门首选 + 2 门再选），来自 UserPreferences */
    val selectedSubjects: List<String> = emptyList(),
)

@HiltViewModel
class MineViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val studyTaskRepository: StudyTaskRepository,
    private val networkRepository: NetworkRepository,
) : ViewModel() {

    private val _networkIp = MutableStateFlow(IP_LOADING)
    val networkIp: StateFlow<String> = _networkIp.asStateFlow()

    val uiState: StateFlow<MineUiState> = combine(
        userPreferences.darkTheme,
        userPreferences.autoPlayMusic,
        userPreferences.gaokaoTimestamp,
        targetSchoolRepository.observeLatest(),
        combine(
            scoreRecordRepository.observeRecent(RECENT_SCORE_LIMIT),
            studyTaskRepository.observeTasks(null),
            userPreferences.selectedSubjects,
        ) { scores, tasks, subjects ->
            MineStats(
                scoreCount = scores.size,
                taskCompletedCount = tasks.count { it.isCompleted },
                selectedSubjects = subjects,
            )
        },
    ) { darkTheme, autoPlayMusic, gaokaoTimestamp, targetSchool, stats ->
        MineUiState(
            isLoading = false,
            examYear = examYearOf(gaokaoTimestamp),
            gaokaoDateText = DateUtils.formatDate(Date(gaokaoTimestamp)),
            targetSchool = targetSchool?.schoolName.orEmpty(),
            scoreRecordCount = stats.scoreCount,
            taskCompletedCount = stats.taskCompletedCount,
            selectedSubjects = stats.selectedSubjects,
            darkTheme = darkTheme,
            autoPlayMusic = autoPlayMusic,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MineUiState(),
    )

    init {
        refreshNetworkIp()
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(value) }
    }

    fun setAutoPlayMusic(value: Boolean) {
        viewModelScope.launch { userPreferences.setAutoPlayMusic(value) }
    }

    /** 保存选科（1 门首选 + 2 门再选），持久化到 UserPreferences。 */
    fun updateSelectedSubjects(subjects: List<String>) {
        viewModelScope.launch { userPreferences.setSelectedSubjects(subjects) }
    }

    fun refreshNetworkIp() {
        viewModelScope.launch {
            _networkIp.value = IP_LOADING
            _networkIp.value = networkRepository.fetchUserIp() ?: IP_FAILED
        }
    }

    private fun examYearOf(gaokaoTimestamp: Long): String =
        Calendar.getInstance().apply { timeInMillis = gaokaoTimestamp }
            .get(Calendar.YEAR)
            .toString()

    private data class MineStats(
        val scoreCount: Int,
        val taskCompletedCount: Int,
        val selectedSubjects: List<String>,
    )

    private companion object {
        const val RECENT_SCORE_LIMIT = 100
        const val IP_LOADING = "获取中…"
        const val IP_FAILED = "获取失败"
    }
}
