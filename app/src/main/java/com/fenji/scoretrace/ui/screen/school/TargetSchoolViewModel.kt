package com.fenji.scoretrace.ui.screen.school

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.entity.TargetSchool
import com.fenji.scoretrace.data.model.Major
import com.fenji.scoretrace.data.model.MajorTreeData
import com.fenji.scoretrace.data.model.SchoolInfo
import com.fenji.scoretrace.data.repository.MajorRepository
import com.fenji.scoretrace.data.repository.SchoolRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TargetSchoolUiState(
    val schools: List<SchoolInfo> = emptyList(),
    val filteredSchools: List<SchoolInfo> = emptyList(),
    val majorTree: MajorTreeData? = null,
    val selectedSchoolId: String? = null,
    val searchQuery: String = "",
    /** 目标分数：选中院校时取该校录取最低分；选中专业后按专业热门度上浮，只读不可编辑 */
    val targetScore: Int = 0,
    /** 目标专业：单选；null 表示未选，此时目标分数为院校基准分 */
    val selectedMajor: Major? = null,
    /** 用户选科（3+1+2），来自用户偏好，用于标题栏展示 */
    val selectedSubjects: List<String> = emptyList(),
    /** 当前总分：各科最近一次成绩之和，用于提示「还差多少分」 */
    val currentScore: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class TargetSchoolViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val majorRepository: MajorRepository,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TargetSchoolUiState())
    val uiState: StateFlow<TargetSchoolUiState> = _uiState.asStateFlow()

    /** 已保存的目标院校记录：保存时沿用其 id 与年份，保证始终只有一条目标 */
    private var savedTarget: TargetSchool? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val schoolData = schoolRepository.loadSchools()
        val majorTree = majorRepository.loadMajorTree()
        val target = targetSchoolRepository.observeLatest().first()
        val subjects = userPreferences.selectedSubjects.first()
        savedTarget = target
        val selectedSchool = target?.let { saved -> schoolData.schools.find { it.name == saved.schoolName } }
        _uiState.value = TargetSchoolUiState(
            schools = schoolData.schools,
            filteredSchools = schoolData.schools,
            majorTree = majorTree,
            selectedSchoolId = selectedSchool?.id,
            targetScore = target?.targetScore ?: 0,
            selectedMajor = findSavedMajor(target?.majorName, majorTree),
            selectedSubjects = subjects,
            currentScore = target?.currentScore?.takeIf { it > 0 } ?: currentTotalScore(),
            isLoading = false,
        )
    }

    /** 兼容旧数据：新格式为「080901 计算机科学与技术」，旧格式可能是「计算机、软件」等多选串。 */
    private fun findSavedMajor(stored: String?, tree: MajorTreeData): Major? {
        if (stored.isNullOrBlank()) return null
        val first = stored.substringBefore(MAJOR_SEPARATOR).trim()
        if (first.isBlank()) return null
        val code = first.substringBefore(' ').trim()
        val all = tree.categories.flatMap { it.subCategories }.flatMap { it.majors }
        return all.firstOrNull { it.code == code }
            ?: all.firstOrNull { it.name == first || first.endsWith(it.name) }
    }

    /** 当前总分：每科取最近一次成绩再求和（与首页/成绩页口径一致）。 */
    private suspend fun currentTotalScore(): Int = scoreRecordRepository
        .observeRecent(CURRENT_SCORE_LIMIT)
        .first()
        .groupBy { it.subjectId }
        .values
        .sumOf { records -> records.maxByOrNull { it.examDate }?.score ?: 0.0 }
        .toInt()

    /** 全量院校过滤放到协程里做，避免输入时阻塞主线程。 */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val schools = _uiState.value.schools
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val filtered = filterSchools(schools, query)
            _uiState.value = _uiState.value.copy(filteredSchools = filtered)
        }
    }

    fun onSchoolSelect(school: SchoolInfo) {
        _uiState.value = _uiState.value.copy(
            selectedSchoolId = school.id,
            // 目标分数自动取该校录取最低分（只读，不允许手动输入）
            targetScore = school.targetScore,
            selectedMajor = null,
        )
    }

    /** 专业单选：再点已选专业则取消，回到院校基准分；否则目标分数按该专业热门度上浮。 */
    fun onMajorSelect(major: Major) {
        val state = _uiState.value
        val school = state.schools.find { it.id == state.selectedSchoolId }
        if (state.selectedMajor?.code == major.code) {
            _uiState.value = state.copy(
                selectedMajor = null,
                targetScore = school?.targetScore ?: 0,
            )
            return
        }
        val score = if (school != null) {
            (school.targetScore + majorScoreBonus(major.name)).coerceAtMost(TOTAL_SCORE)
        } else {
            0
        }
        _uiState.value = state.copy(selectedMajor = major, targetScore = score)
    }

    fun onReset() {
        _uiState.value = _uiState.value.copy(
            selectedSchoolId = null,
            targetScore = 0,
            selectedMajor = null,
            searchQuery = "",
            filteredSchools = _uiState.value.schools,
        )
    }

    /** 保存后回调，确保写入完成再退出页面。 */
    fun onSave(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val school = state.schools.find { it.id == state.selectedSchoolId }
            if (school != null) {
                val year = savedTarget?.year ?: Calendar.getInstance().get(Calendar.YEAR)
                targetSchoolRepository.save(
                    schoolName = school.name,
                    majorName = state.selectedMajor?.display.orEmpty(),
                    targetScore = state.targetScore,
                    currentScore = state.currentScore,
                    year = year,
                    id = savedTarget?.id ?: 0L,
                )
                savedTarget = targetSchoolRepository.observeLatest().first()
            }
            onSaved()
        }
    }

    private fun filterSchools(schools: List<SchoolInfo>, query: String): List<SchoolInfo> {
        if (query.isBlank()) return schools
        return schools.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true) ||
                it.province.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) } ||
                it.type.contains(query, ignoreCase = true)
        }
    }

    /**
     * 专业分数估算：数据源只有院校分，没有分专业录取分，故按专业热门度加档
     * （院校最低分 + 热门系数），并在 750 总分内截断。
     */
    private fun majorScoreBonus(major: String): Int = when {
        major.contains("计算机") || major.contains("软件") || major.contains("人工智能") -> 8
        major.contains("电子") || major.contains("通信") || major.contains("自动化") ||
            major.contains("临床医学") || major.contains("金融") || major.contains("法学") -> 5
        else -> 0
    }

    private companion object {
        const val CURRENT_SCORE_LIMIT = 50
        const val MAJOR_SEPARATOR = "、"
        const val TOTAL_SCORE = 750
    }
}
