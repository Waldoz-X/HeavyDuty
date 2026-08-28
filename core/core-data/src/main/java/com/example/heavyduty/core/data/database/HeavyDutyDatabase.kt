package com.example.heavyduty.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.heavyduty.core.data.database.dao.BodyWeightDao
import com.example.heavyduty.core.data.database.dao.CalendarDao
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.data.database.entity.CalendarEntryEntity
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity

import com.example.heavyduty.core.data.database.dao.ProgramDao
import com.example.heavyduty.core.data.database.entity.ProgramEntity
import com.example.heavyduty.core.data.database.entity.ProgramWeekEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        CalendarEntryEntity::class,
        ProgramEntity::class,
        ProgramWeekEntity::class,
        BodyWeightEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class HeavyDutyDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun calendarDao(): CalendarDao
    abstract fun programDao(): ProgramDao
    abstract fun bodyWeightDao(): BodyWeightDao
}
