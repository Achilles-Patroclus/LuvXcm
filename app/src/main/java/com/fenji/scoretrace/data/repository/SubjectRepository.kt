package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.SubjectDao
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.remote.ApiService
import com.fenji.scoretrace.data.remote.dto.toEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SubjectRepository {
    fun observeSubjects(): Flow<List<Subject>>

    suspend fun addSubject(name: String, color: Long): Long

    suspend fun deleteSubject(subject: Subject)

    suspend fun count(): Int

    /** 从服务端拉取并落库，返回同步条数 */
    suspend fun refreshFromRemote(): Result<Int>
}

@Singleton
class DefaultSubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
    private val apiService: ApiService,
) : SubjectRepository {

    override fun observeSubjects(): Flow<List<Subject>> = subjectDao.observeAll()

    override suspend fun addSubject(name: String, color: Long): Long =
        subjectDao.upsert(Subject(name = name, color = color))

    override suspend fun deleteSubject(subject: Subject) = subjectDao.delete(subject)

    override suspend fun count(): Int = subjectDao.count()

    override suspend fun refreshFromRemote(): Result<Int> = runCatching {
        val remote = apiService.getSubjects().data.orEmpty()
        remote.forEach { subjectDao.upsert(it.toEntity()) }
        remote.size
    }
}
