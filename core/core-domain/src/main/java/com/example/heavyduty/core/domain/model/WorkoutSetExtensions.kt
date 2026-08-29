package com.example.heavyduty.core.domain.model

/**
 * The single, shared filter every volume, PR, 1RM and fatigue calculation should apply
 * before doing any math — ramp-up sets carry no training signal for those numbers, but
 * still belong in the session's log. Filter through this instead of re-deriving the
 * exclusion at each call site.
 */
fun List<WorkoutSet>.excludeWarmups(): List<WorkoutSet> = filter { !it.isWarmup }

/**
 * The set logged at [setNumber] in this exercise's most recent past session, if any — a
 * literal echo of what was logged, meant to prefill a free (unplanned) workout's inputs so
 * repeating last time's numbers doesn't require retyping them. Contrast with
 * [nextPrescription], which prescribes progression instead of repeating.
 */
fun List<WorkoutSet>.prefillFromLastSession(setNumber: Int): WorkoutSet? {
    val lastSessionId = filter { it.isCompleted && !it.isWarmup && it.completedAt != null }
        .maxByOrNull { it.completedAt!! }
        ?.sessionId
        ?: return null
    return firstOrNull { it.sessionId == lastSessionId && it.setNumber == setNumber && !it.isWarmup }
}
