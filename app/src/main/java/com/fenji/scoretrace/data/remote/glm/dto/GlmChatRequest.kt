package com.fenji.scoretrace.data.remote.glm.dto

import com.google.gson.annotations.SerializedName

/** GLM 多模态对话请求（OpenAI 兼容）。 */
data class GlmChatRequest(
    val model: String = "glm-4v-flash",
    val messages: List<GlmMessage>,
    val temperature: Double = 0.2,
    // glm-4v-flash 的 max_tokens 合法区间为 [1,1024]，超过会返回 400（错误码 1210）
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val stream: Boolean = false,
)

data class GlmMessage(
    val role: String,
    val content: List<GlmContent>,
)

/**
 * 内容块：`type = "text"` 时用 [text]；`type = "image_url"` 时用 [imageUrl]。
 * Gson 会省略为 null 的字段，因此同一结构可安全用于两种类型。
 */
data class GlmContent(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: GlmImageUrl? = null,
)

/** 图片地址：支持公网 URL 或 `data:image/jpeg;base64,...`。 */
data class GlmImageUrl(val url: String)
