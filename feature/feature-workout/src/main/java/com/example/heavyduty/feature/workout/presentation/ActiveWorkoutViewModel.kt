package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.nextPrescription
import com.example.heavyduty.core.domain.model.prefillFromLastSession
import com.example.heavyduty.core.domain.model.toProgressionConfig
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
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
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var cachedExercises: List<Exercise>? = null

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ActiveWorkoutUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        checkForActiveSession()
        viewModelScope.launch {
            combine(
                userPreferencesRepository.effortTrackingEnabled,
                userPreferencesRepository.preferredEffortScale,
            ) { enabled, scale -> enabled to scale }.collect { (enabled, scale) ->
                _uiState.update { it.copy(effortTrackingEnabled = enabled, preferredEffortScale = scale) }
            }
        }
    }

    fun onEvent(event: ActiveWorkoutUiEvent) {
        when (event) {
            is ActiveWorkoutUiEvent.OnRepsChange -> {
                _uiState.update { it.copy(repsInput = event.reps) }
            }
            is ActiveWorkoutUiEvent.OnWeightChange -> {
                _uiState.update { it.copy(weightInput = event.weight) }
            }
            ActiveWorkoutUiEvent.OnToggleWarmupInput -> {
                _uiState.update { it.copy(isWarmupInput = !it.isWarmupInput) }
            }
            is ActiveWorkoutUiEvent.OnEffortInputChange -> {
                _uiState.update { it.copy(effortInput = event.value) }
            }
            is ActiveWorkoutUiEvent.OnSelectExercise -> {
                _uiState.update { it.copy(currentExerciseIndex = event.index, currentSetNumber = 1) }
                refreshSuggestion()
                prefillCurrentSet()
            }
            ActiveWorkoutUiEvent.OnLogSet -> logSet()
            ActiveWorkoutUiEvent.OnCompleteWorkout -> completeWorkout()
            ActiveWorkoutUiEvent.OnStartFreeWorkout -> startFreeWorkout()
            ActiveWorkoutUiEvent.OnNavigateToRoutines -> {
                viewModelScope.launch { _uiEffect.emit(ActiveWorkoutUiEffect.NavigateToRoutines) }
            }
            ActiveWorkoutUiEvent.OnToggleExercisePicker -> toggleExercisePicker()
            is ActiveWorkoutUiEvent.OnExercisePickerQueryChange -> onExercisePickerQueryChange(event.query)
            is ActiveWorkoutUiEvent.OnAddExercise -> addExerciseToSession(event.exercise)
            ActiveWorkoutUiEvent.OnUseSuggestion -> useSuggestion()
        }
    }

    /**
     * Recomputes the current exercise's suggested weight/reps from its history in OTHER
     * sessions — the active session's own sets are excluded so the suggestion stays stable
     * as sets are logged today, instead of comparing today against itself.
     */
    private fun refreshSuggestion() {
        val state = _uiState.value
        val routineExercise = state.routineExercises.getOrNull(state.currentExerciseIndex)
        if (routineExercise == null) {
            _uiState.update { it.copy(suggestedPrescription = null) }
            return
        }
        viewModelScope.launch {
            val history = historyExcludingCurrentSession(routineExercise.exerciseId)
            val prescription = nextPrescription(history, routineExercise.toProgressionConfig())
            _uiState.update { it.copy(suggestedPrescription = prescription) }
        }
    }

    private fun useSuggestion() {
        val suggestion = _uiState.value.suggestedPrescription ?: return
        _uiState.update {
            it.copy(
                repsInput = suggestion.reps?.toString() ?: it.repsInput,
                weightInput = suggestion.weightKg?.toString() ?: it.weightInput,
            )
        }
    }

    /**
     * For a free (unplanned) workout only: fills empty reps/weight inputs with whatever was
     * logged at this same set position last time, so repeating last session's numbers doesn't
     * require retyping them. Never overwrites something the lifter already typed.
     */
    private fun prefillCurrentSet() {
        val state = _uiState.value
        if (!state.allowAddingExercises || state.repsInput.isNotEmpty() || state.weightInput.isNotEmpty()) return
        val routineExercise = state.routineExercises.getOrNull(state.currentExerciseIndex) ?: return
        val setNumber = state.currentSetNumber
        viewModelScope.launch {
            val history = historyExcludingCurrentSession(routineExercise.exerciseId)
            val prefill = history.prefillFromLastSession(setNumber) ?: return@launch
            _uiState.update {
                if (it.repsInput.isNotEmpty() || it.weightInput.isNotEmpty()) return@update it
                it.copy(
                    repsInput = prefill.reps?.toString() ?: it.repsInput,
                    weightInput = prefill.weight?.toString() ?: it.weightInput,
                )
            }
        }
    }

    /** This exercise's logged sets from every OTHER session — excludes today's own sets. */
    private suspend fun historyExcludingCurrentSession(exerciseId: Long): List<WorkoutSet> {
        val sessionId = _uiState.value.sessionId
        return workoutRepository.getSetsByExerciseId(exerciseId).firstOrNull().orEmpty()
            .filter { it.sessionId != sessionId }
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
                        allowAddingExercises = activeSession.routineId == null,
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
                refreshSuggestion()
                startTimer()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Rutina no encontrada") }
            }
        }
    }

    private fun startFreeWorkout() {
        viewModelScope.launch {
            val sessionId = startWorkoutUseCase(sessionName = "Entrenamiento Libre")
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    sessionName = "Entrenamiento Libre",
                    isActive = true,
                    isLoading = false,
                    allowAddingExercises = true,
                )
            }
            startTimer()
        }
    }

    private fun loadRoutineExercises(routineId: Long) {
        viewModelScope.launch {
            routineRepository.getRoutineWithExercises(routineId).firstOrNull()?.let { routine ->
                _uiState.update { it.copy(routineExercises = routine.exercises) }
                refreshSuggestion()
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
        val effort = state.effortInput.toFloatOrNull()?.let { value ->
            if (state.preferredEffortScale == "RPE") Effort.Rpe(value) else Effort.Rir(value.toInt())
        }

        viewModelScope.launch {
            try {
                logSetUseCase(
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    setNumber = state.currentSetNumber,
                    reps = reps,
                    weight = weight,
                    isWarmup = state.isWarmupInput,
                    effort = effort,
                    restSeconds = currentExercise.restBetweenSetsSecs,
                )
                _uiState.update {
                    it.copy(
                        currentSetNumber = it.currentSetNumber + 1,
                        repsInput = "",
                        weightInput = "",
                        isWarmupInput = false,
                        effortInput = "",
                    )
                }
                prefillCurrentSet()
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("¡Serie registrada!"))
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Error al registrar la serie"))
            }
        }
    }

    private fun toggleExercisePicker() {
        val opening = !_uiState.value.isPickingExercise
        _uiState.update { it.copy(isPickingExercise = opening) }
        if (opening && cachedExercises == null) {
            viewModelScope.launch {
                cachedExercises = exerciseRepository.getAllExercises().firstOrNull().orEmpty()
                filterExercisePicker(_uiState.value.exercisePickerQuery)
            }
        }
    }

    private fun onExercisePickerQueryChange(query: String) {
        _uiState.update { it.copy(exercisePickerQuery = query) }
        filterExercisePicker(query)
    }

    private fun filterExercisePicker(query: String) {
        val source = cachedExercises.orEmpty()
        val filtered = if (query.isBlank()) {
            source
        } else {
            source.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(exercisePickerResults = filtered.take(30)) }
    }

    private fun addExerciseToSession(exercise: Exercise) {
        _uiState.update { state ->
            val newEntry = RoutineExercise(
                exerciseId = exercise.id,
                exercise = exercise,
                orderIndex = state.routineExercises.size,
            )
            state.copy(
                routineExercises = state.routineExercises + newEntry,
                currentExerciseIndex = state.routineExercises.size,
                currentSetNumber = 1,
                isPickingExercise = false,
                exercisePickerQuery = "",
                exercisePickerResults = emptyList(),
            )
        }
        refreshSuggestion()
        prefillCurrentSet()
    }

    private fun completeWorkout() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            try {
                completeWorkoutUseCase(sessionId)
                _uiState.update { it.copy(isActive = false) }
                _uiEffect.emit(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Error al finalizar el entrenamiento"))
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
