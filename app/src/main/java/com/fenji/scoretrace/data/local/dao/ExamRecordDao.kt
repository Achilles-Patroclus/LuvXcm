package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.ExamRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamRecordDao {

    @Query("SELECT * FROM exam_records ORDER BY examDate DESC")
    fun observeAll(): Flow<List<ExamRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ExamRecord): Long

    @Query("DELETE FROM exam_records")
    suspend fun clearAll()
}
