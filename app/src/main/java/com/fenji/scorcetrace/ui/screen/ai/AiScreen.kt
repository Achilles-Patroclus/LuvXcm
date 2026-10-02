package com.fenji.scorcetrace.ui.screen.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.ui.component.ai.AiInputBar
import com.fenji.scorcetrace.ui.component.ai.AiQuickActionGrid
import com.fenji.scorcetrace.ui.component.ai.AiTopBar
import com.fenji.scorcetrace.ui.component.ai.AiWelcomeHero
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * AI 助手页面（欢迎态）。
 * 顶部标题栏与底部输入栏固定，中间欢迎区 + 快捷入口六宫格可滚动。
 */
@Composable
fun AiScreen(
    viewModel: AiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AiTopBar(
                onNewChat = viewModel::onNewChat,
                onHistoryClick = viewModel::onHistoryClick,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AiWelcomeHero()

                AiQuickActionGrid(
                    actions = viewModel.quickActions,
                    onActionClick = viewModel::onQuickActionClick,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            AiInputBar(
                inputText = state.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::onSend,
                onAddClick = viewModel::onAddClick,
            )
        }
    }
}
