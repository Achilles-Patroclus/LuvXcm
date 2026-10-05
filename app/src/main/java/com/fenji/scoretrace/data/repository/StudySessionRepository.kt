package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.StudySessionDao
import com.fenji.scoretrace.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface StudySessionRepository {
    /** 全部专注记录，按开始时间倒序 */
    fun observeSessions(): Flow<List<StudySession>>

    suspend fun addSession(startedAt: Long, durationSeconds: Int, type: String)

    suspend fun clearAll()
}

@Singleton
class DefaultStudySessionRepository @Inject constructor(
    private val studySessionDao: StudySessionDao,
) : StudySessionRepository {

    override fun observeSessions(): Flow<List<StudySession>> = studySessionDao.observeAll()

    override suspend fun addSession(startedAt: Long, durationSeconds: Int, type: String) {
        studySessionDao.upsert(
            StudySession(
                startedAt = startedAt,
                durationSeconds = durationSeconds,
                type = type,
            )
        )
    }

    override suspend fun clearAll() = studySessionDao.clearAll()
}
