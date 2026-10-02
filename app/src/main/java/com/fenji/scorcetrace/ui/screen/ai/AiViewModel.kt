package com.fenji.scorcetrace.ui.screen.ai

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.fenji.scorcetrace.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** AI 助手页面的 UI 状态 */
data class AiUiState(
    /** 输入框文本 */
    val inputText: String = "",
    /** 是否正在等待 AI 响应（欢迎态为 false，后续对话态用） */
    val isLoading: Boolean = false,
    /** 当前选中的快捷问题（点击后填充到输入框） */
    val selectedQuickAction: String? = null,
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
class AiViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

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

    /** 发送消息：欢迎态下清空输入框（实际对话逻辑后续迭代） */
    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedQuickAction = null,
        )
    }

    /** 点击加号（上传/拍照）：当前无操作，待接入拍照识别成绩单 */
    fun onAddClick() {
    }

    /** 新建对话 */
    fun onNewChat() {
        _uiState.value = AiUiState()
    }

    /** 历史记录 */
    fun onHistoryClick() {
    }
}
