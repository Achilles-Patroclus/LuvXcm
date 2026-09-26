package com.fenji.scorcetrace.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private fun dateFormat() = SimpleDateFormat(Constants.DATE_PATTERN, Locale.getDefault())

    private fun dateTimeFormat() = SimpleDateFormat(Constants.DATE_TIME_PATTERN, Locale.getDefault())

    fun formatDate(date: Date?): String = date?.let { dateFormat().format(it) }.orEmpty()

    fun formatDateTime(date: Date?): String = date?.let { dateTimeFormat().format(it) }.orEmpty()

    fun parseDate(text: String): Date? = runCatching { dateFormat().parse(text) }.getOrNull()

    fun startOfDay(date: Date = Date()): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    fun endOfDay(date: Date = Date()): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.time

    fun isToday(date: Date?): Boolean =
        date != null && startOfDay(date) == startOfDay()

    /** 默认高考日期：今年 6 月 7 日 0 点；若该时刻已过，则顺延到明年 6 月 7 日 0 点 */
    fun defaultGaokaoTimestamp(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            clear()
            set(now.get(Calendar.YEAR), Constants.GAOKAO_MONTH, Constants.GAOKAO_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= System.currentTimeMillis()) {
            target.add(Calendar.YEAR, 1)
        }
        return target.timeInMillis
    }

    /** Material3 DatePicker 的 selectedDateMillis 是所选日期在 UTC 时区的 0 点，换算成本地时区当天 0 点 */
    fun localMidnightFromUtcDate(utcDateMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcDateMillis
        }
        return localMidnight(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** 本地时间戳 -> DatePicker 需要的 UTC 当天 0 点毫秒值 */
    fun utcDateFromLocalTimestamp(timestamp: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = timestamp }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0, 0, 0,
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun localMidnight(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
