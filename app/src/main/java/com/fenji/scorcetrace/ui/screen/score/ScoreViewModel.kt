package com.fenji.scorcetrace.ui.screen.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
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

data class ScoreUiState(
    val subjects: List<Subject> = emptyList(),
    val records: List<ScoreRecord> = emptyList(),
    val selectedSubjectId: Long? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScoreViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
) : ViewModel() {

    private val selectedSubjectId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ScoreUiState> = selectedSubjectId
        .flatMapLatest { subjectId ->
            combine(
                subjectRepository.observeSubjects(),
                scoreRecordRepository.observeRecords(subjectId),
            ) { subjects, records ->
                ScoreUiState(
                    subjects = subjects,
                    records = records,
                    selectedSubjectId = subjectId,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScoreUiState(),
        )

    fun selectSubject(subjectId: Long?) {
        selectedSubjectId.value = subjectId
    }

    fun addRecord(
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String? = null,
        secondarySubject: String? = null,
    ) {
        viewModelScope.launch {
            scoreRecordRepository.addRecord(
                subjectId,
                score,
                fullScore,
                examName,
                examDate,
                primarySubject,
                secondarySubject,
            )
        }
    }

    fun deleteRecord(record: ScoreRecord) {
        viewModelScope.launch { scoreRecordRepository.deleteRecord(record) }
    }
}
