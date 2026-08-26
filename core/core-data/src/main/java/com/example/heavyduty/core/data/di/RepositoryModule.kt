package com.example.heavyduty.core.data.di

import com.example.heavyduty.core.data.repository.CalendarRepositoryImpl
import com.example.heavyduty.core.data.repository.ExerciseRepositoryImpl
import com.example.heavyduty.core.data.repository.RoutineRepositoryImpl
import com.example.heavyduty.core.data.repository.UserPreferencesRepositoryImpl
import com.example.heavyduty.core.data.repository.WorkoutRepositoryImpl
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindProgramRepository(
        programRepositoryImpl: com.example.heavyduty.core.data.repository.ProgramRepositoryImpl,
    ): com.example.heavyduty.core.domain.repository.ProgramRepository
}
