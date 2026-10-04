package com.fenji.scoretrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 通知中心的持久化记录。
 * [type] 取 study / score / ai / system 之一，决定列表左侧图标的颜色与图形。
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
)
