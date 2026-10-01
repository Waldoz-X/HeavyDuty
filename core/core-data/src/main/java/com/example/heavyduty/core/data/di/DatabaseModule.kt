package com.example.heavyduty.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.heavyduty.core.common.dispatchers.ApplicationScope
import com.example.heavyduty.core.data.database.ALL_MIGRATIONS
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

/** Oldest schema version with a migration path; see Migrations.kt. */
private const val FIRST_MIGRATED_VERSION = 15

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

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Existing installs seeded with English names get the Spanish ones; a no-op
                    // once every catalog exercise has been localized.
                    applicationScope.launch {
                        SeedData.localizeExerciseNames(context, exerciseDaoProvider.get())
                    }
                }

                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    super.onDestructiveMigration(db)
                    applicationScope.launch {
                        SeedData.populateExercises(context, exerciseDaoProvider.get())
                    }
                }
            })
            .addMigrations(*ALL_MIGRATIONS)
            // Only a downgrade (installing an older build over a newer one) may wipe data; an
            // upgrade without a migration now fails loudly instead of deleting the user's history.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            // Pre-15 builds never shipped migrations; those schemas are still rebuilt as before.
            .fallbackToDestructiveMigrationFrom(true, *(1 until FIRST_MIGRATED_VERSION).toList().toIntArray())
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
    fun provideBodyWeightDao(database: HeavyDutyDatabase): BodyWeightDao =
        database.bodyWeightDao()
}
