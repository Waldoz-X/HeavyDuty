package com.example.heavyduty.core.domain.repository

/**
 * Starts the app's shared rest timer from outside feature-timer — e.g. automatically after
 * logging a set — without feature-workout depending on feature-timer directly. Implemented by
 * feature-timer's TimerEngine.
 */
interface RestTimerController {
    fun startRest(seconds: Int)
}
