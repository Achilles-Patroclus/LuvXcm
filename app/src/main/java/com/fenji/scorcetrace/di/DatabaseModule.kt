package com.fenji.scorcetrace.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fenji.scorcetrace.data.local.ScoreTraceDatabase
import com.fenji.scorcetrace.data.local.dao.ScoreRecordDao
import com.fenji.scorcetrace.data.local.dao.StudyTaskDao
import com.fenji.scorcetrace.data.local.dao.SubjectDao
import com.fenji.scorcetrace.data.local.dao.TargetSchoolDao
import com.fenji.scorcetrace.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** 删除错题本表：v1 → v2 时因移除错题实体，需显式清表避免校验失败崩溃。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS wrong_questions")
        }
    }

    /** 新增目标院校表：SQL 与 Room 生成的建表语句逐字一致（已比对 ScoreTraceDatabase_Impl.java）。 */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `target_schools` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`schoolName` TEXT NOT NULL, " +
                    "`majorName` TEXT NOT NULL, " +
                    "`targetScore` INTEGER NOT NULL, " +
                    "`currentScore` INTEGER NOT NULL, " +
                    "`year` INTEGER NOT NULL)"
            )
        }
    }

    /**
     * 新高考 3+1+2：成绩记录新增首选/再选科目两列。
     * 两列均可空 —— `ALTER TABLE ... ADD COLUMN` 加 NOT NULL 列必须带 DEFAULT，可空最省事且已存记录本无此信息。
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `score_records` ADD COLUMN `primarySubject` TEXT")
            db.execSQL("ALTER TABLE `score_records` ADD COLUMN `secondarySubject` TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScoreTraceDatabase =
        Room.databaseBuilder(context, ScoreTraceDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultSubjects(db)
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideSubjectDao(database: ScoreTraceDatabase): SubjectDao = database.subjectDao()

    @Provides
    @Singleton
    fun provideStudyTaskDao(database: ScoreTraceDatabase): StudyTaskDao = database.studyTaskDao()

    @Provides
    @Singleton
    fun provideScoreRecordDao(database: ScoreTraceDatabase): ScoreRecordDao =
        database.scoreRecordDao()

    @Provides
    @Singleton
    fun provideTargetSchoolDao(database: ScoreTraceDatabase): TargetSchoolDao =
        database.targetSchoolDao()

    /** 建库时写入默认科目，避免首屏空白 */
    private fun seedDefaultSubjects(db: SupportSQLiteDatabase) {
        Constants.DEFAULT_SUBJECT_NAMES.forEachIndexed { index, name ->
            val color = Constants.SUBJECT_COLORS[index % Constants.SUBJECT_COLORS.size]
            db.execSQL("INSERT INTO subjects (name, color) VALUES (?, ?)", arrayOf<Any>(name, color))
        }
    }
}
