package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The workout screen's two clocks: the session's elapsed time, which always counts from the
 * session's start (never from this screen), and the hold timer for timed sets (planks, hangs).
 */
internal class WorkoutClocks(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val uiState: MutableStateFlow<ActiveWorkoutUiState>
) {
    private var sessionJob: Job? = null
    private var holdJob: Job? = null

    fun startSession(startTime: Long) {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            while (true) {
                val elapsed = ((clock.nowMillis() - startTime) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)
                uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1.seconds)
            }
        }
    }

    fun toggleHold() {
        if (uiState.value.isWorkTimerRunning) {
            holdJob?.cancel()
            uiState.update { it.copy(isWorkTimerRunning = false) }
        } else {
            uiState.update { it.copy(isWorkTimerRunning = true) }
            holdJob = scope.launch {
                while (true) {
                    delay(1.seconds)
                    uiState.update { it.copy(workTimerElapsedSecs = it.workTimerElapsedSecs + 1) }
                }
            }
        }
    }

    fun resetHold() {
        holdJob?.cancel()
        uiState.update { it.copy(isWorkTimerRunning = false, workTimerElapsedSecs = 0) }
    }

    fun stop() {
        sessionJob?.cancel()
        holdJob?.cancel()
    }
}

private const val MILLIS_PER_SECOND = 1000L
