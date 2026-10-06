package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.ScoreRecordDao
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.Constants
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ScoreRecordRepository {
    /** subjectId 为空表示全部科目 */
    fun observeRecords(subjectId: Long?): Flow<List<ScoreRecord>>

    fun observeRecent(limit: Int = Constants.HOME_RECENT_SCORE_LIMIT): Flow<List<ScoreRecord>>

    /** 全部成绩记录（导出用） */
    fun observeAll(): Flow<List<ScoreRecord>>

    suspend fun addRecord(
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String? = null,
        secondarySubject: String? = null,
    )

    suspend fun deleteRecord(record: ScoreRecord)

    /**
     * 删除某次考试名下的全部单科记录，返回删除条数。
     * [examName] 为空时不做任何删除（防止空值误删）。
     */
    suspend fun deleteExamByName(examName: String): Int

    suspend fun clearAll()
}

@Singleton
class DefaultScoreRecordRepository @Inject constructor(
    private val scoreRecordDao: ScoreRecordDao,
) : ScoreRecordRepository {

    override fun observeRecords(subjectId: Long?): Flow<List<ScoreRecord>> =
        scoreRecordDao.observeBySubject(subjectId)

    override fun observeRecent(limit: Int): Flow<List<ScoreRecord>> =
        scoreRecordDao.observeRecent(limit)

    override fun observeAll(): Flow<List<ScoreRecord>> = scoreRecordDao.observeAll()

    override suspend fun addRecord(
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String?,
        secondarySubject: String?,
    ) {
        scoreRecordDao.upsert(
            ScoreRecord(
                subjectId = subjectId,
                score = score,
                fullScore = fullScore,
                examName = examName,
                examDate = examDate,
                primarySubject = primarySubject,
                secondarySubject = secondarySubject,
            )
        )
        AppLogger.i("ScoreInsert", "exam=$examName subjectId=$subjectId score=$score/$fullScore")
    }

    override suspend fun deleteRecord(record: ScoreRecord) {
        scoreRecordDao.delete(record)
        AppLogger.i("ScoreDelete", "record=${record.examName} subjectId=${record.subjectId}")
    }

    override suspend fun deleteExamByName(examName: String): Int {
        if (examName.isBlank()) {
            AppLogger.w("ScoreDelete", "examName 为空，已跳过删除")
            return 0
        }
        val deleted = scoreRecordDao.deleteByExamName(examName)
        AppLogger.i("ScoreDelete", "exam=$examName deletedRows=$deleted")
        return deleted
    }

    override suspend fun clearAll() = scoreRecordDao.clearAll()
}
