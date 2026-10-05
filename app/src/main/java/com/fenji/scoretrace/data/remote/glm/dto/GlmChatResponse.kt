package com.fenji.scoretrace.data.remote.glm.dto

import com.google.gson.annotations.SerializedName

data class GlmChatResponse(
    val choices: List<GlmChoice> = emptyList(),
    val error: GlmError? = null,
)

data class GlmChoice(
    val message: GlmMessageContent? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

data class GlmMessageContent(
    val role: String? = null,
    val content: String? = null,
)

/** 智谱错误体：`{ "error": { "code": "1210", "message": "..." } }` */
data class GlmError(
    val code: String? = null,
    val message: String? = null,
)
