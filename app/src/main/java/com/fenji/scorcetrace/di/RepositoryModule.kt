package com.fenji.scorcetrace.di

import com.fenji.scorcetrace.data.repository.DefaultExamRecordRepository
import com.fenji.scorcetrace.data.repository.DefaultMajorRepository
import com.fenji.scorcetrace.data.repository.DefaultNetworkRepository
import com.fenji.scorcetrace.data.repository.DefaultNotificationRepository
import com.fenji.scorcetrace.data.repository.DefaultSchoolRepository
import com.fenji.scorcetrace.data.repository.DefaultScoreRecordRepository
import com.fenji.scorcetrace.data.repository.DefaultStudyTaskRepository
import com.fenji.scorcetrace.data.repository.DefaultSubjectRepository
import com.fenji.scorcetrace.data.repository.DefaultTargetSchoolRepository
import com.fenji.scorcetrace.data.repository.ExamRecordRepository
import com.fenji.scorcetrace.data.repository.MajorRepository
import com.fenji.scorcetrace.data.repository.NetworkRepository
import com.fenji.scorcetrace.data.repository.NotificationRepository
import com.fenji.scorcetrace.data.repository.SchoolRepository
import com.fenji.scorcetrace.data.repository.ScoreRecordRepository
import com.fenji.scorcetrace.data.repository.StudyTaskRepository
import com.fenji.scorcetrace.data.repository.SubjectRepository
import com.fenji.scorcetrace.data.repository.TargetSchoolRepository
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
