package com.example.heavyduty.core.domain.model

/**
 * Type of exercise that determines logging behavior.
 */
enum class ExerciseType(val displayName: String) {
    STRENGTH("Strength"),
    CARDIO("Cardio"),
    FUNCTIONAL("Functional"),
}

/**
 * Status of a workout session.
 */
enum class WorkoutStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

/**
 * Type of workout session.
 */
enum class WorkoutType(val displayName: String) {
    GYM("Gym"),
    CROSSFIT("CrossFit"),
    CARDIO("Cardio"),
    MIXED("Mixed"),
}

/**
 * Recurrence pattern for calendar entries.
 */
enum class Recurrence {
    WEEKLY,
    CUSTOM,
}
