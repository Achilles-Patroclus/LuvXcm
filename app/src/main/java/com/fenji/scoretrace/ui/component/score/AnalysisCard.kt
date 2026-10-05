package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.component.RadarChart
import com.fenji.scoretrace.ui.component.SubjectScore
import com.fenji.scoretrace.ui.screen.score.AnalysisTab
import com.fenji.scoretrace.ui.screen.score.SubjectTrend
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 各科分析卡：雷达图 / 单科趋势 / 强弱科三个 Tab。 */
@Composable
fun AnalysisCard(
    selectedTab: AnalysisTab,
    onTabChange: (AnalysisTab) -> Unit,
    subjectRates: List<SubjectScore>,
    subjectTrend: SubjectTrend,
    /** 最近一次考试总分；null 时强弱科建议不显示提升估算 */
    currentTotalScore: Int?,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "各科分析",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Tab 切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ScoreTraceColors.PageBackgroundLight)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnalysisTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onTabChange(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            AnalysisTab.Radar -> RadarTabContent(subjectRates)
            AnalysisTab.Trend -> TrendTabContent(selectedSubject, onSubjectChange, subjectRates, subjectTrend)
            AnalysisTab.StrongWeak -> StrongWeakTabContent(subjectRates, currentTotalScore)
        }
    }
}

// ── 雷达图 Tab ──
@Composable
private fun RadarTabContent(subjectRates: List<SubjectScore>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadarChart(
            labels = subjectRates.map { it.name },
            values = subjectRates.map { it.rate },
            modifier = Modifier.size(150.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            subjectRates.forEach { subject ->
                SubjectProgressRow(subject)
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(subject: SubjectScore) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = subject.name,
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.width(32.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ScoreTraceColors.CardBorderLight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(subject.rate.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(subject.color),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(subject.rate * 100).toInt()}%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = subject.color,
            modifier = Modifier.width(36.dp),
        )
    }
}

// ── 单科趋势 Tab ──
@Composable
private fun TrendTabContent(
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    subjectRates: List<SubjectScore>,
    subjectTrend: SubjectTrend,
) {
    Column {
        // 科目选择胶囊行（横向可滑动，避免窄屏溢出）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            subjectRates.forEach { subject ->
                val selected = subject.name == selectedSubject
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) {
                                ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f)
                            } else {
                                ScoreTraceColors.PageBackgroundLight
                            }
                        )
                        .clickable { onSubjectChange(subject.name) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subject.name,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subjectTrend.points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无该科目成绩", fontSize = 14.sp, color = ScoreTraceColors.TextSecondaryLight)
            }
            return
        }

        val fullScore = subjectTrend.fullScore.coerceAtLeast(1)
        LineChart(
            points = subjectTrend.points,
            targetScore = fullScore,
            yMin = (fullScore * 0.4f).toInt(),
            yMax = fullScore,
            modifier = Modifier.height(160.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "最新 ${subjectTrend.latest ?: 0} / $fullScore",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.BrandPrimary,
            )
        }
    }
}

// ── 强弱科 Tab ──
@Composable
private fun StrongWeakTabContent(subjectRates: List<SubjectScore>, currentTotalScore: Int?) {
    // 至少一科有成绩才算有数据，否则六科 rate 全为 0，强弱科与建议都无意义
    val hasData = subjectRates.any { it.rate > 0.01f }
    if (!hasData) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_bar_chart),
                    contentDescription = null,
                    tint = ScoreTraceColors.TextTertiaryLight,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "暂无成绩数据",
                    fontSize = 14.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "录入成绩后自动分析强弱科",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            }
        }
        return
    }

    val sorted = subjectRates.sortedByDescending { it.rate }
    val strong = sorted.filter { it.rate >= 0.75f }
    val weak = sorted.filter { it.rate < 0.75f }.sortedBy { it.rate }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SubjectGroupColumn(
                title = "强势科目",
                subjects = strong,
                accent = ScoreTraceColors.SuccessGreen,
                modifier = Modifier.weight(1f),
            )
            SubjectGroupColumn(
                title = "待补科目",
                subjects = weak,
                accent = ScoreTraceColors.WarningOrange,
                modifier = Modifier.weight(1f),
            )
        }

        if (weak.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val weakNames = weak.take(2).joinToString(" → ") { it.name }
            Text(
                text = if (currentTotalScore != null) {
                    "补弱优先级：$weakNames。预计两科各提 15 分，总分可达 ${currentTotalScore + 30}。"
                } else {
                    "补弱优先级：$weakNames。"
                },
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun SubjectGroupColumn(
    title: String,
    subjects: List<SubjectScore>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(14.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accent)
        Spacer(modifier = Modifier.height(8.dp))
        if (subjects.isEmpty()) {
            Text("—", fontSize = 14.sp, color = ScoreTraceColors.TextSecondaryLight)
        } else {
            subjects.forEach { subject ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScoreTraceColors.TextPrimaryLight,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(subject.rate * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
