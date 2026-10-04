package com.fenji.scoretrace.util

/**
 * 赋分制换算预留接口。
 * 目前直接返回原始分；后续接入等级赋分算法时只改这里，调用方不用动。
 * @param rawScore 原始卷面分
 * @param fullScore 该科满分
 */
object ScoreConverter {
    fun calculateConvertedScore(
        rawScore: Double,
        fullScore: Double = Constants.DEFAULT_FULL_SCORE,
    ): Double = rawScore
}
