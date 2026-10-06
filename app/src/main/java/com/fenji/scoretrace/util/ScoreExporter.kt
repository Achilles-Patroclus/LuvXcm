package com.fenji.scoretrace.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 成绩导出：把成绩记录写成 CSV / JSON，保存到系统「下载/ScoreTrace」目录。
 *
 * 走 MediaStore，Android 10+ 无需存储权限；写入采用 IS_PENDING 两段式，失败时清理半成品。
 */
object ScoreExporter {

    const val FORMAT_CSV = "csv"
    const val FORMAT_JSON = "json"

    /** 导出 [records] 为 [format]（csv/json），返回导出条数。失败抛异常由调用方处理。 */
    suspend fun export(
        context: Context,
        records: List<ScoreRecord>,
        subjectNames: Map<Long, String>,
        format: String,
    ): Int = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName: String
        val mimeType: String
        val content: String
        if (format == FORMAT_JSON) {
            fileName = "ScoreTrace_$stamp.json"
            mimeType = "application/json"
            content = toJson(records, subjectNames)
        } else {
            fileName = "ScoreTrace_$stamp.csv"
            mimeType = "text/csv"
            content = toCsv(records, subjectNames)
        }
        writeToDownloads(context, fileName, mimeType, content)
        records.size
    }

    private fun writeToDownloads(context: Context, fileName: String, mime: String, content: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ScoreTrace")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法在下载目录创建文件")
        try {
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: error("无法写入下载文件")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }

    private fun toCsv(records: List<ScoreRecord>, subjectNames: Map<Long, String>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("考试名称,考试日期,科目,分数,满分,首选科目,再选科目\n")
        records.forEach { record ->
            sb.append(csvCell(record.examName)).append(',')
                .append(dateFormat.format(record.examDate)).append(',')
                .append(csvCell(subjectNames[record.subjectId] ?: record.subjectId.toString())).append(',')
                .append(formatScore(record.score)).append(',')
                .append(formatScore(record.fullScore)).append(',')
                .append(csvCell(record.primarySubject.orEmpty())).append(',')
                .append(csvCell(record.secondarySubject.orEmpty()))
                .append('\n')
        }
        return sb.toString()
    }

    private fun toJson(records: List<ScoreRecord>, subjectNames: Map<Long, String>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("[\n")
        records.forEachIndexed { index, record ->
            sb.append("  {")
                .append("\"examName\":").append(jsonString(record.examName)).append(',')
                .append("\"examDate\":").append(jsonString(dateFormat.format(record.examDate))).append(',')
                .append("\"subject\":")
                .append(jsonString(subjectNames[record.subjectId] ?: record.subjectId.toString())).append(',')
                .append("\"score\":").append(formatScore(record.score)).append(',')
                .append("\"fullScore\":").append(formatScore(record.fullScore)).append(',')
                .append("\"primarySubject\":").append(jsonString(record.primarySubject.orEmpty())).append(',')
                .append("\"secondarySubject\":").append(jsonString(record.secondarySubject.orEmpty()))
                .append('}')
            if (index != records.lastIndex) sb.append(',')
            sb.append('\n')
        }
        sb.append("]")
        return sb.toString()
    }

    private fun csvCell(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        return "\"$escaped\""
    }

    private fun formatScore(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
