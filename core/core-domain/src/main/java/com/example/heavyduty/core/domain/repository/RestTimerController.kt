package com.example.heavyduty.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The app's shared rest timer, driven from outside feature-timer — e.g. started automatically
 * after logging a set and shown/adjusted on the workout screen — without feature-workout
 * depending on feature-timer directly. Implemented by feature-timer's TimerEngine.
 */
interface RestTimerController {
    /** The rest countdown in progress, or null when no rest is running. */
    val restState: StateFlow<RestTimerState?>

    fun startRest(seconds: Int)

    /** Adds (or with a negative value, removes) time from the running rest. */
    fun adjustRest(deltaSeconds: Int)

    /** Ends the running rest early. */
    fun skipRest()
}

data class RestTimerState(val remainingSeconds: Int, val totalSeconds: Int)
