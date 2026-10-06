package com.fenji.scoretrace.util

import android.util.Log

/**
 * 统一的业务日志出口，tag 固定为 [TAG]，便于自动化测试用
 * `adb logcat -s ScoreTrace` 过滤关键链路。
 */
object AppLogger {

    const val TAG = "ScoreTrace"

    fun d(event: String, detail: String? = null) = Log.d(TAG, format(event, detail))

    fun i(event: String, detail: String? = null) = Log.i(TAG, format(event, detail))

    fun w(event: String, detail: String? = null) = Log.w(TAG, format(event, detail))

    fun e(event: String, detail: String? = null, throwable: Throwable? = null) =
        Log.e(TAG, format(event, detail), throwable)

    private fun format(event: String, detail: String?): String =
        if (detail.isNullOrBlank()) "[$event]" else "[$event] $detail"
}
