package com.fenji.scoretrace.data.remote.deepseek

import com.fenji.scoretrace.data.remote.deepseek.dto.ChatRequest
import com.fenji.scoretrace.data.remote.deepseek.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** DeepSeek OpenAI 兼容接口（非流式；流式在 Repository 层用 OkHttp 手动解析 SSE）。 */
interface DeepSeekApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): ChatResponse
}
