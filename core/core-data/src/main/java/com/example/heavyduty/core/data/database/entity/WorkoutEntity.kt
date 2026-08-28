package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["routineId"])],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val sessionName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalDurationSecs: Int = 0,
    val status: String = "IN_PROGRESS",
    val notes: String = "",
    val type: String = "GYM",
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
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
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int = 1,
    val reps: Int? = null,
    val weight: Float? = null,
    val durationSecs: Int? = null,
    val distance: Float? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    val isWarmup: Boolean = false,
)
