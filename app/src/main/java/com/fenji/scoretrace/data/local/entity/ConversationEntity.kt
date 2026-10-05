package com.fenji.scoretrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一次 AI 对话会话。 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 会话标题（取首条提问，AI 不另生成） */
    val title: String,
    val createdAt: Long,
    /** 最近一次消息时间，用于列表排序 */
    val updatedAt: Long,
)
