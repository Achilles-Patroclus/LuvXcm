package com.fenji.scoretrace.data.remote.deepseek.dto

import com.google.gson.annotations.SerializedName

data class ChatChunk(
    val id: String? = null,
    val choices: List<ChunkChoice> = emptyList(),
) {
    data class ChunkChoice(
        val index: Int = 0,
        val delta: Delta? = null,
        @SerializedName("finish_reason")
        val finishReason: String? = null,
    )

    data class Delta(
        val role: String? = null,
        val content: String? = null,
    )
}
