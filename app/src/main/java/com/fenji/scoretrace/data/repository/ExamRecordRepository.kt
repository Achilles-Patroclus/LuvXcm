package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.ExamRecordDao
import com.fenji.scoretrace.data.local.entity.ExamRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ExamRecordRepository {
    fun observeAll(): Flow<List<ExamRecord>>

    suspend fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
    )

    suspend fun clearAll()
}

@Singleton
class DefaultExamRecordRepository @Inject constructor(
    private val examRecordDao: ExamRecordDao,
) : ExamRecordRepository {

    override fun observeAll(): Flow<List<ExamRecord>> = examRecordDao.observeAll()

    override suspend fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
    ) {
        // 覆盖式：先清同名旧行，避免同一考试重复保存累积多条排名行
        examRecordDao.deleteByExamName(examName)
        examRecordDao.upsert(
            ExamRecord(
                examName = examName,
                examDate = examDate,
                classRank = classRank,
                gradeRank = gradeRank,
            )
        )
    }

    override suspend fun clearAll() = examRecordDao.clearAll()
}
