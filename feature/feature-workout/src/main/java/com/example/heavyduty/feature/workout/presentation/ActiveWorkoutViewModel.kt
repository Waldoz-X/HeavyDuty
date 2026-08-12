package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val logSetUseCase: LogSetUseCase,
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ActiveWorkoutUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        checkForActiveSession()
    }

    fun onEvent(event: ActiveWorkoutUiEvent) {
        when (event) {
            is ActiveWorkoutUiEvent.OnRepsChange -> {
                _uiState.update { it.copy(repsInput = event.reps) }
            }
            is ActiveWorkoutUiEvent.OnWeightChange -> {
                _uiState.update { it.copy(weightInput = event.weight) }
            }
            is ActiveWorkoutUiEvent.OnSelectExercise -> {
                _uiState.update { it.copy(currentExerciseIndex = event.index, currentSetNumber = 1) }
            }
            ActiveWorkoutUiEvent.OnLogSet -> logSet()
            ActiveWorkoutUiEvent.OnCompleteWorkout -> completeWorkout()
            ActiveWorkoutUiEvent.OnStartFreeWorkout -> startFreeWorkout()
            ActiveWorkoutUiEvent.OnNavigateToRoutines -> {
                viewModelScope.launch { _uiEffect.emit(ActiveWorkoutUiEffect.NavigateToRoutines) }
            }
        }
    }

    private fun checkForActiveSession() {
        viewModelScope.launch {
            val activeSession = workoutRepository.getActiveSession().firstOrNull()
            if (activeSession != null) {
                // Resume active session
                val sessionWithSets = workoutRepository.getSessionWithSets(activeSession.id).firstOrNull()
                _uiState.update {
                    it.copy(
                        sessionId = activeSession.id,
                        sessionName = activeSession.sessionName,
                        completedSets = sessionWithSets?.sets ?: emptyList(),
                        isActive = true,
                        isLoading = false,
                    )
                }
                activeSession.routineId?.let {
                    loadRoutineExercises(it)
                }
                startTimer()
            } else {
                routineId?.let { startWorkoutFromRoutine(it) } ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun startWorkoutFromRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = routineRepository.getRoutineWithExercises(routineId).firstOrNull()
            if (routine != null) {
                val sessionId = startWorkoutUseCase(
                    routineId = routineId,
                    sessionName = routine.name,
                    type = routine.type,
                )
                _uiState.update {
                    it.copy(
                        sessionId = sessionId,
                        sessionName = routine.name,
                        routineExercises = routine.exercises,
                        isActive = true,
                        isLoading = false,
                    )
                }
                startTimer()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Routine not found") }
            }
        }
    }

    private fun startFreeWorkout() {
        viewModelScope.launch {
            val sessionId = startWorkoutUseCase(sessionName = "Free Workout")
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    sessionName = "Free Workout",
                    isActive = true,
                    isLoading = false,
                )
            }
            startTimer()
        }
    }

    private fun loadRoutineExercises(routineId: Long) {
        viewModelScope.launch {
            routineRepository.getRoutineWithExercises(routineId).firstOrNull()?.let { routine ->
                _uiState.update { it.copy(routineExercises = routine.exercises) }
            }
        }
    }

    private fun logSet() {
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        val exercises = state.routineExercises
        if (exercises.isEmpty()) return

        val currentExercise = exercises.getOrNull(state.currentExerciseIndex) ?: return
        val reps = state.repsInput.toIntOrNull()
        val weight = state.weightInput.toFloatOrNull()

        viewModelScope.launch {
            try {
                logSetUseCase(
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    setNumber = state.currentSetNumber,
                    reps = reps,
                    weight = weight,
                )
                _uiState.update {
                    it.copy(
                        currentSetNumber = it.currentSetNumber + 1,
                        repsInput = "",
                        weightInput = "",
                    )
                }
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Set logged!"))
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Failed to log set"))
            }
        }
    }

    private fun completeWorkout() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            try {
                completeWorkoutUseCase(sessionId)
                _uiState.update { it.copy(isActive = false) }
                _uiEffect.emit(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Failed to complete workout"))
            }
        }
    }



    private fun startTimer() {
        viewModelScope.launch {
            while (_uiState.value.isActive) {
                delay(1.seconds)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }
}
