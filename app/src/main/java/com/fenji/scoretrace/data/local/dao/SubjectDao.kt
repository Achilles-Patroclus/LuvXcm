package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun observeAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: Subject): Long

    @Delete
    suspend fun delete(subject: Subject)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}
