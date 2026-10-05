package com.fenji.scoretrace.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一条对话消息，归属某个会话。 */
@Entity(
    tableName = "chat_messages",
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    /** "user" / "assistant" */
    val role: String,
    val content: String,
    val timestamp: Long,
)
