package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.StudyTaskDao
import com.fenji.scoretrace.data.local.entity.StudyTask
import com.fenji.scoretrace.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface StudyTaskRepository {
    /** 今天到期的任务 */
    fun observeTodayTasks(): Flow<List<StudyTask>>

    /** subjectId 为空表示全部科目 */
    fun observeTasks(subjectId: Long?): Flow<List<StudyTask>>

    suspend fun addTask(subjectId: Long, title: String, content: String, dueDate: java.util.Date?)

    suspend fun setCompleted(id: Long, completed: Boolean)

    suspend fun deleteTask(task: StudyTask)

    suspend fun clearAll()
}

@Singleton
class DefaultStudyTaskRepository @Inject constructor(
    private val studyTaskDao: StudyTaskDao,
) : StudyTaskRepository {

    override fun observeTodayTasks(): Flow<List<StudyTask>> =
        studyTaskDao.observeBetween(DateUtils.startOfDay(), DateUtils.endOfDay())

    override fun observeTasks(subjectId: Long?): Flow<List<StudyTask>> =
        if (subjectId == null) studyTaskDao.observeAll() else studyTaskDao.observeBySubject(subjectId)

    override suspend fun addTask(subjectId: Long, title: String, content: String, dueDate: java.util.Date?) {
        studyTaskDao.upsert(
            StudyTask(
                subjectId = subjectId,
                title = title,
                content = content,
                dueDate = dueDate,
            )
        )
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) =
        studyTaskDao.updateCompleted(id, completed)

    override suspend fun deleteTask(task: StudyTask) = studyTaskDao.delete(task)

    override suspend fun clearAll() = studyTaskDao.clearAll()
}
