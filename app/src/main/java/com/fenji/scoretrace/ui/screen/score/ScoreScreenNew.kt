package com.fenji.scoretrace.ui.screen.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.ui.component.EmptyView
import com.fenji.scoretrace.ui.component.LoadingView
import com.fenji.scoretrace.ui.component.score.AiHintBar
import com.fenji.scoretrace.ui.component.score.AnalysisCard
import com.fenji.scoretrace.ui.component.score.HistorySection
import com.fenji.scoretrace.ui.component.score.LatestExamCard
import com.fenji.scoretrace.ui.component.score.ScoreTopBar
import com.fenji.scoretrace.ui.component.score.ScoreFilterSheet
import com.fenji.scoretrace.ui.component.score.TotalTrendCard
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 成绩页：顶部标题栏 + AI 提示条固定，内容区可滚动。
 *
 * 顶/底系统栏的内边距由 [com.fenji.scoretrace.ui.navigation.AppNavHost] 的 Scaffold 统一处理，
 * 本页不再自行避让。
 */
@Composable
fun ScoreScreenNew(
    viewModel: ScoreViewModelNew = hiltViewModel(),
    onOpenAi: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 待确认删除的考试；非空时弹二次确认
    var pendingDelete by remember { mutableStateOf<ExamHistoryItem?>(null) }
    var showFilter by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreTopBar(
                onFilterClick = { showFilter = true },
                filterActive = state.filter.activeCount > 0,
                filterCount = state.filter.activeCount,
            )
            AiHintBar(onClick = onOpenAi)

            when {
                state.isLoading -> LoadingView(modifier = Modifier.weight(1f))

                !state.hasScores -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyView(
                        message = "暂无成绩",
                        subtitle = "记录每一次考试，才看得见进步的曲线",
                        actionLabel = "去录入成绩",
                        onAction = onOpenAi,
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LatestExamCard(
                        examName = state.latestExamName,
                        examDate = state.latestExamDate,
                        totalScore = state.latestTotalScore,
                        fullScore = state.latestFullScore,
                        delta = state.deltaFromLast,
                        classRank = state.classRank,
                        gradeRank = state.gradeRank,
                        totalRate = state.totalRate,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    TotalTrendCard(
                        points = state.trendPoints,
                        targetScore = if (state.filter.subject == null) state.targetScore else null,
                        selectedRange = state.trendRange,
                        onRangeChange = viewModel::setTrendRange,
                        title = state.trendTitle,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    AnalysisCard(
                        selectedTab = state.analysisTab,
                        onTabChange = viewModel::setAnalysisTab,
                        subjectRates = state.subjectRates,
                        subjectTrend = state.subjectTrend,
                        currentTotalScore = state.latestTotalScore,
                        selectedSubject = state.selectedSubject,
                        onSubjectChange = viewModel::setSelectedSubject,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    HistorySection(
                        groups = state.historyGroups,
                        pendingDeleteName = pendingDelete?.examName,
                        onDeleteRequest = { pendingDelete = it },
                        onItemClick = { onOpenDetail(it.id) },
                        onGuideClick = onOpenAi,
                    )
                }
            }
        }
    }

    if (showFilter) {
        ScoreFilterSheet(
            subjects = state.filterSubjects,
            initial = state.filter,
            onApply = {
                viewModel.setFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除成绩") },
            text = { Text("确定删除「${item.examName}」这次考试的全部成绩吗？该操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExam(item.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = ScoreTraceColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}
