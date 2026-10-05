package com.fenji.scoretrace.ui.screen.ai

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.entity.ExamRecord
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.local.entity.TargetSchool
import com.fenji.scoretrace.data.remote.deepseek.dto.ChatRequest
import com.fenji.scoretrace.data.repository.ConversationRepository
import com.fenji.scoretrace.data.repository.DeepSeekRepository
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.DateUtils
import com.fenji.scoretrace.util.GradeCalculator
import com.fenji.scoretrace.util.aggregateExams
import com.fenji.scoretrace.util.observeOnlineStatus
import com.fenji.scoretrace.util.rankRecordFor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** AI 助手页面的输入区状态 */
data class AiUiState(
    /** 输入框文本 */
    val inputText: String = "",
)

/** 一条对话消息 */
data class ChatMessage(
    val id: Long,
    val role: String,  // "user" / "assistant"
    val content: String,
    val isStreaming: Boolean = false,
)

/** 历史对话列表项 */
data class ConversationItem(
    val id: Long,
    val title: String,
    val updatedAt: Long,
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

/** 单科成绩摘要（供 System Prompt 使用） */
data class SubjectScoreBrief(
    val name: String,
    val score: Int,
)

/** 最近一次考试摘要（供 System Prompt 使用） */
data class ExamSummary(
    val name: String,
    val date: String,
    val totalScore: Int,
    val fullScore: Int,
    val subjects: List<SubjectScoreBrief>,
    val rank: String?,
)

/** 注入 System Prompt 的用户画像 */
data class UserContext(
    val grade: String,
    val province: String,
    val selectedSubjects: List<String>,
    val targetSchool: String?,
    val targetMajor: String?,
    val targetScore: Int?,
    val latestExam: ExamSummary?,
    val weakSubjects: List<String>,
    val hasData: Boolean,
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: DeepSeekRepository,
    private val conversationRepository: ConversationRepository,
    private val userPreferences: UserPreferences,
    private val scoreRecordRepository: ScoreRecordRepository,
    private val examRecordRepository: ExamRecordRepository,
    private val targetSchoolRepository: TargetSchoolRepository,
    private val subjectRepository: SubjectRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 网络在线状态，驱动顶部徽章：绿点「在线」/ 灰点「离线」。 */
    val isOnline: StateFlow<Boolean> = appContext.observeOnlineStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    /** 历史对话列表（按最近消息时间倒序） */
    val conversations: StateFlow<List<ConversationItem>> =
        conversationRepository.observeConversations()
            .map { list -> list.map { ConversationItem(it.id, it.title, it.updatedAt) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var nextMessageId = 0L
    private var streamJob: Job? = null

    /** 当前会话 id；null 表示尚未落库的新对话（首条提问时创建） */
    private var currentConversationId: Long? = null

    /** 六个快捷入口，点击即以预设问题发起对话。 */
    val quickActions: List<AiQuickAction> = buildQuickActions()

    private fun buildQuickActions(): List<AiQuickAction> = listOf(
        AiQuickAction(
            id = "weakness",
            title = "分析薄弱点",
            subtitle = "找出最易提分的科目",
            iconRes = R.drawable.ic_person_search,
            iconTint = Color(0xFF3B82F6),
            presetQuestion = "帮我分析当前各科的薄弱点，找出最容易提分的章节",
        ),
        AiQuickAction(
            id = "plan",
            title = "生成学习计划",
            subtitle = "按剩余天数排冲刺",
            iconRes = R.drawable.ic_calendar_month,
            iconTint = Color(0xFF10B981),
            presetQuestion = "根据我的目标院校和剩余时间，生成一份冲刺学习计划",
        ),
        AiQuickAction(
            id = "explain",
            title = "讲解知识点",
            subtitle = "把难点讲到真懂",
            iconRes = R.drawable.ic_book,
            iconTint = Color(0xFFF59E0B),
            presetQuestion = "用通俗易懂的方式讲解一个知识点（请告诉我科目和主题）",
        ),
        AiQuickAction(
            id = "essay",
            title = "作文模板",
            subtitle = "高分议论文结构",
            iconRes = R.drawable.ic_edit_note,
            iconTint = Color(0xFF8B5CF6),
            presetQuestion = "给我一个高考语文高分议论文的写作结构和万能素材",
        ),
        AiQuickAction(
            id = "analysis",
            title = "成绩分析",
            subtitle = "解读最近考试变化",
            iconRes = R.drawable.ic_show_chart,
            iconTint = Color(0xFFEF4444),
            presetQuestion = "分析我最近一次考试的成绩变化和提分方向",
        ),
        AiQuickAction(
            id = "sprint",
            title = "冲刺策略",
            subtitle = "最后阶段提分优先级",
            iconRes = R.drawable.ic_rocket_launch,
            iconTint = Color(0xFF0EA5E9),
            presetQuestion = "距离高考还有200多天，最后的冲刺策略应该怎么安排？",
        ),
    )

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /** 点击快捷入口：直接以预设问题发起对话。 */
    fun onQuickActionClick(action: AiQuickAction) {
        sendMessage(action.presetQuestion)
    }

    /** 发送输入框中的消息 */
    fun onSend() {
        sendMessage(_uiState.value.inputText)
    }

    /** 发送一条消息：追加用户消息 + 空的 AI 消息，随后流式填充；并自动落库到当前会话。 */
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

        // 历史消息（不含正在流式填充的空 AI 消息）
        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatRequest.Message(role = it.role, content = it.content) }

        streamJob = viewModelScope.launch {
            val conversationId = ensureConversation(trimmed)
            conversationRepository.addMessage(conversationId, "user", trimmed)
            // 发送前取一次用户数据快照，动态构建 System Prompt
            val systemPrompt = buildSystemPrompt(userContextFlow().first())
            try {
                repository.chatStream(history, systemPrompt).collect { chunk ->
                    if (chunk.isEmpty()) {
                        finishStreaming(assistantId)
                    } else {
                        appendToMessage(assistantId, chunk)
                    }
                }
                // 兜底：流正常结束但未收到 [DONE] 时确保收尾
                finishStreaming(assistantId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "请求失败，请重试"
                AppToast.error(message)
                _messages.value = _messages.value.map {
                    if (it.id == assistantId) {
                        it.copy(
                            content = if (it.content.isEmpty()) "[请求失败] $message" else it.content,
                            isStreaming = false,
                        )
                    } else {
                        it
                    }
                }
                _isLoading.value = false
            }
            persistAssistantMessage(conversationId, assistantId)
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
        currentConversationId = null
        _messages.value = emptyList()
        _isLoading.value = false
        _uiState.value = AiUiState()
    }

    /** 载入某个历史会话，恢复聊天上下文。 */
    fun loadConversation(id: Long) {
        streamJob?.cancel()
        streamJob = null
        _isLoading.value = false
        _uiState.value = AiUiState()
        currentConversationId = id
        viewModelScope.launch {
            val rows = conversationRepository.observeMessages(id).first()
            _messages.value = rows.map { ChatMessage(it.id, it.role, it.content, isStreaming = false) }
            nextMessageId = (rows.maxOfOrNull { it.id } ?: 0L) + 1
        }
    }

    /** 删除某个历史会话。 */
    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
            if (currentConversationId == id) {
                currentConversationId = null
                _messages.value = emptyList()
            }
        }
    }

    /** 清空全部历史会话。 */
    fun clearAllConversations() {
        viewModelScope.launch {
            conversationRepository.clearAll()
            currentConversationId = null
            _messages.value = emptyList()
        }
    }

    private suspend fun ensureConversation(titleSeed: String): Long {
        val existing = currentConversationId
        if (existing != null) {
            conversationRepository.touchConversation(existing)
            return existing
        }
        val id = conversationRepository.createConversation(titleSeed.take(20))
        currentConversationId = id
        return id
    }

    private suspend fun persistAssistantMessage(conversationId: Long, assistantId: Long) {
        val content = _messages.value.firstOrNull { it.id == assistantId }?.content.orEmpty()
        if (content.isNotBlank()) {
            conversationRepository.addMessage(conversationId, "assistant", content)
            conversationRepository.touchConversation(conversationId)
        }
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

    // ── 用户画像 → System Prompt ────────────────────────────────

    /** 汇总用户画像的数据源。发送消息时取一次快照（cold flow），不常驻订阅。 */
    private fun userContextFlow(): Flow<UserContext> = combine(
        targetSchoolRepository.observeLatest(),
        scoreRecordRepository.observeRecent(RECENT_SCORE_LIMIT),
        examRecordRepository.observeAll(),
        combine(
            userPreferences.selectedSubjects,
            userPreferences.province,
            userPreferences.gaokaoTimestamp,
            subjectRepository.observeSubjects(),
        ) { selectedSubjects, province, gaokaoTimestamp, subjects ->
            UserBasics(
                selectedSubjects = selectedSubjects,
                province = province,
                gaokaoTimestamp = gaokaoTimestamp,
                subjects = subjects,
            )
        },
    ) { target, scores, exams, basics ->
        buildUserContext(target, scores, exams, basics)
    }

    private fun buildUserContext(
        target: TargetSchool?,
        scores: List<ScoreRecord>,
        exams: List<ExamRecord>,
        basics: UserBasics,
    ): UserContext {
        val idToName = basics.subjects.associate { it.id to it.name }
        val examLog = aggregateExams(scores)
        val latest = examLog.firstOrNull()
        val latestSummary = latest?.let { agg ->
            ExamSummary(
                name = agg.name,
                date = DateUtils.formatDate(agg.date),
                totalScore = agg.totalScore,
                fullScore = agg.fullScore,
                subjects = agg.records.map {
                    SubjectScoreBrief(idToName[it.subjectId] ?: "未知", it.score.toInt())
                },
                rank = rankRecordFor(exams, agg.name)?.classRank?.let { "班级第$it" },
            )
        }
        val weakSubjects = latest?.records
            ?.filter { it.fullScore > 0 }
            ?.sortedBy { it.score / it.fullScore }
            ?.take(2)
            ?.map { idToName[it.subjectId] ?: "未知" }
            .orEmpty()
        val gaokaoDate = Instant.ofEpochMilli(basics.gaokaoTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return UserContext(
            grade = GradeCalculator.label(gaokaoDate),
            province = basics.province,
            selectedSubjects = basics.selectedSubjects,
            targetSchool = target?.schoolName?.takeIf { it.isNotBlank() },
            targetMajor = target?.majorName?.takeIf { it.isNotBlank() },
            targetScore = target?.targetScore,
            latestExam = latestSummary,
            weakSubjects = weakSubjects,
            hasData = scores.isNotEmpty(),
        )
    }

    /** 依据 [ctx] 动态生成 System Prompt，把用户已知信息全部注入，避免反问。 */
    private fun buildSystemPrompt(ctx: UserContext): String {
        val profile = buildString {
            appendLine("- 年级：${ctx.grade}")
            appendLine("- 省份：${ctx.province}")
            appendLine("- 选科：${ctx.selectedSubjects.joinToString("、").ifBlank { "未设置" }}")
            appendLine("- 目标院校：${ctx.targetSchool ?: "未设定"}")
            appendLine("- 目标专业：${ctx.targetMajor ?: "未设定"}")
            append("- 目标分数：${ctx.targetScore?.toString() ?: "未设定"}")
        }
        val examSection = ctx.latestExam?.let { exam ->
            buildString {
                appendLine("## 最近一次考试")
                appendLine("考试名称：${exam.name}（${exam.date}）")
                appendLine("总分：${exam.totalScore}/${exam.fullScore}")
                appendLine("各科：${exam.subjects.joinToString("，") { "${it.name} ${it.score}" }}")
                append("班级排名：${exam.rank ?: "未录入"}")
            }
        } ?: """
            ## 数据状态
            用户尚未录入任何考试成绩。请先友善引导用户录入一次成绩，再基于该成绩给出分析。
            引导语示例："我看你还没有录入考试成绩，建议先记录一次最近的月考成绩，这样我能给你更精准的分析。你可以点右下角 AI 录成绩，支持拍照识别。"
        """.trimIndent()
        val weakSection = ctx.weakSubjects.joinToString("、").ifBlank { "暂无分析" }
        val guidelines = if (ctx.hasData) {
            """
            1. 优先基于以上数据直接分析，**不要反问用户"你几年级/目标是什么/成绩多少"**。
            2. 只有数据确实缺失（如无考试记录）时，才友善提示"建议先录入一次考试成绩"。
            3. 回答要具体、可执行，避免泛泛而谈。
            4. 涉及学科知识时，结合用户选科和薄弱科目给建议。
            5. 保持简洁热情的语气，鼓励为主。
            """.trimIndent()
        } else {
            """
            1. 用户暂无成绩数据，请先友善引导其录入成绩；其他学科问题仍可正常解答。
            2. 回答要具体、可执行，避免泛泛而谈。
            3. 保持简洁热情的语气，鼓励为主。
            """.trimIndent()
        }
        return """
        你是一位经验丰富的高考备考 AI 助手，服务于 ScoreTrace App。

        ## 用户画像（你必须牢记，不要反问用户这些已知信息）
        $profile

        $examSection

        ## 薄弱科目（得分率最低 2 科）
        $weakSection

        ## 回答准则
        $guidelines
        """.trimIndent()
    }

    private data class UserBasics(
        val selectedSubjects: List<String>,
        val province: String,
        val gaokaoTimestamp: Long,
        val subjects: List<Subject>,
    )

    private companion object {
        const val RECENT_SCORE_LIMIT = 200
    }
}
