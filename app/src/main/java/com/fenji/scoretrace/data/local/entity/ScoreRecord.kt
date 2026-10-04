package com.fenji.scoretrace.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/** 成绩记录：某次考试的得分情况。 */
@Immutable
@Entity(tableName = "score_records")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long,
    val score: Double,
    val fullScore: Double = 150.0,
    val examName: String,
    val examDate: Date = Date(),
    /** 新高考 3+1+2 首选科目（物理/历史）；null 表示未填写。存名称字符串，不建外键。 */
    val primarySubject: String? = null,
    /** 新高考 3+1+2 再选科目（化学/生物/政治/地理）；null 表示未填写。存名称字符串，不建外键。 */
    val secondarySubject: String? = null,
)
