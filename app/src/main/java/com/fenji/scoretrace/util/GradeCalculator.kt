package com.fenji.scoretrace.util

import java.time.LocalDate

/**
 * 由高考日期推算「届别」与「在读年级」。
 *
 * 学年以每年 9 月 1 日为分界：某届学生的高三学年 = 高考前一年 9 月 ~ 高考年 6 月，
 * 每往前一个学年降一个年级。纯函数，[today] 可注入，便于单元测试。
 *
 * 例：高考日 2027-06-07、今天 2026-10 → 「2027届（高三在读）」。
 */
object GradeCalculator {

    /** 毕业年份（届别）：高考日期所在年，如 2027。 */
    fun graduationYear(gaokaoDate: LocalDate): Int = gaokaoDate.year

    /** 在读年级短语（如「高三在读」）；尚未升入高中或已毕业时返回 null。 */
    fun gradeInProgress(gaokaoDate: LocalDate, today: LocalDate = LocalDate.now()): String? {
        val yearsUntilGrad = graduationYear(gaokaoDate) - academicStartYear(today)
        return when (4 - yearsUntilGrad) {
            1 -> "高一在读"
            2 -> "高二在读"
            3 -> "高三在读"
            else -> null
        }
    }

    /** 「2027届（高三在读）」；无法判定年级时只返回届别，如「2027届」。 */
    fun label(gaokaoDate: LocalDate, today: LocalDate = LocalDate.now()): String {
        val year = graduationYear(gaokaoDate)
        val grade = gradeInProgress(gaokaoDate, today)
        return if (grade == null) "${year}届" else "${year}届（$grade）"
    }

    /** 当前学年起始年：9 月起算当年，1~8 月归上一年。 */
    private fun academicStartYear(today: LocalDate): Int =
        if (today.monthValue >= 9) today.year else today.year - 1
}
