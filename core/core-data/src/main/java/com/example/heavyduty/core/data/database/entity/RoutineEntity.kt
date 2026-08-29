package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = ProgramWeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["programWeekId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["programWeekId"])],
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: String = "GYM",
    val programWeekId: Long? = null,
    val dayOfWeek: Int? = null, // e.g., 1 for Day 1, 2 for Day 2 within a week
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["exerciseId"]),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeight: Float = 0f,
    val targetDurationSecs: Int = 0,
    val restBetweenSetsSecs: Int = 60,
    val notes: String = "",
    val progressionPolicy: String = "LINEAR",
    val repRangeLow: Int = 8,
    val repRangeHigh: Int = 10,
    val stallThreshold: Int = 3,
    val supersetGroupId: String? = null,
)
