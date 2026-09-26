package com.fenji.scorcetrace.data.repository

import com.fenji.scorcetrace.data.local.dao.ScoreRecordDao
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.util.Constants
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ScoreRecordRepository {
    /** subjectId 为空表示全部科目 */
    fun observeRecords(subjectId: Long?): Flow<List<ScoreRecord>>

    fun observeRecent(limit: Int = Constants.HOME_RECENT_SCORE_LIMIT): Flow<List<ScoreRecord>>

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
    }

    override suspend fun deleteRecord(record: ScoreRecord) = scoreRecordDao.delete(record)

    override suspend fun clearAll() = scoreRecordDao.clearAll()
}
