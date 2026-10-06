package com.fenji.scoretrace.ui.screen.mine

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.repository.ConversationRepository
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.StudySessionRepository
import com.fenji.scoretrace.data.repository.StudyTaskRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import com.fenji.scoretrace.util.AppToastType
import com.fenji.scoretrace.util.AvatarStore
import com.fenji.scoretrace.util.DateUtils
import com.fenji.scoretrace.util.LocationHelper
import com.fenji.scoretrace.util.ScoreExporter
import com.fenji.scoretrace.util.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/** 省份定位状态（驱动「省份」项的副标题与刷新按钮）。 */
enum class ProvinceStatus { Success, Locating, Failed, PermissionDenied }

data class MineUiState(
    val isLoading: Boolean = true,
    /** 届别（由高考日期年份推导，如「2027」） */
    val examYear: String = "",
    /** 高考日期文本（yyyy-MM-dd） */
    val gaokaoDateText: String = "",
    /** 目标院校名；未设置时为空 */
    val targetSchool: String = "",
    /** 备考天数：从「高考日 − 1 年」到今天，与首页口径一致 */
    val studyDays: Int = 0,
    /** 成绩记录：不同考试（examName）的场次数 */
    val examCount: Int = 0,
    /** 连续打卡天数：按有专注记录的连续自然日计算 */
    val continuousCheckIn: Int = 0,
    /** 主题模式：light / dark / system */
    val themeMode: String = "system",
    val autoPlayMusic: Boolean = true,
    /** 当前选科（3+1+2：1 门首选 + 2 门再选），来自 UserPreferences */
    val selectedSubjects: List<String> = emptyList(),
    /** 所在省份（省级行政区名），来自 UserPreferences */
    val province: String = "云南",
    /** 用户昵称，默认「备考人」 */
    val nickname: String = "备考人",
    /** 本地头像文件路径；为 null 时使用默认头像 */
    val avatarPath: String? = null,
)

@HiltViewModel
class MineViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val userPreferences: UserPreferences,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val studyTaskRepository: StudyTaskRepository,
    private val subjectRepository: SubjectRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val notificationRepository: NotificationRepository,
    private val studySessionRepository: StudySessionRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _provinceStatus = MutableStateFlow(ProvinceStatus.Success)
    val provinceStatus: StateFlow<ProvinceStatus> = _provinceStatus.asStateFlow()

    private val _toast = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 2)
    val toast: SharedFlow<ToastMessage> = _toast.asSharedFlow()

    val uiState: StateFlow<MineUiState> = combine(
        userPreferences.themeMode,
        userPreferences.autoPlayMusic,
        userPreferences.gaokaoTimestamp,
        targetSchoolRepository.observeLatest(),
        combine(
            scoreRecordRepository.observeAll(),
            studySessionRepository.observeSessions(),
            userPreferences.selectedSubjects,
            userPreferences.province,
            combine(userPreferences.nickname, userPreferences.avatarPath) { nickname, avatarPath ->
                nickname to avatarPath
            },
        ) { records, sessions, subjects, province, profile ->
            MineStats(
                examCount = records.map { it.examName }.filter { it.isNotBlank() }.distinct().size,
                continuousCheckIn = consecutiveCheckInDays(sessions.map { it.startedAt }),
                selectedSubjects = subjects,
                province = province,
                nickname = profile.first,
                avatarPath = profile.second,
            )
        },
    ) { themeMode, autoPlayMusic, gaokaoTimestamp, targetSchool, stats ->
        MineUiState(
            isLoading = false,
            examYear = examYearOf(gaokaoTimestamp),
            gaokaoDateText = DateUtils.formatDate(Date(gaokaoTimestamp)),
            targetSchool = targetSchool?.schoolName.orEmpty(),
            studyDays = studyDaysOf(gaokaoTimestamp),
            examCount = stats.examCount,
            continuousCheckIn = stats.continuousCheckIn,
            selectedSubjects = stats.selectedSubjects,
            province = stats.province,
            nickname = stats.nickname,
            avatarPath = stats.avatarPath,
            themeMode = themeMode,
            autoPlayMusic = autoPlayMusic,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MineUiState(),
    )

    init {
        viewModelScope.launch {
            // 首次启动若从未定位过（拒绝/失败/未询问），初始展示为「定位失败，点击刷新重试」
            _provinceStatus.value =
                if (userPreferences.provinceLocated.first()) ProvinceStatus.Success else ProvinceStatus.Failed
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setAutoPlayMusic(value: Boolean) {
        viewModelScope.launch { userPreferences.setAutoPlayMusic(value) }
    }

    fun setProvince(value: String) {
        viewModelScope.launch { userPreferences.setProvince(value) }
    }

    /** 重新定位省份：成功后写入偏好并提示，失败保持原值。 */
    fun refreshProvince() {
        viewModelScope.launch {
            _provinceStatus.value = ProvinceStatus.Locating
            val province = LocationHelper.getCurrentProvince(appContext)
            if (province != null) {
                userPreferences.setProvince(province)
                userPreferences.setProvinceLocated(true)
                _provinceStatus.value = ProvinceStatus.Success
                _toast.emit(ToastMessage("已更新为 $province", AppToastType.Success))
            } else {
                _provinceStatus.value = ProvinceStatus.Failed
                _toast.emit(ToastMessage("定位失败，请检查定位权限", AppToastType.Error))
            }
        }
    }

    fun onProvincePermissionDenied() {
        _provinceStatus.value = ProvinceStatus.PermissionDenied
        viewModelScope.launch { _toast.emit(ToastMessage("定位权限被拒，无法获取省份", AppToastType.Warning)) }
    }

    /** 保存昵称；空白输入回落到默认昵称。 */
    fun setNickname(value: String) {
        viewModelScope.launch { userPreferences.setNickname(value.trim().ifBlank { "备考人" }) }
    }

    /** 把相册选中的头像复制到私有目录并保存路径，失败提示。 */
    fun saveAvatar(uri: Uri) {
        viewModelScope.launch {
            val path = AvatarStore.save(appContext, uri)
            if (path != null) {
                userPreferences.setAvatarPath(path)
                _toast.emit(ToastMessage("头像已更新", AppToastType.Success))
            } else {
                _toast.emit(ToastMessage("头像保存失败", AppToastType.Error))
            }
        }
    }

    /** 导出全部成绩为 CSV/JSON，保存到系统下载目录。 */
    fun exportScores(format: String) {
        viewModelScope.launch {
            val records = scoreRecordRepository.observeAll().first()
            if (records.isEmpty()) {
                _toast.emit(ToastMessage("暂无可导出的成绩", AppToastType.Warning))
                return@launch
            }
            val subjectNames = subjectRepository.observeSubjects().first().associate { it.id to it.name }
            try {
                val count = ScoreExporter.export(appContext, records, subjectNames, format)
                _toast.emit(ToastMessage("已导出到下载目录，共 $count 条记录", AppToastType.Success))
            } catch (t: Exception) {
                _toast.emit(ToastMessage("导出失败：${t.message ?: "未知错误"}", AppToastType.Error))
            }
        }
    }

    /** 清除全部用户数据：清空 Room 用户数据表 + 重置全部偏好。 */
    fun clearAllData() {
        viewModelScope.launch {
            scoreRecordRepository.clearAll()
            examRecordRepository.clearAll()
            studyTaskRepository.clearAll()
            targetSchoolRepository.clearAll()
            notificationRepository.clearAll()
            studySessionRepository.clearAll()
            conversationRepository.clearAll()
            userPreferences.clearAllKeepLocation()
            _toast.emit(ToastMessage("已清除全部数据", AppToastType.Success))
        }
    }

    private fun examYearOf(gaokaoTimestamp: Long): String =
        Calendar.getInstance().apply { timeInMillis = gaokaoTimestamp }
            .get(Calendar.YEAR)
            .toString()

    /** 备考天数：从「高考日 − 1 年」到今天，与首页备考天数口径一致。 */
    private fun studyDaysOf(gaokaoTimestamp: Long): Int =
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(gaokaoTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().minusYears(1),
            LocalDate.now(),
        ).toInt().coerceAtLeast(0)

    /** 连续打卡：以有专注记录的连续自然日计数，允许「今天还没学」而从上一天起算。 */
    private fun consecutiveCheckInDays(startedAtMillis: List<Long>): Int {
        if (startedAtMillis.isEmpty()) return 0
        val days = startedAtMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toHashSet()
        val today = LocalDate.now()
        var cursor = if (today in days) today else today.minusDays(1)
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private data class MineStats(
        val examCount: Int,
        val continuousCheckIn: Int,
        val selectedSubjects: List<String>,
        val province: String,
        val nickname: String,
        val avatarPath: String?,
    )
}
