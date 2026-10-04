package com.fenji.scoretrace.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.repository.NetworkRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.StudyTaskRepository
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: Boolean = false,
    val autoPlayMusic: Boolean = true,
    val versionName: String = BuildConfig.VERSION_NAME,
    val gaokaoTimestamp: Long = 0L,
    val gaokaoDateText: String = "",
)

/** 公网 IP 的加载状态：三态独立于设置项状态，避免网络波动影响其它设置。 */
sealed interface IpUiState {
    data object Loading : IpUiState
    data class Success(val ip: String) : IpUiState
    data object Error : IpUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val studyTaskRepository: StudyTaskRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val userPreferences: UserPreferences,
    private val networkRepository: NetworkRepository,
) : ViewModel() {

    // 注：主题偏好目前只保存在内存中，后续接入持久化后再驱动根主题
    private val darkTheme = MutableStateFlow(false)

    private val _ipState = MutableStateFlow<IpUiState>(IpUiState.Loading)
    val ipState: StateFlow<IpUiState> = _ipState.asStateFlow()

    init {
        refreshIp()
    }

    /** 重新拉取公网 IP；失败保持 [IpUiState.Error]，由 UI 提供重试入口 */
    fun refreshIp() {
        viewModelScope.launch {
            _ipState.value = IpUiState.Loading
            val ip = networkRepository.fetchUserIp()
            _ipState.value = if (ip.isNullOrBlank()) IpUiState.Error else IpUiState.Success(ip)
        }
    }
    val uiState: StateFlow<SettingsUiState> = combine(
        darkTheme,
        userPreferences.gaokaoTimestamp,
        userPreferences.autoPlayMusic,
    ) { dark, gaokaoTimestamp, autoPlayMusic ->
        SettingsUiState(
            darkTheme = dark,
            autoPlayMusic = autoPlayMusic,
            gaokaoTimestamp = gaokaoTimestamp,
            gaokaoDateText = DateUtils.formatDate(Date(gaokaoTimestamp)),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = defaultSettingsState(),
    )

    /** 当前高考日期（时间戳），用于初始化日期选择器 */
    fun currentGaokaoTimestamp(): Long = uiState.value.gaokaoTimestamp

    /** 保存选中的日期：换算成本地时区当天 0 点后写入 DataStore */
    fun saveGaokaoDate(utcDateMillis: Long) {
        val timestamp = DateUtils.localMidnightFromUtcDate(utcDateMillis)
        viewModelScope.launch { userPreferences.saveGaokaoTimestamp(timestamp) }
    }

    fun setDarkTheme(enabled: Boolean) {
        darkTheme.value = enabled
    }

    fun setAutoPlayMusic(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAutoPlayMusic(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            studyTaskRepository.clearAll()
            scoreRecordRepository.clearAll()
        }
    }

    private fun defaultSettingsState(): SettingsUiState {
        val timestamp = DateUtils.defaultGaokaoTimestamp()
        return SettingsUiState(
            gaokaoTimestamp = timestamp,
            gaokaoDateText = DateUtils.formatDate(Date(timestamp)),
        )
    }
}
