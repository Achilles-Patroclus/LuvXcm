package com.fenji.scoretrace.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一次专注计时记录。 */
@Immutable
@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 计时开始的绝对时间戳（ms） */
    val startedAt: Long,
    /** 本次专注时长（秒） */
    val durationSeconds: Int,
    /** 计时类型：stopwatch（正计时）/ countdown（倒计时） */
    val type: String,
)
