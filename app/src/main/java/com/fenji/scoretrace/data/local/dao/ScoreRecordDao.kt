package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreRecordDao {

    @Query("SELECT * FROM score_records ORDER BY examDate DESC")
    fun observeAll(): Flow<List<ScoreRecord>>

    /** subjectId 为空表示不筛选（全部科目） */
    @Query("SELECT * FROM score_records WHERE :subjectId IS NULL OR subjectId = :subjectId ORDER BY examDate DESC")
    fun observeBySubject(subjectId: Long?): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM score_records ORDER BY examDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScoreRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ScoreRecord): Long

    @Delete
    suspend fun delete(record: ScoreRecord)

    @Query("DELETE FROM score_records")
    suspend fun clearAll()
}
