package com.fenji.scoretrace.data.repository

import android.util.Log
import com.fenji.scoretrace.data.remote.glm.GlmApiService
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatRequest
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatResponse
import com.fenji.scoretrace.data.remote.glm.dto.GlmContent
import com.fenji.scoretrace.data.remote.glm.dto.GlmImageUrl
import com.fenji.scoretrace.data.remote.glm.dto.GlmMessage
import com.google.gson.Gson
import com.google.gson.Strictness
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.StringReader
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
            val choice = response.choices.firstOrNull()
            val content = choice?.message?.content
                ?: throw GlmException(null, "GLM 未返回识别结果")
            try {
                Result.success(parse(content))
            } catch (e: Exception) {
                // 解析失败：打印原始内容与 finish_reason，便于判断是 markdown / 非法 token / 截断
                Log.w(TAG, "GLM parse failed (finish_reason=${choice.finishReason}) rawContent=${content.take(500)}")
                throw e
            }
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
        val cleaned = cleanJsonContent(raw)
        val dto = parseRaw(cleaned)
            ?: throw GlmException(null, "识别结果格式异常")
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

    /** 先按标准 JSON 解析；失败再用宽容模式兜底（容忍未加引号的键名等）。 */
    private fun parseRaw(json: String): GlmSheetRaw? {
        val standard: GlmSheetRaw? = runCatching<GlmSheetRaw?> {
            gson.fromJson(json, GlmSheetRaw::class.java)
        }.getOrNull()
        if (standard != null) return standard

        val reader = JsonReader(StringReader(json))
        reader.strictness = Strictness.LENIENT
        return try {
            gson.fromJson<GlmSheetRaw>(reader, GlmSheetRaw::class.java)
        } catch (e: Exception) {
            null
        } finally {
            reader.close()
        }
    }

    /**
     * 清洗模型输出，尽量还原出合法 JSON：
     * 1) 剥离 markdown 代码块围栏；2) 去除 BOM 与零宽字符；3) 截取首个 `{` 到末个 `}`。
     */
    private fun cleanJsonContent(raw: String): String {
        var s = raw.trim()

        val codeBlock = CODE_BLOCK_REGEX.find(s)
        if (codeBlock != null) {
            s = codeBlock.groupValues[1].trim()
        }

        s = s.replace("\uFEFF", "").replace("\u200B", "").trim()

        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1)
        }
        return s
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

        val CODE_BLOCK_REGEX = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)

        val PROMPT = """
            你是成绩单识别助手。请识别这张成绩单图片，**只返回一个 JSON 对象**：
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
            输出要求：仅返回 JSON 对象，禁止添加 markdown 代码块、注释、前后说明文字。第一个字符必须是 {，最后一个字符必须是 }。
        """.trimIndent()
    }
}
