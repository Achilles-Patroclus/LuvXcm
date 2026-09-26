package com.fenji.scorcetrace.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.UserPreferences
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.StudyTask
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.data.local.entity.TargetSchool
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
import com.fenji.scorcetrace.data.repository.StudyTaskRepository
import com.fenji.scorcetrace.data.repository.SubjectRepository
import com.fenji.scorcetrace.data.repository.TargetSchoolRepository
import com.fenji.scorcetrace.util.Constants
import com.fenji.scorcetrace.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    val todayTasks: List<StudyTask> = emptyList(),
    val recentScores: List<ScoreRecord> = emptyList(),
    val subjects: List<Subject> = emptyList(),
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
    private val studyTaskRepository: StudyTaskRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    subjectRepository: SubjectRepository,
    userPreferences: UserPreferences,
    private val targetSchoolRepository: TargetSchoolRepository,
) : ViewModel() {

    /** 立即发射一次当前时间，之后每秒发射一次，驱动秒级倒计时 */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(MILLIS_PER_SECOND)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        studyTaskRepository.observeTodayTasks(),
        scoreRecordRepository.observeRecent(Constants.HOME_RECENT_SCORE_LIMIT),
        subjectRepository.observeSubjects(),
        userPreferences.autoPlayMusic,
        targetSchoolRepository.observeLatest(),
    ) { tasks, scores, subjects, autoPlayMusic, targetSchool ->
        HomeUiState(
            todayTasks = tasks,
            recentScores = scores,
            subjects = subjects,
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

    fun toggleTask(task: StudyTask) {
        viewModelScope.launch { studyTaskRepository.setCompleted(task.id, !task.isCompleted) }
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
    }
}
