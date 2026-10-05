package com.fenji.scoretrace.data.remote.glm

import com.fenji.scoretrace.data.remote.glm.dto.GlmChatRequest
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** 智谱 GLM 视觉接口（OpenAI 兼容，Base URL 在 NetworkModule 配置）。 */
interface GlmApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: GlmChatRequest): GlmChatResponse
}
