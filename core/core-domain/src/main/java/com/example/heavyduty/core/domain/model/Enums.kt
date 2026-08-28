package com.example.heavyduty.core.domain.model

/**
 * Type of exercise that determines logging behavior.
 */
enum class ExerciseType(val displayName: String) {
    STRENGTH("Fuerza"),
    CARDIO("Cardio"),
    FUNCTIONAL("Funcional"),
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
    GYM("Gimnasio"),
    CROSSFIT("CrossFit"),
    CARDIO("Cardio"),
    MIXED("Mixto"),
}

/**
 * Recurrence pattern for calendar entries.
 */
enum class Recurrence {
    WEEKLY,
    CUSTOM,
}

/**
 * How [nextPrescription] should adjust an exercise's weight/reps/duration between sessions.
 */
enum class ProgressionPolicy(val displayName: String) {
    OFF("Sin progresión automática"),
    LINEAR("Lineal"),
    GREYSKULL_LP("Greyskull LP"),
    DOUBLE_PROGRESSION("Doble progresión"),
    TIME("Por tiempo"),
}
