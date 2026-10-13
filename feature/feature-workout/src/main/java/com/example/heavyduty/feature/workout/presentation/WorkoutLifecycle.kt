package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.DiscardWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Starting a workout (from a routine or free), finishing it and discarding it. Starting only
 * inserts the session: the screen fills in when the database reports it as the active one.
 */
internal class WorkoutLifecycle(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ActiveWorkoutUiState>,
    private val effects: WorkoutEffects,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val discardWorkoutUseCase: DiscardWorkoutUseCase,
    private val routineRepository: RoutineRepository,
    private val restTimerController: RestTimerController,
    private val clock: Clock
) {
    /** A session is being inserted and the database hasn't reported it as active yet. */
    var isStarting = false
        private set

    /** The database reported the workout in progress: whatever was starting has started. */
    fun onSessionActive() {
        isStarting = false
    }

    fun startFromRoutine(routineId: Long) {
        if (uiState.value.isActive) {
            effects.showMessage(R.string.workout_already_active)
            return
        }
        if (isStarting) return
        isStarting = true
        uiState.update { it.copy(isLoading = true) }
        scope.launch {
            val routine = routineRepository.getRoutineWithExercises(routineId).firstOrNull()
            if (routine == null) {
                isStarting = false
                uiState.update { it.copy(isLoading = false) }
                effects.showMessage(R.string.workout_routine_not_found)
                return@launch
            }
            startSession {
                startWorkoutUseCase(
                    routineId = routineId,
                    sessionName = routine.name,
                    type = routine.type,
                    startTime = clock.nowMillis()
                )
            }
        }
    }

    fun startFree() {
        if (uiState.value.isActive || isStarting) return
        isStarting = true
        uiState.update { it.copy(isLoading = true) }
        scope.launch {
            startSession { startWorkoutUseCase(sessionName = FREE_WORKOUT_NAME, startTime = clock.nowMillis()) }
        }
    }

    /** Inserts the session; the active-session observer picks it up and fills the screen. */
    private suspend fun startSession(insert: suspend () -> Long) {
        try {
            insert()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            isStarting = false
            uiState.update { it.copy(isLoading = false) }
            effects.showMessage(R.string.workout_error_start)
        }
    }

    fun complete() {
        val sessionId = uiState.value.sessionId ?: return
        scope.launch {
            try {
                completeWorkoutUseCase(sessionId)
                restTimerController.skipRest()
                effects.send(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                effects.showMessage(R.string.workout_error_finish)
            }
        }
    }

    fun discard() {
        val sessionId = uiState.value.sessionId ?: return
        scope.launch {
            try {
                discardWorkoutUseCase(sessionId)
                restTimerController.skipRest()
                effects.send(ActiveWorkoutUiEffect.WorkoutDiscarded)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                effects.showMessage(R.string.workout_error_discard)
            }
        }
    }
}

private const val FREE_WORKOUT_NAME = "Entrenamiento libre"
