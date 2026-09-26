package com.fenji.scorcetrace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fenji.scorcetrace.data.local.dao.ScoreRecordDao
import com.fenji.scorcetrace.data.local.dao.StudyTaskDao
import com.fenji.scorcetrace.data.local.dao.SubjectDao
import com.fenji.scorcetrace.data.local.dao.TargetSchoolDao
import com.fenji.scorcetrace.data.local.entity.ScoreRecord
import com.fenji.scorcetrace.data.local.entity.StudyTask
import com.fenji.scorcetrace.data.local.entity.Subject
import com.fenji.scorcetrace.data.local.entity.TargetSchool

@Database(
    entities = [
        Subject::class,
        StudyTask::class,
        ScoreRecord::class,
        TargetSchool::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ScoreTraceDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao

    abstract fun studyTaskDao(): StudyTaskDao

    abstract fun scoreRecordDao(): ScoreRecordDao

    abstract fun targetSchoolDao(): TargetSchoolDao
}
