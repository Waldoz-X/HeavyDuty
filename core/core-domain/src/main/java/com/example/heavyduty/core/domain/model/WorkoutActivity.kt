package com.example.heavyduty.core.domain.model

import java.util.concurrent.TimeUnit

/**
 * A pause longer than this between the last thing logged and tapping "Finalizar" isn't part of
 * the workout — a rest between sets is minutes, not half an hour. Past it, the workout ends at
 * its last set instead of when it was finally closed.
 */
val IDLE_END_GAP_MILLIS: Long = TimeUnit.MINUTES.toMillis(30)

/** A workout with nothing logged for this long was almost certainly left open by mistake. */
val FORGOTTEN_AFTER_MILLIS: Long = TimeUnit.HOURS.toMillis(3)

/** When the lifter last did something in this workout: their latest set, or the start. */
fun lastActivityAt(startTime: Long, sets: List<WorkoutSet>): Long = maxOf(startTime, sets.maxOfOrNull { it.completedAt ?: 0L } ?: 0L)

/**
 * When a workout being finished at [now] really ended: [now] when it's closed right after
 * training, but the last set when it sat open long after it — so one left running overnight
 * counts the hour trained, not the 35 it was open.
 */
fun workoutEndTime(startTime: Long, sets: List<WorkoutSet>, now: Long): Long {
    val lastActivity = lastActivityAt(startTime, sets)
    return if (now - lastActivity > IDLE_END_GAP_MILLIS) lastActivity else now
}

/** Nothing logged for [FORGOTTEN_AFTER_MILLIS]: worth asking whether it's still going. */
fun isForgotten(lastActivityAt: Long, now: Long): Boolean = now - lastActivityAt >= FORGOTTEN_AFTER_MILLIS
