package com.example.heavyduty.core.domain.model

/**
 * The current time. Injected wherever logic depends on "now" — workout timestamps, streaks,
 * fatigue decay, "this week" — so tests can pin it to a fixed moment instead of depending on
 * the day they run (core-testing's FakeClock).
 */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}
