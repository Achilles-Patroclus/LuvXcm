package com.fenji.scorcetrace.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/** 学习任务：归属于某个科目，可标记完成。 */
@Immutable
@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long,
    val title: String,
    val content: String = "",
    val dueDate: Date? = null,
    val isCompleted: Boolean = false,
)
