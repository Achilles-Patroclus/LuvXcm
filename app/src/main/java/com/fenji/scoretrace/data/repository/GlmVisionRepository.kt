package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.remote.glm.GlmApiService
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatRequest
import com.fenji.scoretrace.data.remote.glm.dto.GlmContent
import com.fenji.scoretrace.data.remote.glm.dto.GlmImageUrl
import com.fenji.scoretrace.data.remote.glm.dto.GlmMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

interface GlmVisionRepository {
    /**
     * 识别成绩单图片。[imageDataUrl] 为 `data:image/jpeg;base64,...`。
     * 返回结构化结果，失败时 [Result.failure] 携带原因。
     */
    suspend fun recognizeScoreSheet(imageDataUrl: String): Result<ScoreSheetData>
}

@Singleton
class DefaultGlmVisionRepository @Inject constructor(
    private val apiService: GlmApiService,
    private val gson: Gson,
) : GlmVisionRepository {

    override suspend fun recognizeScoreSheet(imageDataUrl: String): Result<ScoreSheetData> =
        runCatching {
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
            response.error?.let { throw IllegalStateException(it.message ?: "GLM 调用失败") }
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("GLM 未返回识别结果")
            parse(content)
        }

    private fun parse(raw: String): ScoreSheetData {
        val json = extractJson(raw) ?: throw IllegalStateException("识别结果不是有效 JSON")
        val dto = gson.fromJson(json, GlmSheetRaw::class.java)
            ?: throw IllegalStateException("识别结果解析失败")
        val scores = dto.scores.orEmpty()
            .filterKeys { it.isNotBlank() }
            .map { (name, score) -> ScoreSheetSubject(name, score) }
        if (scores.isEmpty() && dto.examName.isNullOrBlank()) {
            throw IllegalStateException("未能识别出成绩信息")
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
