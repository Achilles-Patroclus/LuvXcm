package com.fenji.scoretrace.ui.screen.ai

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.remote.deepseek.dto.ChatRequest
import com.fenji.scoretrace.data.repository.DeepSeekRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** AI 助手页面的输入区状态 */
data class AiUiState(
    /** 输入框文本 */
    val inputText: String = "",
    /** 当前选中的快捷问题（点击后填充到输入框） */
    val selectedQuickAction: String? = null,
)

/** 一条对话消息 */
data class ChatMessage(
    val id: Long,
    val role: String,  // "user" / "assistant"
    val content: String,
    val isStreaming: Boolean = false,
)

/** 快捷入口定义 */
data class AiQuickAction(
    val id: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val iconTint: Color,
    /** 点击后填充到输入框的预设问题 */
    val presetQuestion: String,
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: DeepSeekRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var nextMessageId = 0L
    private var streamJob: Job? = null

    /** 六个快捷入口 */
    val quickActions: List<AiQuickAction> = listOf(
        AiQuickAction(
            id = "weakness",
            title = "分析薄弱点",
            subtitle = "找出最拉分的科目与章节",
            iconRes = R.drawable.ic_person_search,
            iconTint = Color(0xFF3B82F6),
            presetQuestion = "帮我分析当前各科的薄弱点，找出最容易提分的章节",
        ),
        AiQuickAction(
            id = "plan",
            title = "生成学习计划",
            subtitle = "按剩余247天排冲刺节奏",
            iconRes = R.drawable.ic_calendar_month,
            iconTint = Color(0xFF10B981),
            presetQuestion = "根据我的目标院校和剩余时间，生成一份冲刺学习计划",
        ),
        AiQuickAction(
            id = "explain",
            title = "讲解知识点",
            subtitle = "把难点讲到你真的听懂",
            iconRes = R.drawable.ic_book,
            iconTint = Color(0xFFF59E0B),
            presetQuestion = "用通俗易懂的方式讲解一个知识点（请告诉我科目和主题）",
        ),
        AiQuickAction(
            id = "essay",
            title = "作文模板",
            subtitle = "高分议论文结构与素材",
            iconRes = R.drawable.ic_edit_note,
            iconTint = Color(0xFF8B5CF6),
            presetQuestion = "给我一个高考语文高分议论文的写作结构和万能素材",
        ),
        AiQuickAction(
            id = "analysis",
            title = "成绩分析",
            subtitle = "解读最近一次考试变化",
            iconRes = R.drawable.ic_show_chart,
            iconTint = Color(0xFFEF4444),
            presetQuestion = "分析我最近一次考试的成绩变化和提分方向",
        ),
        AiQuickAction(
            id = "sprint",
            title = "冲刺策略",
            subtitle = "最后阶段的提分优先级",
            iconRes = R.drawable.ic_rocket_launch,
            iconTint = Color(0xFF0EA5E9),
            presetQuestion = "距离高考还有200多天，最后的冲刺策略应该怎么安排？",
        ),
    )

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /** 点击快捷入口：将预设问题填充到输入框 */
    fun onQuickActionClick(action: AiQuickAction) {
        _uiState.value = _uiState.value.copy(
            inputText = action.presetQuestion,
            selectedQuickAction = action.id,
        )
    }

    /** 发送输入框中的消息 */
    fun onSend() {
        sendMessage(_uiState.value.inputText)
    }

    /** 发送一条消息：追加用户消息 + 空的 AI 消息，随后流式填充。 */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        val userMessage = ChatMessage(id = nextMessageId++, role = "user", content = trimmed)
        val assistantId = nextMessageId++
        val assistantMessage = ChatMessage(
            id = assistantId,
            role = "assistant",
            content = "",
            isStreaming = true,
        )
        _messages.value = _messages.value + userMessage + assistantMessage
        _uiState.value = AiUiState()
        _isLoading.value = true
        _error.value = null

        // 历史消息（不含正在流式填充的空 AI 消息，也不含系统提示词——仓库会补）
        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatRequest.Message(role = it.role, content = it.content) }

        streamJob = viewModelScope.launch {
            try {
                repository.chatStream(history).collect { chunk ->
                    if (chunk.isEmpty()) {
                        finishStreaming(assistantId)
                    } else {
                        appendToMessage(assistantId, chunk)
                    }
                }
                // 兜底：流正常结束但未收到 [DONE] 时确保收尾
                finishStreaming(assistantId)
            } catch (e: Exception) {
                _error.value = e.message ?: "请求失败，请重试"
                _messages.value = _messages.value.map {
                    if (it.id == assistantId) {
                        it.copy(
                            content = if (it.content.isEmpty()) "[请求失败] ${e.message}" else it.content,
                            isStreaming = false,
                        )
                    } else {
                        it
                    }
                }
                _isLoading.value = false
            }
        }
    }

    /** 重试最后一条：丢弃该条用户消息之后的全部内容并重新发送。 */
    fun retryLast() {
        if (_isLoading.value) return
        val lastUserIndex = _messages.value.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        val lastUser = _messages.value[lastUserIndex]
        _messages.value = _messages.value.take(lastUserIndex)
        sendMessage(lastUser.content)
    }

    /** 新建对话：清空消息并中断进行中的流式请求。 */
    fun onNewChat() {
        streamJob?.cancel()
        streamJob = null
        _messages.value = emptyList()
        _error.value = null
        _isLoading.value = false
        _uiState.value = AiUiState()
    }

    /** 点击加号（上传/拍照）：当前无操作，待接入拍照识别成绩单 */
    fun onAddClick() {
    }

    /** 历史记录 */
    fun onHistoryClick() {
    }

    private fun appendToMessage(id: Long, chunk: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(content = it.content + chunk) else it
        }
    }

    private fun finishStreaming(id: Long) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(isStreaming = false) else it
        }
        _isLoading.value = false
    }
}
