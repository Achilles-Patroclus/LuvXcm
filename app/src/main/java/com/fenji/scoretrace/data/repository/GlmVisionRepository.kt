package com.fenji.scoretrace.data.repository

import android.util.Log
import com.fenji.scoretrace.data.remote.glm.GlmApiService
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatRequest
import com.fenji.scoretrace.data.remote.glm.dto.GlmChatResponse
import com.fenji.scoretrace.data.remote.glm.dto.GlmContent
import com.fenji.scoretrace.data.remote.glm.dto.GlmImageUrl
import com.fenji.scoretrace.data.remote.glm.dto.GlmMessage
import com.fenji.scoretrace.data.remote.glm.dto.ScoreSheetType
import com.fenji.scoretrace.util.AppLogger
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
     * 拍照识分统一入口：类型由**用户主动选择**（避免模型误判与内容审核）。
     * [type] = PERSONAL → 直接提取；= CLASS_RANKING 且 [name] 为空 → 返回 [VisionResult.ClassRanking]
     *（触发姓名弹窗），有 [name] → 按姓名提取该行。
     * [imageDataUrl] 为 `data:image/jpeg;base64,...`；[onStage] 在各阶段开始时回调，供 UI 展示分阶段 loading。
     */
    suspend fun recognizeScoreSheet(
        imageDataUrl: String,
        type: ScoreSheetType,
        name: String? = null,
        onStage: (VisionStage) -> Unit = {},
    ): Result<VisionResult>

    /**
     * 自动判断图片类型：personal / class_ranking / other。
     * 作为「AI 自动判断」主流程的第一步；失败或不确定（other）时由 UI 降级为手动选择。
     */
    suspend fun detectSheetType(imageDataUrl: String): Result<ScoreSheetType>
}

@Singleton
class DefaultGlmVisionRepository @Inject constructor(
    private val apiService: GlmApiService,
    private val gson: Gson,
) : GlmVisionRepository {

    override suspend fun recognizeScoreSheet(
        imageDataUrl: String,
        type: ScoreSheetType,
        name: String?,
        onStage: (VisionStage) -> Unit,
    ): Result<VisionResult> = when (type) {
        ScoreSheetType.PERSONAL -> {
            AppLogger.i("GlmVision", "start type=PERSONAL")
            onStage(VisionStage.EXTRACTING)
            extractPersonalScore(imageDataUrl).fold(
                onSuccess = {
                    AppLogger.i("GlmVision", "success type=PERSONAL subjects=${it.scores.size}")
                    Result.success(VisionResult.Personal(it))
                },
                onFailure = {
                    AppLogger.e("GlmVision", "fail type=PERSONAL code=${codeOf(it)}", it)
                    Result.success(VisionResult.Error(codeOf(it), messageOf(it)))
                },
            )
        }

        ScoreSheetType.CLASS_RANKING -> {
            if (name.isNullOrBlank()) {
                Result.success(VisionResult.ClassRanking("检测到班级排名表，请输入你的姓名"))
            } else {
                AppLogger.i("GlmVision", "start type=CLASS_RANKING name=$name")
                onStage(VisionStage.SEARCHING)
                extractClassRankingRow(imageDataUrl, name).fold(
                    onSuccess = {
                        AppLogger.i("GlmVision", "success type=CLASS_RANKING subjects=${it.scores.size}")
                        Result.success(VisionResult.Personal(it))
                    },
                    onFailure = {
                        AppLogger.e("GlmVision", "fail type=CLASS_RANKING code=${codeOf(it)}", it)
                        Result.success(VisionResult.Error(codeOf(it), messageOf(it)))
                    },
                )
            }
        }

        // UI 目前只提供 PERSONAL / CLASS_RANKING；保留分支以防未来扩展
        ScoreSheetType.OTHER -> Result.success(VisionResult.Error("UNSUPPORTED", "不支持的图片类型"))
    }

    /** 自动判断图片类型，供「AI 自动判断」主流程使用。 */
    override suspend fun detectSheetType(imageDataUrl: String): Result<ScoreSheetType> =
        callModel(imageDataUrl, TYPE_PROMPT, MAX_TOKENS_TYPE).mapCatching { content ->
            val dto = parseJson(cleanJsonContent(content), TypeRaw::class.java)
            when (dto?.type?.trim()?.lowercase()) {
                "personal" -> ScoreSheetType.PERSONAL
                "class_ranking" -> ScoreSheetType.CLASS_RANKING
                else -> ScoreSheetType.OTHER
            }
        }

    /** 步骤二（个人成绩单）：结构化提取。 */
    private suspend fun extractPersonalScore(imageDataUrl: String): Result<ScoreSheetData> =
        callModel(imageDataUrl, SCORE_PROMPT, MAX_TOKENS_SCORE).mapCatching { parseScoreData(it) }

    /** 步骤二（班级排名表）：按姓名定位该行并提取。 */
    private suspend fun extractClassRankingRow(imageDataUrl: String, name: String): Result<ScoreSheetData> =
        callModel(imageDataUrl, rankingPrompt(name), MAX_TOKENS_ROW).mapCatching { content ->
            if (content.contains(CODE_NOT_FOUND, ignoreCase = true)) {
                throw GlmException(CODE_NOT_FOUND, "未找到该姓名")
            }
            parseScoreData(content)
        }

    /** 发一次请求并返回模型的 text 内容；仅对 1305 限流做指数退避重试。 */
    private suspend fun callModel(imageDataUrl: String, prompt: String, maxTokens: Int): Result<String> {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val attemptResult = try {
                val request = GlmChatRequest(
                    maxTokens = maxTokens,
                    messages = listOf(
                        GlmMessage(
                            role = "user",
                            content = listOf(
                                GlmContent(type = "image_url", imageUrl = GlmImageUrl(imageDataUrl)),
                                GlmContent(type = "text", text = prompt),
                            ),
                        ),
                    ),
                )
                val response = apiService.chatCompletion(request)
                response.error?.let { throw GlmException(it.code, it.message ?: "GLM 返回错误") }
                val content = response.choices.firstOrNull()?.message?.content
                    ?: throw GlmException(null, "GLM 未返回结果")
                Result.success(content)
            } catch (e: HttpException) {
                Result.failure(toGlmException(e))
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (attemptResult.isSuccess) return attemptResult
            lastError = attemptResult.exceptionOrNull()
            val retryable = (lastError as? GlmException)?.code == CODE_RATE_LIMIT
            if (!retryable || attempt == MAX_ATTEMPTS - 1) return attemptResult
            delay(RETRY_BASE_DELAY_MS * (1L shl attempt))
        }
        return Result.failure(lastError ?: GlmException(null, "识别失败"))
    }

    private fun parseScoreData(raw: String): ScoreSheetData {
        val json = cleanJsonContent(raw)
        val dto = parseJson(json, GlmSheetRaw::class.java)
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
    private fun <T> parseJson(json: String, clazz: Class<T>): T? {
        runCatching { gson.fromJson(json, clazz) }.getOrNull()?.let { return it }
        val reader = JsonReader(StringReader(json))
        reader.strictness = Strictness.LENIENT
        return try {
            gson.fromJson(reader, clazz)
        } catch (e: Exception) {
            null
        } finally {
            reader.close()
        }
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

    private fun codeOf(e: Throwable): String = (e as? GlmException)?.code ?: "ERROR"

    private fun messageOf(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() } ?: "识别失败"

    private fun rankingPrompt(name: String): String = """
        这是一张班级排名表。请找到姓名为「$name」的学生所在行，提取其成绩信息。
        如果表格中不存在该姓名，仅返回：{"error":"NOT_FOUND"}
        如果找到，返回与下列结构相同的 JSON：
        {
          "exam_name": "考试名称",
          "exam_date": "yyyy-MM-dd",
          "scores": {"语文": 120, "数学": 130, "英语": 125, "物理": 80, "化学": 85, "生物": 82},
          "class_rank": 12,
          "grade_rank": 100
        }
        要求：分值为纯数字不带单位；识别不到的字段用 null 或省略；日期统一 yyyy-MM-dd。
        输出要求：仅返回 JSON 对象，禁止 markdown 代码块、注释、前后说明文字。第一个字符必须是 {，最后一个字符必须是 }。
    """.trimIndent()

    private data class TypeRaw(val type: String? = null)

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
        const val CODE_NOT_FOUND = "NOT_FOUND"

        // glm-4v-flash 的 max_tokens 合法区间为 [1,1024]
        const val MAX_TOKENS_TYPE = 64
        const val MAX_TOKENS_SCORE = 1024
        const val MAX_TOKENS_ROW = 512

        val CODE_BLOCK_REGEX = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)

        val TYPE_PROMPT = """
            判断这张图片的类型，只返回如下 JSON 之一：
            {"type":"personal"} 表示个人成绩单（单人多科成绩）；
            {"type":"class_ranking"} 表示班级排名表（含多行学生数据）；
            {"type":"other"} 表示与成绩无关的图片。
            输出要求：仅输出 JSON 对象，禁止任何说明文字。第一个字符必须是 {，最后一个字符必须是 }。
        """.trimIndent()

        val SCORE_PROMPT = """
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
