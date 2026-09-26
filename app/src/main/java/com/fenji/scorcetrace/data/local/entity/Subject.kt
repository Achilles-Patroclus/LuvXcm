package com.fenji.scorcetrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 科目：语文、数学、英语等。
 * color 以 ARGB 的 Long 值保存，便于 UI 直接还原成 Compose Color。
 */
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val color: Long,
)
