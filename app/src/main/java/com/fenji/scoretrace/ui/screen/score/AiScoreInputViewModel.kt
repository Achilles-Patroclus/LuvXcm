package com.fenji.scoretrace.ui.screen.score

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.remote.glm.dto.ScoreSheetType
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.GlmException
import com.fenji.scoretrace.data.repository.GlmVisionRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.ScoreSheetData
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.VisionResult
import com.fenji.scoretrace.data.repository.VisionStage
import com.fenji.scoretrace.ui.component.score.ScoreFormSubject
import com.fenji.scoretrace.ui.component.score.scoreFormSubjects
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.ImageEncoder
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

/** 一次录入中的单科条目（名称 + 得分 + 满分）。 */
data class ScoreEntry(val name: String, val score: Double, val fullScore: Double)

/** 识别出的科目与用户选科不一致时，供 UI 弹窗展示。 */
data class SubjectMismatch(val recognized: List<String>, val current: List<String>)

/** 识别结果，驱动顶部状态反馈文案。 */
enum class RecognizeOutcome { SUCCESS, FAILED }

@HiltViewModel
class AiScoreInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val notificationRepository: NotificationRepository,
    private val glmVisionRepository: GlmVisionRepository,
    private val userPreferences: UserPreferences,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    /** 表单科目：语数英 + 用户选考三科 */
    val formSubjects: StateFlow<List<ScoreFormSubject>> = userPreferences.selectedSubjects
        .map { scoreFormSubjects(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = scoreFormSubjects(emptyList()),
        )

    /** 分阶段 loading 文案；null 表示不在识别中 */
    private val _loadingStage = MutableStateFlow<String?>(null)
    val loadingStage: StateFlow<String?> = _loadingStage.asStateFlow()

    /** 识别结果（成功/失败）；null 表示尚无结果 */
    private val _recognizeOutcome = MutableStateFlow<RecognizeOutcome?>(null)
    val recognizeOutcome: StateFlow<RecognizeOutcome?> = _recognizeOutcome.asStateFlow()

    /** 图片识别的一次性提示（成功/失败/信息），UI 消费后调用 [clearParseHint] */
    private val _parseHint = MutableStateFlow<String?>(null)
    val parseHint: StateFlow<String?> = _parseHint.asStateFlow()

    /** 识别成功的结构化结果，供 UI 回填表单；消费后调用 [clearParsedSheet] */
    private val _parsedSheet = MutableStateFlow<ScoreSheetData?>(null)
    val parsedSheet: StateFlow<ScoreSheetData?> = _parsedSheet.asStateFlow()

    /** 检测到班级排名表，等待用户输入姓名 */
    private val _pendingNameInput = MutableStateFlow(false)
    val pendingNameInput: StateFlow<Boolean> = _pendingNameInput.asStateFlow()

    /** 待确认的选科不一致（识别科目 vs 当前选科） */
    private val _subjectMismatch = MutableStateFlow<SubjectMismatch?>(null)
    val subjectMismatch: StateFlow<SubjectMismatch?> = _subjectMismatch.asStateFlow()

    /** 自动判断失败/内容审核时置位，请求 UI 弹出类型选择降级 */
    private val _reopenTypeSelector = MutableStateFlow(false)
    val reopenTypeSelector: StateFlow<Boolean> = _reopenTypeSelector.asStateFlow()

    /** 等待姓名输入或降级手选时暂存的图片（base64 data URL） */
    private var pendingImageDataUrl: String? = null

    /** 等待选科确认时暂存的成绩数据 */
    private var pendingScoreData: ScoreSheetData? = null

    fun consumeReopenTypeSelector() {
        _reopenTypeSelector.value = false
    }

    /**
     * 选中图片：压缩 → AI 自动判断类型 → 分派。
     *
     * 个人成绩单直接提取回填；班级排名表转姓名弹窗；判断失败/不确定/内容审核 → 降级为手动选择类型。
     */
    fun onImagePicked(image: Uri) {
        if (_loadingStage.value != null) return
        _parseHint.value = null
        _recognizeOutcome.value = null
        _loadingStage.value = "正在读取图片…"
        viewModelScope.launch {
            val dataUrl = withContext(Dispatchers.IO) {
                ImageEncoder.uriToDataUrl(appContext, image)
            }
            if (dataUrl == null) {
                fail("无法读取所选图片，请重新选择或手动录入")
                return@launch
            }
            pendingImageDataUrl = dataUrl
            _loadingStage.value = "正在判断图片类型…"
            glmVisionRepository.detectSheetType(dataUrl)
                .onSuccess { auto ->
                    AppLogger.i("AiVision", "autoType=$auto userType=null")
                    when (auto) {
                        ScoreSheetType.PERSONAL -> extractWithType(dataUrl, ScoreSheetType.PERSONAL)
                        ScoreSheetType.CLASS_RANKING -> {
                            stopLoading()
                            _pendingNameInput.value = true
                        }
                        ScoreSheetType.OTHER -> fallbackToManualSelection("AI 未能确定图片类型，请手动选择")
                    }
                }
                .onFailure { error ->
                    val code = (error as? GlmException)?.code
                    AppLogger.e("AiVision", "detect failed code=$code", error)
                    val message = when (code) {
                        CODE_CONTENT_REVIEW -> "图片可能含敏感内容，请更换或手动选择类型"
                        CODE_RATE_LIMIT -> "服务繁忙，请稍后重试或手动选择类型"
                        else -> "AI 未能识别图片类型，请手动选择"
                    }
                    fallbackToManualSelection(message)
                }
        }
    }

    /** 用户在降级弹窗中手动选定图片类型。 */
    fun parseWithType(type: ScoreSheetType) {
        val dataUrl = pendingImageDataUrl ?: return
        AppLogger.i("AiVision", "autoType=failed userType=$type")
        _parseHint.value = null
        _recognizeOutcome.value = null
        if (type == ScoreSheetType.CLASS_RANKING) {
            stopLoading()
            _pendingNameInput.value = true
            return
        }
        _loadingStage.value = "正在提取成绩…"
        viewModelScope.launch { extractWithType(dataUrl, type) }
    }

    private suspend fun extractWithType(dataUrl: String, type: ScoreSheetType) {
        if (_loadingStage.value == null) _loadingStage.value = "正在提取成绩…"
        val result = glmVisionRepository.recognizeScoreSheet(dataUrl, type, name = null, onStage = ::onStage)
        handleVisionResult(result, dataUrl)
    }

    /** 用户在姓名弹窗中提交姓名（仅班级排名表场景）。 */
    fun onNameSubmitted(name: String) {
        val dataUrl = pendingImageDataUrl ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _pendingNameInput.value = false
        _parseHint.value = null
        _recognizeOutcome.value = null
        _loadingStage.value = "正在查找 $trimmed 的成绩…"
        viewModelScope.launch {
            val result = glmVisionRepository.recognizeScoreSheet(
                dataUrl,
                ScoreSheetType.CLASS_RANKING,
                name = trimmed,
                onStage = ::onStage,
            )
            handleVisionResult(result, dataUrl)
        }
    }

    fun onCancelNameInput() {
        _pendingNameInput.value = false
        pendingImageDataUrl = null
        stopLoading()
    }

    /** 确认切换选科并回填：把用户选科更新为识别出的选科。 */
    fun onConfirmSubjectSwitch() {
        val data = pendingScoreData ?: return
        val recognized = data.scores.map { it.name }
        val electives = recognized.filterNot { it in REQUIRED_SUBJECTS }
        viewModelScope.launch {
            userPreferences.setSelectedSubjects(electives)
            _parsedSheet.value = data
            _recognizeOutcome.value = RecognizeOutcome.SUCCESS
            _subjectMismatch.value = null
            pendingScoreData = null
            _parseHint.value = "选科配置已同步更新"
        }
    }

    /** 取消切换选科：不回填。 */
    fun onCancelSubjectSwitch() {
        _subjectMismatch.value = null
        pendingScoreData = null
        _parseHint.value = "已取消，未回填数据"
    }

    fun clearParseHint() {
        _parseHint.value = null
    }

    fun clearParsedSheet() {
        _parsedSheet.value = null
    }

    private fun onStage(stage: VisionStage) {
        _loadingStage.value = when (stage) {
            VisionStage.DETECTING -> "正在识别图片类型…"
            VisionStage.EXTRACTING -> "正在提取成绩…"
            VisionStage.SEARCHING -> "正在查找成绩…"
        }
    }

    private fun handleVisionResult(result: Result<VisionResult>, dataUrl: String) {
        result
            .onSuccess { vision ->
                when (vision) {
                    is VisionResult.Personal -> validateSubjects(vision.data)
                    is VisionResult.ClassRanking -> {
                        stopLoading()
                        pendingImageDataUrl = dataUrl
                        _pendingNameInput.value = true
                    }

                    is VisionResult.Error -> {
                        stopLoading()
                        _recognizeOutcome.value = RecognizeOutcome.FAILED
                        _parseHint.value = describeError(GlmException(vision.code, vision.message))
                        // 内容审核（1301）：让用户重新选择类型 / 更换图片
                        if (vision.code == CODE_CONTENT_REVIEW) {
                            _reopenTypeSelector.value = true
                        }
                    }
                }
            }
            .onFailure { error ->
                stopLoading()
                _recognizeOutcome.value = RecognizeOutcome.FAILED
                _parseHint.value = describeError(error)
            }
    }

    /** 校验识别出的科目是否与「语数英 + 用户选科」一致；一致直接回填，否则弹窗确认。 */
    private fun validateSubjects(data: ScoreSheetData) {
        viewModelScope.launch {
            val recognized = data.scores.map { it.name }
            val selected = userPreferences.selectedSubjects.first()
            val expected = REQUIRED_SUBJECTS + selected
            stopLoading()
            if (recognized.toSet() == expected.toSet()) {
                _parsedSheet.value = data
                _recognizeOutcome.value = RecognizeOutcome.SUCCESS
            } else {
                pendingScoreData = data
                _subjectMismatch.value = SubjectMismatch(recognized = recognized, current = selected)
            }
        }
    }

    private fun fallbackToManualSelection(message: String) {
        stopLoading()
        _parseHint.value = message
        _reopenTypeSelector.value = true
    }

    private fun stopLoading() {
        _loadingStage.value = null
    }

    private fun fail(message: String) {
        stopLoading()
        _recognizeOutcome.value = RecognizeOutcome.FAILED
        _parseHint.value = message
    }

    /** 把识别错误翻译成用户可读文案：常见码给友好提示，其余带具体错误码。 */
    private fun describeError(error: Throwable): String {
        val code = (error as? GlmException)?.code
        return when {
            code == CODE_NOT_FOUND -> "未找到该姓名，请检查输入"
            code == "1305" -> "识别失败：服务繁忙，请稍后重试，也可手动录入"
            code == "1301" -> "识别失败：图片可能含敏感内容，请更换后重试"
            error is JsonParseException -> "识别失败：识别结果格式异常，请手动录入"
            else -> {
                val detail = error.message?.takeIf { it.isNotBlank() } ?: "网络异常"
                val prefix = code?.let { "$it - " }.orEmpty()
                "识别失败：$prefix$detail，请手动录入"
            }
        }
    }

    /**
     * 保存一次考试：逐科写成绩记录，写考试排名（含年级排名），并生成一条「新成绩已录入」通知。
     * 写入前先删除同名旧记录，避免重复保存造成同科多行。只写有分的科目。
     */
    fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
        entries: List<ScoreEntry>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            scoreRecordRepository.deleteExamByName(examName)
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
                gradeRank = gradeRank,
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

    private companion object {
        const val CODE_NOT_FOUND = "NOT_FOUND"
        const val CODE_CONTENT_REVIEW = "1301"
        const val CODE_RATE_LIMIT = "1305"
        val REQUIRED_SUBJECTS = listOf("语文", "数学", "英语")
    }
}
