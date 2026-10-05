package com.fenji.scoretrace.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.repository.StudySessionRepository
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 计时模式 */
enum class TimerMode(val label: String) {
    Stopwatch("正计时"),
    Countdown("倒计时"),
}

/** 倒计时可选档位（分钟） */
val COUNTDOWN_PRESETS = listOf(15, 25, 45, 60)

const val DEFAULT_TARGET_SECONDS = 25 * 60

data class TimerUiState(
    val mode: TimerMode = TimerMode.Stopwatch,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val targetSeconds: Int = DEFAULT_TARGET_SECONDS,
    val todaySeconds: Int = 0,
    val totalSeconds: Int = 0,
    val sessionCount: Int = 0,
)

@HiltViewModel
class StudyTimerViewModel @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    private var tickerJob: Job? = null

    /** 本次计时的墙上时钟起点（暂停不计；用于落库 startedAt） */
    private var sessionStartedAt: Long = 0L

    init {
        viewModelScope.launch {
            studySessionRepository.observeSessions().collect { sessions ->
                val startOfDay = DateUtils.startOfDay().time
                _state.update {
                    it.copy(
                        todaySeconds = sessions.filter { s -> s.startedAt >= startOfDay }
                            .sumOf { s -> s.durationSeconds },
                        totalSeconds = sessions.sumOf { s -> s.durationSeconds },
                        sessionCount = sessions.size,
                    )
                }
            }
        }
    }

    fun setMode(mode: TimerMode) {
        if (_state.value.isRunning || mode == _state.value.mode) return
        _state.update { it.copy(mode = mode, elapsedSeconds = 0) }
    }

    /** 自定义倒计时时长（分钟），限制 1~240。 */
    fun setTargetMinutes(minutes: Int) {
        _state.update { it.copy(targetSeconds = minutes.coerceIn(1, 240) * 60) }
    }

    fun start() {
        if (_state.value.isRunning) return
        if (_state.value.elapsedSeconds == 0) sessionStartedAt = System.currentTimeMillis()
        _state.update { it.copy(isRunning = true) }
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    fun pause() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(isRunning = false) }
    }

    /** 重置：丢弃当前计时，不清除历史记录。 */
    fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(isRunning = false, elapsedSeconds = 0) }
    }

    /** 结束并保存：把当前已计时长写入数据库，然后归零。 */
    fun saveSession() {
        val snapshot = _state.value
        tickerJob?.cancel()
        tickerJob = null
        val seconds = snapshot.elapsedSeconds
        _state.update { it.copy(isRunning = false, elapsedSeconds = 0) }
        if (seconds < 1) return
        val type = if (snapshot.mode == TimerMode.Countdown) TYPE_COUNTDOWN else TYPE_STOPWATCH
        viewModelScope.launch {
            studySessionRepository.addSession(sessionStartedAt, seconds, type)
            _toast.emit("已保存本次专注 ${formatFocus(seconds)}")
        }
    }

    private fun tick() {
        val current = _state.value
        val next = current.elapsedSeconds + 1
        if (current.mode == TimerMode.Countdown && next >= current.targetSeconds) {
            // 倒计时完成：自动落库 + 提示 + 归零
            val started = sessionStartedAt
            val target = current.targetSeconds
            tickerJob?.cancel()
            tickerJob = null
            _state.update { it.copy(isRunning = false, elapsedSeconds = 0) }
            viewModelScope.launch {
                studySessionRepository.addSession(started, target, TYPE_COUNTDOWN)
                _toast.emit("专注完成！已记录 ${formatFocus(target)}")
            }
        } else {
            _state.update { it.copy(elapsedSeconds = next) }
        }
    }

    private companion object {
        const val TYPE_STOPWATCH = "stopwatch"
        const val TYPE_COUNTDOWN = "countdown"
    }
}

/** 秒 → 「mm:ss」或「h:mm:ss」 */
fun formatClock(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 秒 → 人类可读时长（用于统计与提示） */
fun formatFocus(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    return when {
        h > 0 -> "${h}小时${m}分钟"
        m > 0 -> "${m}分钟"
        else -> "${seconds}秒"
    }
}
