package com.example.heavyduty.core.domain.model

/**
 * Type of exercise that determines logging behavior.
 */
enum class ExerciseType {
    STRENGTH,
    CARDIO,
    FUNCTIONAL,
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
enum class WorkoutType {
    GYM,
    CROSSFIT,
    CARDIO,
    MIXED,
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
enum class ProgressionPolicy {
    OFF,
    LINEAR,
    GREYSKULL_LP,
    DOUBLE_PROGRESSION,
    TIME,
}

/**
 * Which unit an exercise's weight is entered/displayed in — set per exercise (a barbell
 * exercise loaded in kg plates vs. a dumbbell line only sold in lb, say). Every weight is
 * still stored in kg everywhere (sets, targets, reports) regardless of this; it only decides
 * the number shown to and typed by the lifter for that exercise. See [WeightUnitExtensions].
 */
enum class WeightUnit {
    KG,
    LB,
}
