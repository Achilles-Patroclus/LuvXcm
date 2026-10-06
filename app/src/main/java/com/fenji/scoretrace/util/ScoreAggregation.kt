package com.fenji.scoretrace.util

import com.fenji.scoretrace.data.local.entity.ExamRecord
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import java.util.Date

/**
 * 一次考试：把同一次考试的各科成绩聚合到一起。
 *
 * 分组键是 `examName`——与首页 [com.fenji.scoretrace.ui.screen.home.HomeViewModel.summarizeScores]
 * 保持同一口径，避免成绩页与首页对「同一次考试」的界定出现分歧。
 */
data class ExamAggregate(
    val name: String,
    val date: Date,
    val totalScore: Int,
    val fullScore: Int,
    val records: List<ScoreRecord>,
)

/** 把单科成绩按考试名聚合为考试记录，按考试日期倒序（最新在前）。 */
fun aggregateExams(scores: List<ScoreRecord>): List<ExamAggregate> =
    scores.groupBy { it.examName }
        .map { (name, records) ->
            // 同一次考试同一科只应有一条：历史脏数据（重复保存）按 id 去重，保留最新一条，
            // 否则总分与各科明细都会重复累加。
            val deduped = records
                .groupBy { it.subjectId }
                .map { (_, rows) -> rows.maxBy { it.id } }
            ExamAggregate(
                name = name,
                date = deduped.maxOf { it.examDate },
                totalScore = deduped.sumOf { it.score }.toInt(),
                fullScore = deduped.sumOf { it.fullScore }.toInt(),
                records = deduped,
            )
        }
        .sortedByDescending { it.date }

/**
 * 取某次考试的排名记录：按 examName 匹配；同名多行时取日期最近的一条。
 * [ExamRecord] 表没有唯一约束，重复保存同名考试会留下多行，这里做一次收敛。
 */
fun rankRecordFor(exams: List<ExamRecord>, examName: String): ExamRecord? =
    exams.filter { it.examName == examName }.maxByOrNull { it.examDate }

/**
 * 考试名 → 稳定 Long id，用于导航路由（[com.fenji.scoretrace.ui.navigation.Screen.ScoreDetail] 仍是 Long 参数）。
 * 同一名称始终得到同一值；详情页据此反查 examName。
 */
fun examNameToId(examName: String): Long = examName.hashCode().toLong()
