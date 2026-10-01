package com.example.heavyduty.core.domain.model

/**
 * Domain model representing an active or completed workout session.
 */
data class WorkoutSession(
    val id: Long = 0,
    val routineId: Long? = null,
    val sessionName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalDurationSecs: Int = 0,
    val status: WorkoutStatus = WorkoutStatus.IN_PROGRESS,
    val notes: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val sets: List<WorkoutSet> = emptyList(),
)

/**
 * Domain model representing a single set performed during a workout.
 * Supports strength (reps/weight), cardio (duration/distance), and functional exercises.
 */
data class WorkoutSet(
    val id: Long = 0,
    val sessionId: Long = 0,
    val exerciseId: Long = 0,
    val exercise: Exercise? = null,
    val setNumber: Int = 1,
    val reps: Int? = null,
    val weight: Float? = null,
    val durationSecs: Int? = null,
    val distance: Float? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    /** Ramp-up set — excluded from volume, PRs, 1RM and fatigue (see [excludeWarmups]). */
    val isWarmup: Boolean = false,
    /** Optional perceived-effort rating — see [Effort] doc for why it's never rewritten. */
    val effort: Effort? = null,
    /** Unit the weight was entered in (shown back that way); [weight] itself is always kg. */
    val weightUnit: WeightUnit? = null,
)
