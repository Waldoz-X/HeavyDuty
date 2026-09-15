package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.domain.model.nextPrescription
import com.example.heavyduty.core.domain.model.prefillFromLastSession
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.domain.model.toProgressionConfig
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private var workTimerJob: Job? = null
    private var timerJob: Job? = null

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
            ActiveWorkoutUiEvent.OnToggleTimedInput -> toggleTimedInput()
            ActiveWorkoutUiEvent.OnToggleWorkTimer -> toggleWorkTimer()
            is ActiveWorkoutUiEvent.OnSelectExercise -> {
                resetWorkTimer()
                val selectedExId = _uiState.value.routineExercises.getOrNull(event.index)?.exerciseId
                val nextSet = if (selectedExId != null) {
                    getNextSetNumber(selectedExId, _uiState.value.completedSets)
                } else 1
                _uiState.update { it.copy(currentExerciseIndex = event.index, currentSetNumber = nextSet) }
                refreshSuggestion()
                prefillCurrentSet()
            }
            is ActiveWorkoutUiEvent.OnDeleteSet -> deleteSet(event.set)
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

    private fun getNextSetNumber(exerciseId: Long, completedSets: List<WorkoutSet>): Int {
        val setsForExercise = completedSets.filter { it.exerciseId == exerciseId }
        val maxSet = setsForExercise.maxOfOrNull { it.setNumber } ?: 0
        return maxOf(setsForExercise.size, maxSet) + 1
    }

    /**
     * Which exercise to land on when (re)computing state for an already-running session —
     * on first load this is just index 0, but when the ViewModel is recreated mid-session
     * (process death, or navigating back into the workout tab from elsewhere) it must resolve
     * to wherever the lifter actually left off, not silently fall back to the first exercise.
     */
    private fun resumeExerciseIndex(exercises: List<RoutineExercise>, completedSets: List<WorkoutSet>): Int {
        val lastLoggedExerciseId = completedSets.maxByOrNull { it.completedAt ?: 0L }?.exerciseId ?: return 0
        return exercises.indexOfFirst { it.exerciseId == lastLoggedExerciseId }.takeIf { it >= 0 } ?: 0
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
        val state = _uiState.value
        val suggestion = state.suggestedPrescription ?: return
        val unit = state.routineExercises.getOrNull(state.currentExerciseIndex)?.exercise?.weightUnit ?: WeightUnit.KG
        _uiState.update {
            it.copy(
                repsInput = suggestion.reps?.toString() ?: it.repsInput,
                weightInput = suggestion.weightKg?.kgTo(unit)?.toString() ?: it.weightInput,
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
        val unit = routineExercise.exercise?.weightUnit ?: WeightUnit.KG
        val setNumber = state.currentSetNumber
        viewModelScope.launch {
            val history = historyExcludingCurrentSession(routineExercise.exerciseId)
            val prefill = history.prefillFromLastSession(setNumber) ?: return@launch
            _uiState.update {
                if (it.repsInput.isNotEmpty() || it.weightInput.isNotEmpty()) return@update it
                it.copy(
                    repsInput = prefill.reps?.toString() ?: it.repsInput,
                    weightInput = prefill.weight?.kgTo(unit)?.toString() ?: it.weightInput,
                )
            }
        }
    }

    /** Switches the current set between reps input and a work-hold timer (planks, hangs, carries). */
    private fun toggleTimedInput() {
        resetWorkTimer()
        _uiState.update { it.copy(isTimedInput = !it.isTimedInput) }
    }

    private fun toggleWorkTimer() {
        if (_uiState.value.isWorkTimerRunning) {
            workTimerJob?.cancel()
            _uiState.update { it.copy(isWorkTimerRunning = false) }
        } else {
            _uiState.update { it.copy(isWorkTimerRunning = true) }
            workTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1.seconds)
                    _uiState.update { it.copy(workTimerElapsedSecs = it.workTimerElapsedSecs + 1) }
                }
            }
        }
    }

    private fun resetWorkTimer() {
        workTimerJob?.cancel()
        _uiState.update { it.copy(isWorkTimerRunning = false, workTimerElapsedSecs = 0) }
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
                val sessionStartTime = activeSession.startTime
                val currentElapsed = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt().coerceAtLeast(0)
                // Resume active session
                val sessionWithSets = workoutRepository.getSessionWithSets(activeSession.id).firstOrNull()
                val sets = sessionWithSets?.sets ?: emptyList()
                _uiState.update {
                    it.copy(
                        sessionId = activeSession.id,
                        sessionName = activeSession.sessionName,
                        completedSets = sets,
                        isActive = true,
                        isLoading = false,
                        elapsedSeconds = currentElapsed,
                        sessionStartTime = sessionStartTime,
                        allowAddingExercises = activeSession.routineId == null,
                    )
                }
                val routineId = activeSession.routineId
                if (routineId != null) {
                    loadRoutineExercises(routineId)
                } else if (sets.isNotEmpty()) {
                    val distinctExerciseIds = sets.map { it.exerciseId }.distinct()
                    val exercises = distinctExerciseIds.mapNotNull { exerciseRepository.getExerciseById(it).firstOrNull() }
                    val routineExercises = exercises.mapIndexed { index, ex ->
                        RoutineExercise(
                            exerciseId = ex.id,
                            exercise = ex,
                            orderIndex = index,
                        )
                    }
                    val resumeIndex = resumeExerciseIndex(routineExercises, sets)
                    val currentExId = routineExercises.getOrNull(resumeIndex)?.exerciseId
                    val nextSet = if (currentExId != null) getNextSetNumber(currentExId, sets) else 1
                    _uiState.update { state ->
                        state.copy(
                            routineExercises = routineExercises,
                            currentExerciseIndex = resumeIndex,
                            currentSetNumber = nextSet,
                        )
                    }
                    refreshSuggestion()
                }
                startTimer(sessionStartTime)
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
                val now = System.currentTimeMillis()
                val sessionId = startWorkoutUseCase(
                    routineId = routineId,
                    sessionName = routine.name,
                    type = routine.type,
                    startTime = now,
                )
                _uiState.update {
                    it.copy(
                        sessionId = sessionId,
                        sessionName = routine.name,
                        routineExercises = routine.exercises,
                        isActive = true,
                        isLoading = false,
                        elapsedSeconds = 0,
                        sessionStartTime = now,
                    )
                }
                refreshSuggestion()
                startTimer(now)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Rutina no encontrada") }
            }
        }
    }

    private fun startFreeWorkout() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sessionId = startWorkoutUseCase(
                sessionName = "Entrenamiento Libre",
                startTime = now,
            )
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    sessionName = "Entrenamiento Libre",
                    isActive = true,
                    isLoading = false,
                    allowAddingExercises = true,
                    elapsedSeconds = 0,
                    sessionStartTime = now,
                )
            }
            startTimer(now)
        }
    }

    private fun loadRoutineExercises(routineId: Long) {
        viewModelScope.launch {
            routineRepository.getRoutineWithExercises(routineId).firstOrNull()?.let { routine ->
                _uiState.update { state ->
                    val resumeIndex = resumeExerciseIndex(routine.exercises, state.completedSets)
                    val currentExId = routine.exercises.getOrNull(resumeIndex)?.exerciseId
                    val nextSet = if (currentExId != null) getNextSetNumber(currentExId, state.completedSets) else 1
                    state.copy(
                        routineExercises = routine.exercises,
                        currentExerciseIndex = resumeIndex,
                        currentSetNumber = nextSet,
                    )
                }
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
        val reps = if (state.isTimedInput) null else state.repsInput.toIntOrNull()
        val durationSecs = if (state.isTimedInput) state.workTimerElapsedSecs.takeIf { it > 0 } else null
        // weightInput is typed in the exercise's own display unit — always stored in kg.
        val unit = currentExercise.exercise?.weightUnit ?: WeightUnit.KG
        val weight = state.weightInput.toFloatOrNull()?.toKgFrom(unit)
        val effort = state.effortInput.toFloatOrNull()?.let { value ->
            if (state.preferredEffortScale == "RPE") Effort.Rpe(value) else Effort.Rir(value.toInt())
        }

        viewModelScope.launch {
            try {
                val setId = logSetUseCase(
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    setNumber = state.currentSetNumber,
                    reps = reps,
                    weight = weight,
                    durationSecs = durationSecs,
                    isWarmup = state.isWarmupInput,
                    effort = effort,
                    restSeconds = currentExercise.restBetweenSetsSecs,
                )
                val loggedSet = WorkoutSet(
                    id = setId,
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    setNumber = state.currentSetNumber,
                    reps = reps,
                    weight = weight,
                    durationSecs = durationSecs,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    isWarmup = state.isWarmupInput,
                    effort = effort,
                )
                val newCompletedSets = state.completedSets + loggedSet
                val nextSet = getNextSetNumber(currentExercise.exerciseId, newCompletedSets)
                resetWorkTimer()
                _uiState.update {
                    it.copy(
                        completedSets = newCompletedSets,
                        currentSetNumber = nextSet,
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

    private fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(set)
                val newCompletedSets = _uiState.value.completedSets.filter { it.id != set.id }
                val currentExId = _uiState.value.routineExercises.getOrNull(_uiState.value.currentExerciseIndex)?.exerciseId
                val nextSet = if (currentExId != null) {
                    getNextSetNumber(currentExId, newCompletedSets)
                } else 1
                _uiState.update {
                    it.copy(
                        completedSets = newCompletedSets,
                        currentSetNumber = nextSet,
                    )
                }
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Serie eliminada"))
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Error al eliminar la serie"))
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
        resetWorkTimer()
        val nextSet = getNextSetNumber(exercise.id, _uiState.value.completedSets)
        _uiState.update { state ->
            val newEntry = RoutineExercise(
                exerciseId = exercise.id,
                exercise = exercise,
                orderIndex = state.routineExercises.size,
            )
            state.copy(
                routineExercises = state.routineExercises + newEntry,
                currentExerciseIndex = state.routineExercises.size,
                currentSetNumber = nextSet,
                isPickingExercise = false,
                exercisePickerQuery = "",
                exercisePickerResults = emptyList(),
                isTimedInput = false,
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
                timerJob?.cancel()
                timerJob = null
                _uiState.update { it.copy(isActive = false, sessionStartTime = null) }
                _uiEffect.emit(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (_: Exception) {
                _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage("Error al finalizar el entrenamiento"))
            }
        }
    }

    private fun startTimer(startTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isActive) {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1.seconds)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        workTimerJob?.cancel()
    }
}
