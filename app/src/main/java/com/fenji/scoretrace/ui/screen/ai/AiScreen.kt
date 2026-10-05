package com.fenji.scoretrace.ui.screen.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.component.ai.AiInputBar
import com.fenji.scoretrace.ui.component.ai.AiQuickActionGrid
import com.fenji.scoretrace.ui.component.ai.AiTopBar
import com.fenji.scoretrace.ui.component.ai.AiWelcomeHero
import com.fenji.scoretrace.ui.component.ai.ChatBubble
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * AI 助手页面。
 * 顶部标题栏与底部输入栏固定：无消息时中间为欢迎区 + 快捷入口；有消息时中间为对话列表（流式追加）。
 *
 * 根布局加 [imePadding] 并在键盘弹出时隐藏底栏（见 AppNavHost），使输入框紧贴键盘、列表自适应缩放。
 * 底部「+」号打开功能菜单：拍照识分 / 从相册选图跳转 AI 录成绩页（后者自动触发选择图片），语音/文件为占位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    onOpenHistory: () -> Unit = {},
    onOpenAiScore: (pickImage: Boolean) -> Unit = {},
    loadConversationId: Long = -1L,
    newChatTick: Long = 0L,
    onCommandConsumed: () -> Unit = {},
    viewModel: AiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val quickActions = viewModel.quickActions
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // 从历史对话页返回时：加载选中的会话 / 开启新对话
    LaunchedEffect(loadConversationId) {
        if (loadConversationId > 0L) {
            viewModel.loadConversation(loadConversationId)
            onCommandConsumed()
        }
    }
    LaunchedEffect(newChatTick) {
        if (newChatTick > 0L) {
            viewModel.onNewChat()
            onCommandConsumed()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AiTopBar(
                isOnline = isOnline,
                onNewChat = viewModel::onNewChat,
                onHistoryClick = onOpenHistory,
            )

            if (messages.isEmpty()) {
                WelcomeContent(
                    actions = quickActions,
                    onActionClick = viewModel::onQuickActionClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ChatList(
                    messages = messages,
                    modifier = Modifier.weight(1f),
                )
            }

            AiInputBar(
                inputText = state.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::onSend,
                onAddClick = { showAddSheet = true },
                isLoading = isLoading,
            )
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = ScoreTraceColors.CardBackgroundLight,
        ) {
            AddMenuContent(
                onPickCamera = {
                    showAddSheet = false
                    onOpenAiScore(false)
                },
                onPickGallery = {
                    showAddSheet = false
                    onOpenAiScore(true)
                },
                onPlaceholder = {
                    showAddSheet = false
                    AppToast.info("即将支持")
                },
            )
        }
    }
}

/** 欢迎态：中央视觉 + 快捷入口六宫格，可滚动。 */
@Composable
private fun WelcomeContent(
    actions: List<AiQuickAction>,
    onActionClick: (AiQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        AiWelcomeHero()

        AiQuickActionGrid(
            actions = actions,
            onActionClick = onActionClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 对话态：消息气泡列表，新消息（含流式增量）自动滚动到底部。 */
@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            // 流式阶段用瞬时滚动避免每段动画抖动；收尾后平滑滚动
            if (messages.last().isStreaming) {
                listState.scrollToItem(messages.lastIndex)
            } else {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            ChatBubble(message = message)
        }
    }
}

/** 「+」号弹出的功能菜单内容：拍照识分 / 从相册选图 / 语音输入 / 上传文件。 */
@Composable
private fun AddMenuContent(
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onPlaceholder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "添加内容",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        AddMenuItem(
            icon = painterResource(R.drawable.ic_photo_camera),
            tint = ScoreTraceColors.BrandPrimary,
            title = "拍照识分",
            subtitle = "拍摄成绩单，AI 自动识别并录入",
            onClick = onPickCamera,
        )
        AddMenuItem(
            icon = painterResource(R.drawable.ic_image),
            tint = ScoreTraceColors.SuccessGreen,
            title = "从相册选图",
            subtitle = "选择已保存的成绩单图片",
            onClick = onPickGallery,
        )
        AddMenuItem(
            icon = painterResource(R.drawable.ic_mic),
            tint = ScoreTraceColors.WarningOrange,
            title = "语音输入",
            subtitle = "说出你的问题，无需打字",
            onClick = onPlaceholder,
        )
        AddMenuItem(
            icon = painterResource(R.drawable.ic_upload),
            tint = ScoreTraceColors.SchoolPurple,
            title = "上传文件",
            subtitle = "导入 PDF / 表格成绩单",
            onClick = onPlaceholder,
        )
    }
}

@Composable
private fun AddMenuItem(
    icon: Painter,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
    }
}
