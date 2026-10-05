package com.fenji.scoretrace.ui.screen.score

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** 一次录入中的单科条目（名称 + 得分 + 满分）。 */
data class ScoreEntry(val name: String, val score: Double, val fullScore: Double)

@HiltViewModel
class AiScoreInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = subjectRepository.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 是否正在识别成绩单图片 */
    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    /** 图片识别的一次性提示，UI 消费后调用 [clearParseHint] */
    private val _parseHint = MutableStateFlow<String?>(null)
    val parseHint: StateFlow<String?> = _parseHint.asStateFlow()

    /**
     * 解析成绩单图片。
     *
     * 当前 DeepSeek 仅提供文本模型（deepseek-chat / deepseek-reasoner），无视觉能力，
     * 无法从图片识别分数；此处友好降级——不生成任何假数据，仅提示用户手动录入。
     */
    fun parseScoreImage(image: Uri) {
        if (_isParsing.value) return
        _isParsing.value = true
        viewModelScope.launch {
            _isParsing.value = false
            _parseHint.value = "当前版本暂不支持自动识别成绩单，请手动录入各科分数"
        }
    }

    fun clearParseHint() {
        _parseHint.value = null
    }

    /**
     * 保存一次考试：逐科写成绩记录，写考试排名，并生成一条「新成绩已录入」通知。
     * 只写有分的科目；总分由各科分实时求和（表单已即时校验）。
     */
    fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        entries: List<ScoreEntry>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val idByName = subjectRepository.observeSubjects().first().associate { it.name to it.id }
            var total = 0.0
            entries.forEach { entry ->
                val subjectId = idByName[entry.name] ?: return@forEach
                if (entry.score > 0) {
                    scoreRecordRepository.addRecord(
                        subjectId = subjectId,
                        score = entry.score,
                        fullScore = entry.fullScore,
                        examName = examName,
                        examDate = examDate,
                    )
                    total += entry.score
                }
            }
            examRecordRepository.save(
                examName = examName,
                examDate = examDate,
                classRank = classRank,
                gradeRank = null,
            )
            val rankText = classRank?.let { " · 班级第$it" }.orEmpty()
            notificationRepository.add(
                type = NotificationType.SCORE,
                title = "新成绩已录入",
                content = "$examName · 总分 ${total.toInt()}$rankText",
            )
            onSaved()
        }
    }
}
