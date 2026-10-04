package com.fenji.scoretrace.data.remote.deepseek.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    val stream: Boolean = false,
) {
    data class Message(
        val role: String,  // "system" / "user" / "assistant"
        val content: String,
    )
}
