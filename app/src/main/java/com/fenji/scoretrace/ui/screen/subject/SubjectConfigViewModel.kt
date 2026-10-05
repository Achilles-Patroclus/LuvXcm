package com.fenji.scoretrace.ui.screen.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 3+1+2 的当前选择：首选 1 门 + 再选最多 2 门。 */
data class SubjectSelection(
    val primary: String? = null,
    val secondary: List<String> = emptyList(),
) {
    /** 选满「1 首选 + 2 再选」才可保存 */
    val isValid: Boolean get() = primary != null && secondary.size == SECONDARY_COUNT

    /** 平铺成存储顺序：首选 + 再选（再选按 [Constants.SECONDARY_SUBJECT_NAMES] 顺序）。 */
    fun toSubjectList(): List<String> =
        listOfNotNull(primary) + Constants.SECONDARY_SUBJECT_NAMES.filter { it in secondary }

    companion object {
        const val SECONDARY_COUNT = 2

        /** 默认选科：物理 + 化学 + 生物（与 UserPreferences 的默认值一致） */
        val DEFAULT = SubjectSelection(primary = "物理", secondary = listOf("化学", "生物"))
    }
}

/**
 * 选科配置页状态。
 *
 * 进入时从 [UserPreferences.selectedSubjects] 读一次当前选科作为编辑起点；此后编辑只在内存中进行，
 * 点「保存选科配置」才写回。
 */
@HiltViewModel
class SubjectConfigViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _selection = MutableStateFlow(SubjectSelection())
    val selection: StateFlow<SubjectSelection> = _selection.asStateFlow()

    init {
        viewModelScope.launch {
            _selection.value = fromStored(userPreferences.selectedSubjects.first())
        }
    }

    /** 首选科目二选一：直接替换。 */
    fun selectPrimary(name: String) {
        _selection.update { it.copy(primary = name) }
    }

    /** 再选科目：已选则取消；未选且未满 2 门才可加入。 */
    fun toggleSecondary(name: String) {
        _selection.update { current ->
            when {
                name in current.secondary -> current.copy(secondary = current.secondary - name)
                current.secondary.size < SubjectSelection.SECONDARY_COUNT ->
                    current.copy(secondary = current.secondary + name)
                else -> current
            }
        }
    }

    /** 重置为默认选科（物理 + 化学 + 生物）。 */
    fun reset() {
        _selection.value = SubjectSelection.DEFAULT
    }

    /** 保存到 [UserPreferences]，成功后回调（用于 toast + 返回）。非法选择不保存。 */
    fun save(onSaved: () -> Unit) {
        val current = _selection.value
        if (!current.isValid) return
        viewModelScope.launch {
            userPreferences.setSelectedSubjects(current.toSubjectList())
            onSaved()
        }
    }

    private fun fromStored(subjects: List<String>): SubjectSelection {
        val primary = Constants.PRIMARY_SUBJECT_NAMES.firstOrNull { it in subjects }
        val secondary = Constants.SECONDARY_SUBJECT_NAMES.filter { it in subjects }
            .take(SubjectSelection.SECONDARY_COUNT)
        return SubjectSelection(primary = primary, secondary = secondary)
    }
}
