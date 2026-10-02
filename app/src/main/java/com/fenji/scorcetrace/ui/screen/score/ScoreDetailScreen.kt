package com.fenji.scorcetrace.ui.screen.score

import androidx.compose.foundation.layout.Arrangement
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
import com.fenji.scorcetrace.ui.component.score.DetailActionButtons
import com.fenji.scorcetrace.ui.component.score.DetailTopBar
import com.fenji.scorcetrace.ui.component.score.ExamOverviewCard
import com.fenji.scorcetrace.ui.component.score.ScoreDistributionCard
import com.fenji.scorcetrace.ui.component.score.SubjectDetailList
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 成绩详情页：顶部栏固定，内容区可滚动。
 *
 * 本页为全屏页，[com.fenji.scorcetrace.ui.navigation.AppNavHost] 在该路由隐藏底部导航栏；
 * 顶部状态栏内边距由 Scaffold 统一处理，底部操作区自行避让手势条。
 */
@Composable
fun ScoreDetailScreen(
    onBack: () -> Unit,
    viewModel: ScoreDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                onBack = onBack,
                onEdit = { /* TODO: 跳转到编辑页 */ },
                onDelete = { viewModel.deleteExam() },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ExamOverviewCard(
                    examName = state.examName,
                    examDate = state.examDate,
                    totalStudents = state.totalStudents,
                    totalScore = state.totalScore,
                    fullScore = state.fullScore,
                    totalRate = state.totalRate,
                    classRank = state.classRank,
                    gradeRank = state.gradeRank,
                    deltaFromLast = state.deltaFromLast,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                SubjectDetailList(
                    subjects = state.subjects,
                    onWrongQuestionsClick = { /* TODO: 跳转到错题页 */ },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                ScoreDistributionCard(
                    segments = state.segments,
                    myScore = state.totalScore,
                    totalClassStudents = state.classStudentCount,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                DetailActionButtons(
                    onAiAnalyze = { /* TODO: 跳转到 AI 分析 */ },
                    onEdit = { /* TODO: 跳转到编辑页 */ },
                    onDelete = { viewModel.deleteExam() },
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
