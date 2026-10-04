package com.fenji.scoretrace.data.remote.deepseek.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
) {
    data class Choice(
        val index: Int = 0,
        val message: ChatRequest.Message? = null,
        @SerializedName("finish_reason")
        val finishReason: String? = null,
    )

    data class Usage(
        @SerializedName("prompt_tokens")
        val promptTokens: Int = 0,
        @SerializedName("completion_tokens")
        val completionTokens: Int = 0,
        @SerializedName("total_tokens")
        val totalTokens: Int = 0,
    )
}
