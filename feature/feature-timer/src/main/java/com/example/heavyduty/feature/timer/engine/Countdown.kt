package com.example.heavyduty.feature.timer.engine

import kotlin.math.ceil
import kotlinx.coroutines.delay

internal const val MILLIS_PER_SECOND = 1_000L

/**
 * Milliseconds that keep counting while the phone sleeps, and that changing the date or time
 * doesn't move (SystemClock.elapsedRealtime): what a running timer measures against.
 */
fun interface TimerClock {
    fun elapsedMillis(): Long
}

/**
 * A countdown to a moment on [clock], rather than a count of delays: with the screen off the phone
 * may sleep through a delay, and time slept must count.
 */
internal class Countdown(private val clock: TimerClock) {
    private var endsAt = 0L

    fun startFrom(seconds: Int) {
        endsAt = clock.elapsedMillis() + seconds * MILLIS_PER_SECOND
    }

    fun extendBy(seconds: Int) {
        endsAt += seconds * MILLIS_PER_SECOND
    }

    /** From when the last round ended, not from now, so rounds don't drift apart. */
    fun nextRound(seconds: Int) = extendBy(seconds)

    /** Calls [show] with the whole seconds left each time they change, the last time with 0. */
    suspend fun runToZero(show: (Int) -> Unit) {
        while (true) {
            val leftMillis = endsAt - clock.elapsedMillis()
            show(ceil(leftMillis.coerceAtLeast(0) / MILLIS_PER_SECOND.toDouble()).toInt())
            if (leftMillis <= 0) return
            delay((leftMillis - 1) % MILLIS_PER_SECOND + 1)
        }
    }
}
