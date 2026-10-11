package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutSet

/** A free-workout entry: no planned sets or reps, so nothing shows as "pending". */
internal fun freeExercise(exercise: Exercise, orderIndex: Int) = RoutineExercise(
    exerciseId = exercise.id,
    exercise = exercise,
    orderIndex = orderIndex,
    targetSets = 0,
    targetReps = 0
)

/** Appends exercises that have logged sets but aren't listed yet (free workouts). */
internal fun mergeLoggedExercises(exercises: List<RoutineExercise>, sets: List<WorkoutSet>): List<RoutineExercise> {
    val listed = exercises.map { it.exerciseId }.toSet()
    val missing = sets.mapNotNull { it.exercise }
        .distinctBy { it.id }
        .filter { it.id !in listed }
    if (missing.isEmpty()) return exercises
    return exercises + missing.mapIndexed { offset, exercise -> freeExercise(exercise, exercises.size + offset) }
}

/**
 * Which exercise to land on when (re)loading an already-running session — wherever the
 * lifter last logged a set, not silently the first exercise.
 */
internal fun resumeExerciseIndex(exercises: List<RoutineExercise>, completedSets: List<WorkoutSet>): Int {
    // Ties (two sets in the same millisecond) go to the one saved last.
    val lastLoggedExerciseId = completedSets
        .maxWithOrNull(compareBy<WorkoutSet>({ it.completedAt ?: 0L }, { it.id }))
        ?.exerciseId ?: return 0
    return exercises.indexOfFirst { it.exerciseId == lastLoggedExerciseId }.takeIf { it >= 0 } ?: 0
}
