package com.example.heavyduty.core.domain.model

private const val ONE_REP_MAX_REP_CAP = 12

/**
 * Epley-formula estimated one-rep max: weight × (1 + reps/30).
 *
 * Returns null above [ONE_REP_MAX_REP_CAP] reps — past that point the formula's error
 * grows too large to call the result strength rather than endurance, so it's better to
 * not guess at all than to show a misleading number. A true 1-rep set is a direct
 * measurement, not an estimate, so it's returned unchanged rather than run through the
 * formula (which would nudge it up by ~3%).
 */
fun estimatedOneRepMax(weightKg: Float, reps: Int): Float? = when {
    weightKg <= 0f || reps <= 0 -> null
    reps == 1 -> weightKg
    reps > ONE_REP_MAX_REP_CAP -> null
    else -> weightKg * (1f + reps / 30f)
}

/**
 * The best [estimatedOneRepMax] among these sets' working sets (warm-ups don't count), or null
 * when none has a weight and reps it can be estimated from.
 */
fun List<WorkoutSet>.bestEstimatedOneRepMax(): Float? = filter { !it.isWarmup }
    .mapNotNull { set ->
        val weight = set.weight ?: return@mapNotNull null
        val reps = set.reps ?: return@mapNotNull null
        estimatedOneRepMax(weight, reps)
    }
    .maxOrNull()
