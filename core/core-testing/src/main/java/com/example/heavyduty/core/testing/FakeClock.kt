package com.example.heavyduty.core.testing

import com.example.heavyduty.core.domain.model.Clock

/** A clock that stays where a test puts it; [advanceBy] moves it forward. */
class FakeClock(var now: Long) : Clock {
    override fun nowMillis(): Long = now

    fun advanceBy(millis: Long) {
        now += millis
    }
}
