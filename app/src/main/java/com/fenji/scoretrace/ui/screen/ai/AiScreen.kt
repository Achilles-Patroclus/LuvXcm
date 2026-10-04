package com.fenji.scoretrace.ui.screen.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.ui.component.ai.AiInputBar
import com.fenji.scoretrace.ui.component.ai.AiQuickActionGrid
import com.fenji.scoretrace.ui.component.ai.AiTopBar
import com.fenji.scoretrace.ui.component.ai.AiWelcomeHero
import com.fenji.scoretrace.ui.component.ai.ChatBubble
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * AI 助手页面。
 * 顶部标题栏与底部输入栏固定：无消息时中间为欢迎区 + 快捷入口；有消息时中间为对话列表（流式追加）。
 */
@Composable
fun AiScreen(
    viewModel: AiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AiTopBar(
                onNewChat = viewModel::onNewChat,
                onHistoryClick = viewModel::onHistoryClick,
            )

            if (messages.isEmpty()) {
                WelcomeContent(
                    actions = viewModel.quickActions,
                    onActionClick = viewModel::onQuickActionClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ChatList(
                    messages = messages,
                    error = error,
                    modifier = Modifier.weight(1f),
                )
            }

            AiInputBar(
                inputText = state.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::onSend,
                onAddClick = viewModel::onAddClick,
                isLoading = isLoading,
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

/** 对话态：消息气泡列表，新消息自动滚动到底部。 */
@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    error: String?,
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

        if (error != null) {
            item {
                Text(
                    text = error,
                    color = ScoreTraceColors.ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
