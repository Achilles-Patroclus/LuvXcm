package com.fenji.scorcetrace.ui.screen.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.entity.StudyTask
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.data.repository.StudyTaskRepository
import com.fenji.scorcetrace.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class PlanUiState(
    val subjects: List<Subject> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val selectedSubjectId: Long? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val studyTaskRepository: StudyTaskRepository,
) : ViewModel() {

    private val selectedSubjectId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PlanUiState> = selectedSubjectId
        .flatMapLatest { subjectId ->
            combine(
                subjectRepository.observeSubjects(),
                studyTaskRepository.observeTasks(subjectId),
            ) { subjects, tasks ->
                PlanUiState(
                    subjects = subjects,
                    tasks = tasks,
                    selectedSubjectId = subjectId,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanUiState(),
        )

    fun selectSubject(subjectId: Long?) {
        selectedSubjectId.value = subjectId
    }

    fun addTask(subjectId: Long, title: String, content: String, dueDate: Date?) {
        viewModelScope.launch { studyTaskRepository.addTask(subjectId, title, content, dueDate) }
    }

    fun toggleTask(task: StudyTask) {
        viewModelScope.launch { studyTaskRepository.setCompleted(task.id, !task.isCompleted) }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch { studyTaskRepository.deleteTask(task) }
    }
}
