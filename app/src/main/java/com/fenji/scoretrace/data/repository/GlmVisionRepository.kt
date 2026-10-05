package com.fenji.scoretrace.data.repository

import android.util.Log
import com.fenji.scoretrace.data.remote.glm.GlmApiService
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatRequest
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatResponse
import com.fenji.scoretrace.data.remote.glm.dto.GlmContent
import com.fenji.scoretrace.data.remote.glm.dto.GlmImageUrl
import com.fenji.scoretrace.data.remote.glm.dto.GlmMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** 识别出的单科分数 */
data class ScoreSheetSubject(val name: String, val score: Double)

/** 成绩单结构化识别结果 */
data class ScoreSheetData(
    val examName: String,
    val examDate: String,
    val scores: List<ScoreSheetSubject>,
    val classRank: Int?,
    val gradeRank: Int?,
)

/** GLM 调用错误：携带智谱返回的 [code] 与 [message]，便于 UI 展示具体原因。 */
class GlmException(val code: String?, override val message: String) : Exception(message)

interface GlmVisionRepository {
    /**
     * 识别成绩单图片。[imageDataUrl] 为 `data:image/jpeg;base64,...`。
     * 返回结构化结果，失败时 [Result.failure] 携带 [GlmException]（含错误码）。
     */
    suspend fun recognizeScoreSheet(imageDataUrl: String): Result<ScoreSheetData>
}

@Singleton
class DefaultGlmVisionRepository @Inject constructor(
    private val apiService: GlmApiService,
    private val gson: Gson,
) : GlmVisionRepository {

    override suspend fun recognizeScoreSheet(imageDataUrl: String): Result<ScoreSheetData> {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = requestOnce(imageDataUrl)
            if (result.isSuccess) return result
            val error = result.exceptionOrNull()
            lastError = error
            // 仅对「限流 1305」做指数退避重试；其它错误（鉴权/格式/内容审核）立即返回。
            val retryable = (error as? GlmException)?.code == CODE_RATE_LIMIT
            if (!retryable || attempt == MAX_ATTEMPTS - 1) return result
            delay(RETRY_BASE_DELAY_MS * (1L shl attempt))
        }
        return Result.failure(lastError ?: GlmException(null, "识别失败"))
    }

    private suspend fun requestOnce(imageDataUrl: String): Result<ScoreSheetData> =
        try {
            val request = GlmChatRequest(
                messages = listOf(
                    GlmMessage(
                        role = "user",
                        content = listOf(
                            GlmContent(type = "image_url", imageUrl = GlmImageUrl(imageDataUrl)),
                            GlmContent(type = "text", text = PROMPT),
                        ),
                    ),
                ),
            )
            val response = apiService.chatCompletion(request)
            response.error?.let { throw GlmException(it.code, it.message ?: "GLM 返回错误") }
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw GlmException(null, "GLM 未返回识别结果")
            Result.success(parse(content))
        } catch (e: HttpException) {
            // 非 2xx：Retrofit 抛 HttpException，错误码在响应体里，必须解析出来。
            Result.failure(toGlmException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** 解析 4xx/5xx 的错误响应体为 [GlmException]（含 code/message）。 */
    private fun toGlmException(e: HttpException): GlmException {
        val rawBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        Log.w(TAG, "GLM HTTP ${e.code()} errorBody=$rawBody")
        val error = rawBody
            ?.let { runCatching { gson.fromJson(it, GlmChatResponse::class.java)?.error }.getOrNull() }
        val message = error?.message?.takeIf { it.isNotBlank() } ?: "HTTP ${e.code()}"
        return GlmException(error?.code, message)
    }

    private fun parse(raw: String): ScoreSheetData {
        val json = extractJson(raw) ?: throw GlmException(null, "识别结果不是有效 JSON")
        val dto = gson.fromJson(json, GlmSheetRaw::class.java)
            ?: throw GlmException(null, "识别结果解析失败")
        val scores = dto.scores.orEmpty()
            .filterKeys { it.isNotBlank() }
            .map { (name, score) -> ScoreSheetSubject(name, score) }
        if (scores.isEmpty() && dto.examName.isNullOrBlank()) {
            throw GlmException(null, "未能识别出成绩信息")
        }
        return ScoreSheetData(
            examName = dto.examName.orEmpty(),
            examDate = dto.examDate.orEmpty(),
            scores = scores,
            classRank = dto.classRank,
            gradeRank = dto.gradeRank,
        )
    }

    /** 取第一个 `{` 到最后一个 `}`，容忍模型包一层 markdown 代码块或多余说明。 */
    private fun extractJson(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private data class GlmSheetRaw(
        @SerializedName("exam_name") val examName: String? = null,
        @SerializedName("exam_date") val examDate: String? = null,
        val scores: Map<String, Double>? = null,
        @SerializedName("class_rank") val classRank: Int? = null,
        @SerializedName("grade_rank") val gradeRank: Int? = null,
    )

    private companion object {
        const val TAG = "GlmVision"
        const val MAX_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 800L
        const val CODE_RATE_LIMIT = "1305"

        val PROMPT = """
            你是成绩单识别助手。请识别这张成绩单图片，**只返回一个 JSON 对象**（不要 markdown 代码块、不要任何解释）：
            {
              "exam_name": "考试名称",
              "exam_date": "yyyy-MM-dd",
              "scores": {"语文": 120, "数学": 130, "英语": 125, "物理": 80, "化学": 85, "生物": 82},
              "class_rank": 12,
              "grade_rank": 100
            }
            要求：
            1. scores 的键必须是图片中确实出现的科目名（可选：语文/数学/英语/物理/化学/生物/政治/历史/地理）。
            2. 识别不到的字段用 null 或直接省略。
            3. 分值为纯数字，不带单位；日期统一为 yyyy-MM-dd。
        """.trimIndent()
    }
}
