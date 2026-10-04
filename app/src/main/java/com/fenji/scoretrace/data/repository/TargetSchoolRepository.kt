package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.TargetSchoolDao
import com.fenji.scoretrace.data.local.entity.TargetSchool
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TargetSchoolRepository {
    fun observeLatest(): Flow<TargetSchool?>

    /** [id] 传 0 表示新增；传已有 id 则覆盖那条记录 */
    suspend fun save(
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
        id: Long = 0L,
    ): Long

    suspend fun delete(entity: TargetSchool)

    suspend fun clearAll()
}

@Singleton
class DefaultTargetSchoolRepository @Inject constructor(
    private val targetSchoolDao: TargetSchoolDao,
) : TargetSchoolRepository {

    override fun observeLatest(): Flow<TargetSchool?> = targetSchoolDao.observeLatest()

    override suspend fun save(
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
        id: Long,
    ): Long = targetSchoolDao.upsert(
        TargetSchool(
            id = id,
            schoolName = schoolName,
            majorName = majorName,
            targetScore = targetScore,
            currentScore = currentScore,
            year = year,
        )
    )

    override suspend fun delete(entity: TargetSchool) = targetSchoolDao.delete(entity)

    override suspend fun clearAll() = targetSchoolDao.clearAll()
}
