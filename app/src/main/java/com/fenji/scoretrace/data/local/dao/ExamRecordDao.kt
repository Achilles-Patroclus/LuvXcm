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

    /** 删除某次考试的排名行，返回删除条数（重录/编辑时先清旧行，避免同名累积）。 */
    @Query("DELETE FROM exam_records WHERE examName = :examName")
    suspend fun deleteByExamName(examName: String): Int

    @Query("DELETE FROM exam_records")
    suspend fun clearAll()
}
