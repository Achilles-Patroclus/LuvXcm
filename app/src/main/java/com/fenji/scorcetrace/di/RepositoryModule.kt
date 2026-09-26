package com.fenji.scorcetrace.di

import com.fenji.scorcetrace.data.repository.DefaultNetworkRepository
import com.fenji.scorcetrace.data.repository.DefaultScoreRecordRepository
import com.fenji.scorcetrace.data.repository.DefaultStudyTaskRepository
import com.fenji.scorcetrace.data.repository.DefaultSubjectRepository
import com.fenji.scorcetrace.data.repository.DefaultTargetSchoolRepository
import com.fenji.scorcetrace.data.repository.NetworkRepository
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
}
