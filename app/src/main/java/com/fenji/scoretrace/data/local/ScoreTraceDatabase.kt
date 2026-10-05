package com.fenji.scoretrace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fenji.scoretrace.data.local.dao.ConversationDao
import com.fenji.scoretrace.data.local.dao.ExamRecordDao
import com.fenji.scoretrace.data.local.dao.NotificationDao
import com.fenji.scoretrace.data.local.dao.ScoreRecordDao
import com.fenji.scoretrace.data.local.dao.StudySessionDao
import com.fenji.scoretrace.data.local.dao.StudyTaskDao
import com.fenji.scoretrace.data.local.dao.SubjectDao
import com.fenji.scoretrace.data.local.dao.TargetSchoolDao
import com.fenji.scoretrace.data.local.entity.ConversationEntity
import com.fenji.scoretrace.data.local.entity.ExamRecord
import com.fenji.scoretrace.data.local.entity.MessageEntity
import com.fenji.scoretrace.data.local.entity.NotificationEntity
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import com.fenji.scoretrace.data.local.entity.StudySession
import com.fenji.scoretrace.data.local.entity.StudyTask
import com.fenji.scoretrace.data.local.entity.Subject
import com.fenji.scoretrace.data.local.entity.TargetSchool

@Database(
    entities = [
        Subject::class,
        StudyTask::class,
        ScoreRecord::class,
        TargetSchool::class,
        ExamRecord::class,
        NotificationEntity::class,
        StudySession::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ScoreTraceDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao

    abstract fun studyTaskDao(): StudyTaskDao

    abstract fun scoreRecordDao(): ScoreRecordDao

    abstract fun targetSchoolDao(): TargetSchoolDao

    abstract fun examRecordDao(): ExamRecordDao

    abstract fun notificationDao(): NotificationDao

    abstract fun studySessionDao(): StudySessionDao

    abstract fun conversationDao(): ConversationDao
}
