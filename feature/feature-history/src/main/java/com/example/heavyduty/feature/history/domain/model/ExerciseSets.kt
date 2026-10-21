package com.example.heavyduty.feature.history.domain.model

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.excludeWarmups

/** One exercise of a workout and its sets, by set number. */
data class ExerciseSets(val exerciseId: Long, val name: String, val sets: List<WorkoutSet>)

/** The workout's sets grouped by exercise, in the order the exercises were first trained. */
fun WorkoutSession.exerciseSets(): List<ExerciseSets> = sets
    .sortedWith(compareBy({ it.completedAt ?: Long.MAX_VALUE }, { it.setNumber }))
    .groupBy { it.exerciseId }
    .map { (exerciseId, sets) -> ExerciseSets(exerciseId, sets.first().exercise?.displayName.orEmpty(), sets.sortedBy { it.setNumber }) }

/** Working sets (no warm-ups), like everywhere else the app counts sets. */
val WorkoutSession.workingSetCount: Int get() = sets.excludeWarmups().size

/** Kilograms moved in working sets. */
val WorkoutSession.workingVolumeKg: Float
    get() = sets.excludeWarmups().sumOf { (it.weight ?: 0f).toDouble() * (it.reps ?: 0) }.toFloat()
