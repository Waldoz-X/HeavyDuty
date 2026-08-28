package com.example.heavyduty.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.heavyduty.core.common.dispatchers.ApplicationScope
import com.example.heavyduty.core.data.database.HeavyDutyDatabase
import com.example.heavyduty.core.data.database.SeedData
import com.example.heavyduty.core.data.database.dao.BodyWeightDao
import com.example.heavyduty.core.data.database.dao.CalendarDao
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        exerciseDaoProvider: Provider<ExerciseDao>,
        @ApplicationScope applicationScope: CoroutineScope,
    ): HeavyDutyDatabase {
        return Room.databaseBuilder(
            context,
            HeavyDutyDatabase::class.java,
            "heavyduty.db",
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    applicationScope.launch {
                        SeedData.populateExercises(context, exerciseDaoProvider.get())
                    }
                }
            })
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideExerciseDao(database: HeavyDutyDatabase): ExerciseDao =
        database.exerciseDao()

    @Provides
    fun provideRoutineDao(database: HeavyDutyDatabase): RoutineDao =
        database.routineDao()

    @Provides
    fun provideWorkoutDao(database: HeavyDutyDatabase): WorkoutDao =
        database.workoutDao()

    @Provides
    fun provideCalendarDao(database: HeavyDutyDatabase): CalendarDao =
        database.calendarDao()

    @Provides
    fun provideProgramDao(database: HeavyDutyDatabase): com.example.heavyduty.core.data.database.dao.ProgramDao =
        database.programDao()

    @Provides
    fun provideBodyWeightDao(database: HeavyDutyDatabase): BodyWeightDao =
        database.bodyWeightDao()
}
