package com.fenji.scoretrace.ui.screen.score

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.GlmException
import com.fenji.scoretrace.data.repository.GlmVisionRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.ScoreSheetData
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.util.ImageEncoder
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val glmVisionRepository: GlmVisionRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = subjectRepository.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 是否正在识别成绩单图片 */
    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    /** 图片识别的一次性提示（成功/失败），UI 消费后调用 [clearParseHint] */
    private val _parseHint = MutableStateFlow<String?>(null)
    val parseHint: StateFlow<String?> = _parseHint.asStateFlow()

    /** 识别成功的结构化结果，供 UI 回填表单；消费后调用 [clearParsedSheet] */
    private val _parsedSheet = MutableStateFlow<ScoreSheetData?>(null)
    val parsedSheet: StateFlow<ScoreSheetData?> = _parsedSheet.asStateFlow()

    /**
     * 识别成绩单图片：Uri → 压缩为 base64 data URL → 调 GLM-4V-Flash 结构化识别。
     * 成功通过 [parsedSheet] 回填表单；失败用 [parseHint] 提示并保留手动录入。
     */
    fun parseScoreImage(image: Uri) {
        if (_isParsing.value) return
        _isParsing.value = true
        _parseHint.value = null
        viewModelScope.launch {
            val dataUrl = withContext(Dispatchers.IO) {
                ImageEncoder.uriToDataUrl(appContext, image)
            }
            if (dataUrl == null) {
                _isParsing.value = false
                _parseHint.value = "无法读取所选图片，请重新选择或手动录入"
                return@launch
            }
            val result = glmVisionRepository.recognizeScoreSheet(dataUrl)
            _isParsing.value = false
            result
                .onSuccess { _parsedSheet.value = it }
                .onFailure { error -> _parseHint.value = describeError(error) }
        }
    }

    fun clearParseHint() {
        _parseHint.value = null
    }

    fun clearParsedSheet() {
        _parsedSheet.value = null
    }

    /** 把识别错误翻译成用户可读文案：常见码给友好提示，其余带具体错误码。 */
    private fun describeError(error: Throwable): String {
        val code = (error as? GlmException)?.code
        return when {
            code == "1305" -> "识别失败：服务繁忙，请稍后重试，也可手动录入"
            code == "1301" -> "识别失败：图片可能含敏感内容，请更换后重试"
            // JSON 解析类异常统一友好化，不暴露 Gson 的异常类名/堆栈
            error is JsonParseException -> "识别失败：识别结果格式异常，请手动录入"
            else -> {
                val detail = error.message?.takeIf { it.isNotBlank() } ?: "网络异常"
                val prefix = code?.let { "$it - " }.orEmpty()
                "识别失败：$prefix$detail，请手动录入"
            }
        }
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
