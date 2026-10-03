package com.fenji.scorcetrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 考试级记录：某次考试的排名信息。
 * 各科分仍存于 [ScoreRecord]（以 examName 关联），本表只补充分数之外的考试级信息。
 */
@Entity(tableName = "exam_records")
data class ExamRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val examName: String,
    val examDate: Date = Date(),
    /** 班级排名；null 表示未填写 */
    val classRank: Int? = null,
    /** 年级排名；null 表示未填写 */
    val gradeRank: Int? = null,
)
