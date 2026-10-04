package com.fenji.scoretrace.di

import com.fenji.scoretrace.data.repository.DefaultExamRecordRepository
import com.fenji.scoretrace.data.repository.DefaultMajorRepository
import com.fenji.scoretrace.data.repository.DefaultNetworkRepository
import com.fenji.scoretrace.data.repository.DefaultNotificationRepository
import com.fenji.scoretrace.data.repository.DefaultSchoolRepository
import com.fenji.scoretrace.data.repository.DefaultScoreRecordRepository
import com.fenji.scoretrace.data.repository.DefaultStudyTaskRepository
import com.fenji.scoretrace.data.repository.DefaultSubjectRepository
import com.fenji.scoretrace.data.repository.DefaultTargetSchoolRepository
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.MajorRepository
import com.fenji.scoretrace.data.repository.NetworkRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.SchoolRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.StudyTaskRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSubjectRepository(impl: DefaultSubjectRepository): SubjectRepository

    @Binds
    abstract fun bindStudyTaskRepository(impl: DefaultStudyTaskRepository): StudyTaskRepository

    @Binds
    abstract fun bindScoreRecordRepository(impl: DefaultScoreRecordRepository): ScoreRecordRepository

    @Binds
    abstract fun bindTargetSchoolRepository(impl: DefaultTargetSchoolRepository): TargetSchoolRepository

    @Binds
    abstract fun bindNetworkRepository(impl: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindSchoolRepository(impl: DefaultSchoolRepository): SchoolRepository

    @Binds
    abstract fun bindMajorRepository(impl: DefaultMajorRepository): MajorRepository

    @Binds
    abstract fun bindExamRecordRepository(impl: DefaultExamRecordRepository): ExamRecordRepository

    @Binds
    abstract fun bindNotificationRepository(impl: DefaultNotificationRepository): NotificationRepository
}
