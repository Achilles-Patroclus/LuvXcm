package com.fenji.scoretrace.ui.screen.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.fenji.scoretrace.ui.component.score.AiHintBar
import com.fenji.scoretrace.ui.component.score.AnalysisCard
import com.fenji.scoretrace.ui.component.score.HistorySection
import com.fenji.scoretrace.ui.component.score.LatestExamCard
import com.fenji.scoretrace.ui.component.score.ScoreTopBar
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreTopBar(onFilterClick = { /* TODO: 按考试/科目/时间筛选 */ })
            AiHintBar(onClick = onOpenAi)

            Column(
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
                    targetScore = state.targetScore,
                    selectedRange = state.trendRange,
                    onRangeChange = viewModel::setTrendRange,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                AnalysisCard(
                    selectedTab = state.analysisTab,
                    onTabChange = viewModel::setAnalysisTab,
                    subjectRates = state.subjectRates,
                    selectedSubject = state.selectedSubject,
                    onSubjectChange = viewModel::setSelectedSubject,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                HistorySection(
                    groups = state.historyGroups,
                    onDelete = { viewModel.deleteExam(it.id) },
                    onItemClick = { onOpenDetail(it.id) },
                    onGuideClick = onOpenAi,
                )
            }
        }
    }
}
