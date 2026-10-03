package com.fenji.scorcetrace.data.repository

import com.fenji.scorcetrace.data.remote.deepseek.DeepSeekApiService
import com.fenji.scorcetrace.data.remote.deepseek.dto.ChatChunk
import com.fenji.scorcetrace.data.remote.deepseek.dto.ChatRequest
import com.fenji.scorcetrace.data.remote.deepseek.dto.ChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * DeepSeek 对话仓库。
 * 非流式走 Retrofit（[chat]），流式走 OkHttp 手动解析 SSE（[chatStream]）。
 */
@Singleton
class DeepSeekRepository @Inject constructor(
    private val apiService: DeepSeekApiService,
    @param:Named("deepseek") private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {

    /** 系统提示词：定义 AI 助手的高考辅导角色与回答风格。 */
    private val systemPrompt = """
        你是 ScoreTrace 高考备考助手，一位经验丰富的高中全科辅导老师。
        你的任务是帮助高三学生备考高考，涵盖语文、数学、英语、物理、化学、生物、政治、历史、地理。
        回答要求：
        1. 简洁明了，重点突出，避免冗长
        2. 解题时给出步骤和思路，不只是答案
        3. 用学生能理解的语言，避免过于学术化
        4. 涉及具体题目时，先分析考点再解答
        5. 鼓励性语气，帮助学生建立信心
        6. 如果问题不明确，主动询问更多信息
    """.trimIndent()

    /** 非流式对话（一次性返回完整结果）。 */
    suspend fun chat(messages: List<ChatRequest.Message>): Result<ChatResponse> {
        return try {
            val response = apiService.chatCompletion(
                ChatRequest(messages = withSystemPrompt(messages), stream = false),
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 流式对话（SSE），每个 emit 是一段增量文本；emit 空字符串表示结束。
     */
    fun chatStream(messages: List<ChatRequest.Message>): Flow<String> = flow {
        val requestBody = gson.toJson(
            ChatRequest(messages = withSystemPrompt(messages), stream = true),
        )
            .toRequestBody("application/json".toMediaType())

        // Authorization / Content-Type 由 @Named("deepseek") 客户端的拦截器统一添加
        val request = Request.Builder()
            .url("$DEEPSEEK_BASE_URL$CHAT_COMPLETIONS_PATH")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit("[错误] HTTP ${response.code}: ${response.message}")
                return@use
            }
            val body = response.body ?: run {
                emit("[错误] 响应体为空")
                return@use
            }
            body.charStream().buffered().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (!currentLine.startsWith(SSE_DATA_PREFIX)) continue

                    val data = currentLine.removePrefix(SSE_DATA_PREFIX).trim()
                    if (data == SSE_DONE) {
                        emit("")  // 结束标记
                        break
                    }
                    try {
                        val chunk = gson.fromJson(data, ChatChunk::class.java)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (_: Exception) {
                        // 忽略无法解析的中间行
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun withSystemPrompt(messages: List<ChatRequest.Message>): List<ChatRequest.Message> =
        listOf(ChatRequest.Message(role = "system", content = systemPrompt)) + messages

    private companion object {
        const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"
        const val CHAT_COMPLETIONS_PATH = "chat/completions"
        const val SSE_DATA_PREFIX = "data: "
        const val SSE_DONE = "[DONE]"
    }
}
