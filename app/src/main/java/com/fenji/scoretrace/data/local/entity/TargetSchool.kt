package com.fenji.scoretrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 目标院校及专业：首页用「目标分数 vs 当前分数」直观展示与目标的差距。
 * 高考分数与年份都是整数，直接以 Int 存库，避免展示层再换算。
 */
@Entity(tableName = "target_schools")
data class TargetSchool(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val schoolName: String,
    val majorName: String,
    val targetScore: Int,
    val currentScore: Int,
    val year: Int,
)
