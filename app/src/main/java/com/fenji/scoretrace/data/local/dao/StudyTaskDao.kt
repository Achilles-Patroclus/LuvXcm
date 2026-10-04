package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.StudyTask
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface StudyTaskDao {

    /** 未完成的排在前面，再按截止时间升序 */
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun observeAll(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE subjectId = :subjectId ORDER BY isCompleted ASC, dueDate ASC")
    fun observeBySubject(subjectId: Long): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE dueDate IS NOT NULL AND dueDate >= :start AND dueDate < :end ORDER BY dueDate ASC")
    fun observeBetween(start: Date, end: Date): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: StudyTask): Long

    @Delete
    suspend fun delete(task: StudyTask)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM study_tasks")
    suspend fun clearAll()
}
