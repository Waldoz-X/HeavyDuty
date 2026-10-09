package com.example.heavyduty.core.data.database.relation

/** Row of WorkoutDao.getExerciseSetSummaries. */
data class ExerciseSetSummaryRow(
    val exerciseId: Long,
    val workingSets: Int,
    val bestOneRepMaxKg: Double?,
    val bestSetWeightKg: Float?,
    val bestSetReps: Int?,
    val bestSetAt: Long?,
)

/** Row of WorkoutDao.getLastHardSetTimes. */
data class LastHardSetRow(val exerciseId: Long, val lastHardSetAt: Long)

/** Row of WorkoutDao.getBestWeightsBefore. */
data class ExerciseWeightRow(val exerciseId: Long, val weightKg: Float)

/** Row of WorkoutDao.getCompletedSessionTotals. */
data class SessionTotalsRow(val workouts: Int, val durationSecs: Int)

/** Row of WorkoutDao.getWorkingSetTotals. */
data class SetTotalsRow(val sets: Int, val volumeKg: Double)
