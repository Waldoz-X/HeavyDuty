package com.example.heavyduty.core.domain.model

import kotlin.math.round

private const val WEIGHT_INCREMENT_KG = 2.5f
private const val DELOAD_FRACTION = 0.10f
private const val TIME_INCREMENT_SECS = 5

/**
 * Per-exercise inputs to [nextPrescription] — the parts of [RoutineExercise] that shape how
 * its weight/reps/duration should evolve between sessions.
 */
data class ProgressionConfig(
    val policy: ProgressionPolicy,
    val repRangeLow: Int,
    val repRangeHigh: Int,
    val stallThreshold: Int,
    val targetSets: Int,
    val startingWeightKg: Float,
    /** Reps are logged as a total across both sides — the rep step must stay even. */
    val isPerSide: Boolean = false
)

fun RoutineExercise.toProgressionConfig(): ProgressionConfig = ProgressionConfig(
    policy = progressionPolicy,
    repRangeLow = repRangeLow,
    repRangeHigh = repRangeHigh,
    stallThreshold = stallThreshold,
    targetSets = targetSets,
    startingWeightKg = targetWeight,
    isPerSide = exercise?.isPerSide ?: false
)

/**
 * What to do on the next session for one exercise: how many sets, and the reps/weight or
 * duration target for each. [isDeload] and [reasonNote] surface *why* a number changed, so
 * the workout screen can show it instead of a bare, unexplained weight jump or drop.
 */
data class Prescription(
    val sets: Int,
    val reps: Int? = null,
    val weightKg: Float? = null,
    val durationSecs: Int? = null,
    val isDeload: Boolean = false,
    val reasonNote: String? = null
)

/**
 * Derives the next session's target for one exercise from its logged history — nothing is
 * persisted between sessions, so correcting an old set automatically corrects the next
 * suggestion.
 *
 * Bodyweight exercises are detected from the data itself (every set in the most recent
 * session logged 0kg/no weight), not a flag on the exercise — the same movement can be
 * bodyweight one session and weighted the next (e.g. a weighted vest gets added), and the
 * suggestion follows whichever the lifter actually did last. When bodyweight, all four
 * policies are overridden by a single reps-to-a-ceiling-then-add-a-set progression, since
 * "add weight" isn't an available lever. [ProgressionPolicy.TIME] is the exception — it
 * targets [WorkoutSet.durationSecs] (holds/planks), which bodyweight detection doesn't apply
 * to.
 *
 * [ProgressionPolicy.GREYSKULL_LP] approximates "two fixed sets + one AMRAP set" by reading
 * the last logged set of a session as the AMRAP set, since [WorkoutSet] has no set-type
 * discriminator (straight/AMRAP/dropset) yet — see the roadmap's Nivel 2 "registro rico de
 * la sesión" item.
 */
fun nextPrescription(history: List<WorkoutSet>, config: ProgressionConfig): Prescription {
    val sessions = history
        .filter { it.isCompleted && !it.isWarmup && it.completedAt != null }
        .groupBy { it.sessionId }
        .values
        .sortedBy { sets -> sets.minOf { it.completedAt!! } }

    if (config.policy == ProgressionPolicy.OFF) {
        return Prescription(sets = config.targetSets, reps = config.repRangeHigh, weightKg = config.startingWeightKg)
    }
    if (config.policy == ProgressionPolicy.TIME) {
        return timePrescription(sessions, config)
    }

    val lastSession = sessions.lastOrNull()
    val isBodyweight = lastSession != null && lastSession.all { (it.weight ?: 0f) <= 0f }
    if (isBodyweight) {
        return bodyweightPrescription(sessions, config)
    }

    return when (config.policy) {
        ProgressionPolicy.LINEAR -> linearPrescription(sessions, config)
        ProgressionPolicy.GREYSKULL_LP -> greyskullPrescription(sessions, config)
        ProgressionPolicy.DOUBLE_PROGRESSION -> doubleProgressionPrescription(sessions, config)
        ProgressionPolicy.OFF, ProgressionPolicy.TIME -> error("handled above")
    }
}

private fun sessionSucceeded(session: List<WorkoutSet>, targetReps: Int): Boolean =
    session.isNotEmpty() && session.all { (it.reps ?: 0) >= targetReps }

private fun consecutiveFailures(sessions: List<List<WorkoutSet>>, targetReps: Int): Int {
    var failures = 0
    for (session in sessions.asReversed()) {
        if (sessionSucceeded(session, targetReps)) break
        failures++
    }
    return failures
}

private fun Float.roundToNearestIncrement(): Float = round(this / WEIGHT_INCREMENT_KG) * WEIGHT_INCREMENT_KG

private fun linearPrescription(sessions: List<List<WorkoutSet>>, config: ProgressionConfig): Prescription {
    val target = config.repRangeHigh
    val last = sessions.lastOrNull()
        ?: return Prescription(sets = config.targetSets, reps = target, weightKg = config.startingWeightKg)
    val lastWeight = last.mapNotNull { it.weight }.maxOrNull() ?: config.startingWeightKg

    if (sessionSucceeded(last, target)) {
        return Prescription(sets = config.targetSets, reps = target, weightKg = lastWeight + WEIGHT_INCREMENT_KG)
    }

    val failures = consecutiveFailures(sessions, target)
    return if (failures >= config.stallThreshold) {
        Prescription(
            sets = config.targetSets,
            reps = target,
            weightKg = (lastWeight * (1f - DELOAD_FRACTION)).roundToNearestIncrement(),
            isDeload = true,
            reasonNote = "$failures fallos seguidos: deload del ${(DELOAD_FRACTION * 100).toInt()}%"
        )
    } else {
        Prescription(sets = config.targetSets, reps = target, weightKg = lastWeight)
    }
}

/** See [nextPrescription] doc for the AMRAP-set-approximation caveat. */
private fun greyskullPrescription(sessions: List<List<WorkoutSet>>, config: ProgressionConfig): Prescription {
    val target = config.repRangeHigh
    val last = sessions.lastOrNull()
        ?: return Prescription(sets = config.targetSets, reps = target, weightKg = config.startingWeightKg)
    val lastWeight = last.mapNotNull { it.weight }.maxOrNull() ?: config.startingWeightKg
    val amrapReps = last.maxByOrNull { it.setNumber }?.reps ?: 0

    return when {
        amrapReps >= target * 2 -> Prescription(
            sets = config.targetSets,
            reps = target,
            weightKg = lastWeight + WEIGHT_INCREMENT_KG * 2,
            reasonNote = "Reps dobladas en la última serie: salto doble"
        )

        amrapReps >= target -> Prescription(sets = config.targetSets, reps = target, weightKg = lastWeight + WEIGHT_INCREMENT_KG)

        else -> Prescription(
            sets = config.targetSets,
            reps = target,
            weightKg = (lastWeight * (1f - DELOAD_FRACTION)).roundToNearestIncrement(),
            isDeload = true,
            reasonNote = "Fallo en la última serie: deload del ${(DELOAD_FRACTION * 100).toInt()}%"
        )
    }
}

private fun doubleProgressionPrescription(sessions: List<List<WorkoutSet>>, config: ProgressionConfig): Prescription {
    val last = sessions.lastOrNull()
        ?: return Prescription(sets = config.targetSets, reps = config.repRangeLow, weightKg = config.startingWeightKg)
    val lastWeight = last.mapNotNull { it.weight }.maxOrNull() ?: config.startingWeightKg
    val minReps = last.minOf { it.reps ?: 0 }

    val repStep = if (config.isPerSide) 2 else 1
    return if (minReps >= config.repRangeHigh) {
        Prescription(sets = config.targetSets, reps = config.repRangeLow, weightKg = lastWeight + WEIGHT_INCREMENT_KG)
    } else {
        Prescription(sets = config.targetSets, reps = (minReps + repStep).coerceAtMost(config.repRangeHigh), weightKg = lastWeight)
    }
}

private fun bodyweightPrescription(sessions: List<List<WorkoutSet>>, config: ProgressionConfig): Prescription {
    val last = sessions.lastOrNull()
        ?: return Prescription(sets = config.targetSets, reps = config.repRangeLow, weightKg = null)
    val minReps = last.minOf { it.reps ?: 0 }
    val repStep = if (config.isPerSide) 2 else 1

    return when {
        minReps >= config.repRangeHigh -> Prescription(
            sets = config.targetSets + 1,
            reps = config.repRangeLow,
            weightKg = null,
            reasonNote = "Techo de reps alcanzado: se agrega una serie"
        )

        minReps >= config.repRangeLow -> Prescription(sets = config.targetSets, reps = minReps + repStep, weightKg = null)

        else -> Prescription(sets = config.targetSets, reps = minReps, weightKg = null)
    }
}

/** Reuses double progression's logic, but on [WorkoutSet.durationSecs] (holds/planks) instead of reps. */
private fun timePrescription(sessions: List<List<WorkoutSet>>, config: ProgressionConfig): Prescription {
    val last = sessions.lastOrNull()
        ?: return Prescription(sets = config.targetSets, durationSecs = config.repRangeLow)
    val minDuration = last.minOf { it.durationSecs ?: 0 }

    val nextDuration = if (minDuration >= config.repRangeHigh) {
        config.repRangeHigh
    } else {
        (minDuration + TIME_INCREMENT_SECS).coerceAtMost(config.repRangeHigh)
    }
    return Prescription(sets = config.targetSets, durationSecs = nextDuration)
}
