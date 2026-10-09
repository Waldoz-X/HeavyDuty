package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.bestEstimatedOneRepMax
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.other
import com.example.heavyduty.core.domain.model.nextPrescription
import com.example.heavyduty.core.domain.model.searchExercises
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.domain.model.toProgressionConfig
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the active workout. The in-progress session is observed straight from the database,
 * so this screen is correct no matter how many times it's (re)created — a second instance
 * (another nav entry, process death) shows the same logged sets instead of a stale copy.
 * Only what the lifter is typing, and which exercise is on screen, lives here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val logSetUseCase: LogSetUseCase,
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val restTimerController: RestTimerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var cachedExercises: List<Exercise>? = null
    private var workTimerJob: Job? = null
    private var clockJob: Job? = null
    private var exerciseInfoJob: Job? = null
    private var isStartingSession = false

    /** Session whose exercise list is loaded into state; any other id is a different workout. */
    private var loadedSessionId: Long? = null

    /** Routine to start on arrival (from "start today's routine"), until a session exists. */
    private val routineIdToStart: Long? = savedStateHandle.get<Long>(KEY_ROUTINE_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ActiveWorkoutUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        observePreferences()
        observeRoutines()
        observeRestTimer()
        observeActiveSession()
    }

    fun onEvent(event: ActiveWorkoutUiEvent) {
        when (event) {
            is ActiveWorkoutUiEvent.OnRepsChange -> updateInputs { it.copy(repsInput = event.reps) }
            is ActiveWorkoutUiEvent.OnWeightChange -> updateInputs { it.copy(weightInput = event.weight) }
            ActiveWorkoutUiEvent.OnToggleWarmupInput -> _uiState.update { it.copy(isWarmupInput = !it.isWarmupInput) }
            is ActiveWorkoutUiEvent.OnEffortInputChange -> updateInputs { it.copy(effortInput = event.value) }
            ActiveWorkoutUiEvent.OnToggleTimedInput -> toggleTimedInput()
            ActiveWorkoutUiEvent.OnToggleWorkTimer -> toggleWorkTimer()
            is ActiveWorkoutUiEvent.OnSelectExercise -> selectExercise(event.index)
            is ActiveWorkoutUiEvent.OnDeleteSet -> deleteSet(event.set)
            is ActiveWorkoutUiEvent.OnUndoDeleteSet -> restoreSet(event.set)
            ActiveWorkoutUiEvent.OnLogSet -> logSet()
            ActiveWorkoutUiEvent.OnCompleteWorkout -> completeWorkout()
            ActiveWorkoutUiEvent.OnStartFreeWorkout -> startFreeWorkout()
            is ActiveWorkoutUiEvent.OnStartRoutine -> startWorkoutFromRoutine(event.routineId)
            ActiveWorkoutUiEvent.OnNavigateToRoutines -> {
                viewModelScope.launch { _uiEffect.emit(ActiveWorkoutUiEffect.NavigateToRoutines) }
            }
            ActiveWorkoutUiEvent.OnToggleExercisePicker -> toggleExercisePicker()
            is ActiveWorkoutUiEvent.OnExercisePickerQueryChange -> onExercisePickerQueryChange(event.query)
            is ActiveWorkoutUiEvent.OnAddExercise -> addExerciseToSession(event.exercise)
            ActiveWorkoutUiEvent.OnUseSuggestion -> useSuggestion()
            is ActiveWorkoutUiEvent.OnAdjustRest -> restTimerController.adjustRest(event.deltaSeconds)
            ActiveWorkoutUiEvent.OnSkipRest -> restTimerController.skipRest()
            ActiveWorkoutUiEvent.OnToggleUnit -> toggleUnit()
        }
    }

    // ---------------------------------------------------------------- observation

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.effortTrackingEnabled,
                userPreferencesRepository.preferredEffortScale,
            ) { enabled, scale -> enabled to scale }.collect { (enabled, scale) ->
                _uiState.update { it.copy(effortTrackingEnabled = enabled, preferredEffortScale = scale) }
            }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            routineRepository.getAllRoutines().collect { routines ->
                _uiState.update { it.copy(routines = routines) }
            }
        }
    }

    private fun observeRestTimer() {
        viewModelScope.launch {
            restTimerController.restState.collect { rest ->
                _uiState.update { it.copy(restTimer = rest) }
            }
        }
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            workoutRepository.getActiveSession()
                .flatMapLatest { active ->
                    if (active == null) {
                        flowOf(null)
                    } else {
                        workoutRepository.getSessionWithSets(active.id).map { it ?: active }
                    }
                }
                .collect { session ->
                    if (session == null || session.status != WorkoutStatus.IN_PROGRESS) {
                        onNoActiveSession()
                    } else {
                        onActiveSession(session)
                    }
                }
        }
    }

    private fun onNoActiveSession() {
        val routineId = routineIdToStart
        if (routineId != null && savedStateHandle.get<Boolean>(KEY_START_HANDLED) != true) {
            savedStateHandle[KEY_START_HANDLED] = true
            startWorkoutFromRoutine(routineId)
            return
        }
        if (isStartingSession) return
        loadedSessionId = null
        clockJob?.cancel()
        workTimerJob?.cancel()
        exerciseInfoJob?.cancel()
        _uiState.update { current ->
            ActiveWorkoutUiState(
                isLoading = false,
                routines = current.routines,
                restTimer = current.restTimer,
                effortTrackingEnabled = current.effortTrackingEnabled,
                preferredEffortScale = current.preferredEffortScale,
            )
        }
    }

    private suspend fun onActiveSession(session: WorkoutSession) {
        // A workout is running: the nav argument must never start a second one.
        savedStateHandle[KEY_START_HANDLED] = true
        isStartingSession = false
        val sets = session.sets.sortedWith(compareBy({ it.completedAt ?: Long.MAX_VALUE }, { it.id }))

        if (session.id != loadedSessionId) {
            loadedSessionId = session.id
            val exercises = mergeLoggedExercises(loadSessionExercises(session, sets), sets)
            val sameSession = savedStateHandle.get<Long>(KEY_SESSION_ID) == session.id
            val savedIndex = savedStateHandle.get<Int>(KEY_EXERCISE_INDEX).takeIf { sameSession }
            val index = (savedIndex ?: resumeExerciseIndex(exercises, sets))
                .coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
            savedStateHandle[KEY_SESSION_ID] = session.id
            savedStateHandle[KEY_EXERCISE_INDEX] = index
            _uiState.update {
                it.copy(
                    sessionId = session.id,
                    sessionName = session.sessionName,
                    sessionStartTime = session.startTime,
                    isActive = true,
                    isLoading = false,
                    allowAddingExercises = session.routineId == null,
                    routineExercises = exercises,
                    completedSets = sets,
                    currentExerciseIndex = index,
                    // What was typed but not logged yet, if Android closed the app meanwhile.
                    repsInput = savedStateHandle.get<String>(KEY_REPS_INPUT).takeIf { sameSession }.orEmpty(),
                    weightInput = savedStateHandle.get<String>(KEY_WEIGHT_INPUT).takeIf { sameSession }.orEmpty(),
                    effortInput = savedStateHandle.get<String>(KEY_EFFORT_INPUT).takeIf { sameSession }.orEmpty(),
                )
            }
            startClock(session.startTime)
            loadCurrentExerciseInfo()
        } else {
            _uiState.update {
                it.copy(
                    sessionName = session.sessionName,
                    completedSets = sets,
                    routineExercises = mergeLoggedExercises(it.routineExercises, sets),
                )
            }
        }
    }

    /** The routine's exercises, or — for a free workout — the ones logged or added so far. */
    private suspend fun loadSessionExercises(session: WorkoutSession, sets: List<WorkoutSet>): List<RoutineExercise> {
        val routineId = session.routineId
        if (routineId != null) {
            val routineExercises = routineRepository.getRoutineWithExercises(routineId).firstOrNull()?.exercises
            if (!routineExercises.isNullOrEmpty()) return routineExercises
        }
        val addedIds = savedStateHandle.get<LongArray>(KEY_ADDED_EXERCISE_IDS)
            ?.takeIf { savedStateHandle.get<Long>(KEY_SESSION_ID) == session.id }
            ?.toList()
            .orEmpty()
        val loggedIds = sets.map { it.exerciseId }
        return (loggedIds + addedIds).distinct().mapIndexedNotNull { index, exerciseId ->
            val exercise = sets.firstOrNull { it.exerciseId == exerciseId }?.exercise
                ?: exerciseRepository.getExerciseById(exerciseId).firstOrNull()
                ?: return@mapIndexedNotNull null
            freeExercise(exercise, index)
        }
    }

    /** A free-workout entry: no planned sets or reps, so nothing shows as "pending". */
    private fun freeExercise(exercise: Exercise, orderIndex: Int) = RoutineExercise(
        exerciseId = exercise.id,
        exercise = exercise,
        orderIndex = orderIndex,
        targetSets = 0,
        targetReps = 0,
    )

    /** Appends exercises that have logged sets but aren't listed yet (free workouts). */
    private fun mergeLoggedExercises(exercises: List<RoutineExercise>, sets: List<WorkoutSet>): List<RoutineExercise> {
        val listed = exercises.map { it.exerciseId }.toSet()
        val missing = sets.mapNotNull { it.exercise }
            .distinctBy { it.id }
            .filter { it.id !in listed }
        if (missing.isEmpty()) return exercises
        return exercises + missing.mapIndexed { offset, exercise -> freeExercise(exercise, exercises.size + offset) }
    }

    /**
     * Which exercise to land on when (re)loading an already-running session — wherever the
     * lifter last logged a set, not silently the first exercise.
     */
    private fun resumeExerciseIndex(exercises: List<RoutineExercise>, completedSets: List<WorkoutSet>): Int {
        // Ties (two sets in the same millisecond) go to the one saved last.
        val lastLoggedExerciseId = completedSets
            .maxWithOrNull(compareBy<WorkoutSet>({ it.completedAt ?: 0L }, { it.id }))
            ?.exerciseId ?: return 0
        return exercises.indexOfFirst { it.exerciseId == lastLoggedExerciseId }.takeIf { it >= 0 } ?: 0
    }

    // ---------------------------------------------------------------- current exercise

    /**
     * Switching exercises only changes what's on screen: logged sets stay in the database,
     * and neither the session clock, the rest timer nor a running hold timer is touched.
     */
    private fun selectExercise(index: Int) {
        val state = _uiState.value
        if (index == state.currentExerciseIndex || index !in state.routineExercises.indices) return
        savedStateHandle[KEY_EXERCISE_INDEX] = index
        updateInputs {
            it.copy(
                currentExerciseIndex = index,
                repsInput = "",
                weightInput = "",
                effortInput = "",
                isWarmupInput = false,
                suggestedPrescription = null,
                previousSets = emptyMap(),
            )
        }
        loadCurrentExerciseInfo()
    }

    /**
     * Loads the current exercise's suggestion and last-session sets from its history in OTHER
     * sessions — today's own sets are excluded so the suggestion stays stable as sets are logged.
     */
    private fun loadCurrentExerciseInfo() {
        val routineExercise = _uiState.value.currentExercise
        exerciseInfoJob?.cancel()
        if (routineExercise == null) {
            _uiState.update { it.copy(suggestedPrescription = null, previousSets = emptyMap()) }
            return
        }
        exerciseInfoJob = viewModelScope.launch {
            val history = historyExcludingCurrentSession(routineExercise.exerciseId)
            val prescription = nextPrescription(history, routineExercise.toProgressionConfig())
            val lastSessionId = history.filter { it.completedAt != null }.maxByOrNull { it.completedAt!! }?.sessionId
            val previousSets = history.filter { it.sessionId == lastSessionId }.associateBy { it.setNumber }
            _uiState.update { state ->
                if (state.currentExercise?.exerciseId != routineExercise.exerciseId) return@update state
                state.copy(
                    // A free workout has no plan to progress, only last time's numbers.
                    suggestedPrescription = if (state.allowAddingExercises) null else prescription,
                    previousSets = previousSets,
                )
            }
        }
    }

    private fun useSuggestion() {
        val state = _uiState.value
        val suggestion = state.suggestedPrescription ?: return
        val unit = state.currentUnit
        updateInputs {
            it.copy(
                repsInput = suggestion.reps?.toString() ?: it.repsInput,
                weightInput = suggestion.weightKg?.takeIf { kg -> kg > 0f }?.kgToRounded(unit)?.let(::formatInput) ?: it.weightInput,
            )
        }
    }

    /**
     * Changes the state and remembers the typed-but-not-logged reps, weight and effort in
     * [savedStateHandle], so they survive Android closing the app in the background mid-set.
     */
    private inline fun updateInputs(change: (ActiveWorkoutUiState) -> ActiveWorkoutUiState) {
        _uiState.update(change)
        val state = _uiState.value
        savedStateHandle[KEY_REPS_INPUT] = state.repsInput
        savedStateHandle[KEY_WEIGHT_INPUT] = state.weightInput
        savedStateHandle[KEY_EFFORT_INPUT] = state.effortInput
    }

    /** This exercise's logged sets from every OTHER session — excludes today's own sets. */
    private suspend fun historyExcludingCurrentSession(exerciseId: Long): List<WorkoutSet> {
        val sessionId = _uiState.value.sessionId
        return workoutRepository.getSetsByExerciseId(exerciseId).firstOrNull().orEmpty()
            .filter { it.sessionId != sessionId }
    }

    /**
     * Switches the current exercise between kg and lb, and remembers it: in a routine, for that
     * routine's exercise (the same machine next time); in a free workout, as the exercise's
     * default. Sets already logged keep the unit they were entered in.
     */
    private fun toggleUnit() {
        val state = _uiState.value
        val index = state.currentExerciseIndex
        val routineExercise = state.currentExercise ?: return
        val newUnit = routineExercise.displayUnit.other()
        val updated = routineExercise.copy(weightUnit = newUnit)
        updateInputs {
            it.copy(
                routineExercises = it.routineExercises.mapIndexed { i, entry -> if (i == index) updated else entry },
                // What was typed was in the old unit; clear it rather than silently reinterpret it.
                weightInput = "",
            )
        }
        viewModelScope.launch {
            try {
                if (!state.allowAddingExercises && routineExercise.id > 0) {
                    routineRepository.updateRoutineExercise(updated.copy(exercise = null))
                } else {
                    routineExercise.exercise?.let { exerciseRepository.updateExercise(it.copy(weightUnit = newUnit)) }
                }
            } catch (_: Exception) {
                // Only the remembered default failed; this session still uses the new unit.
            }
        }
    }

    // ---------------------------------------------------------------- timers

    /** Switches the current set between reps input and a hold timer (planks, hangs, carries). */
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

    /** The session clock always counts from the session's start time, never from this screen. */
    private fun startClock(startTime: Long) {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1.seconds)
            }
        }
    }

    // ---------------------------------------------------------------- session lifecycle

    private fun startWorkoutFromRoutine(routineId: Long) {
        if (_uiState.value.isActive) {
            showMessage(R.string.workout_already_active)
            return
        }
        if (isStartingSession) return
        isStartingSession = true
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val routine = routineRepository.getRoutineWithExercises(routineId).firstOrNull()
            if (routine == null) {
                isStartingSession = false
                _uiState.update { it.copy(isLoading = false) }
                showMessage(R.string.workout_routine_not_found)
                return@launch
            }
            startSession {
                startWorkoutUseCase(
                    routineId = routineId,
                    sessionName = routine.name,
                    type = routine.type,
                    startTime = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun startFreeWorkout() {
        if (_uiState.value.isActive || isStartingSession) return
        isStartingSession = true
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            startSession { startWorkoutUseCase(sessionName = FREE_WORKOUT_NAME, startTime = System.currentTimeMillis()) }
        }
    }

    /** Inserts the session; the active-session observer picks it up and fills the screen. */
    private suspend fun startSession(insert: suspend () -> Long) {
        try {
            insert()
        } catch (_: Exception) {
            isStartingSession = false
            _uiState.update { it.copy(isLoading = false) }
            showMessage(R.string.workout_error_start)
        }
    }

    private fun logSet() {
        val state = _uiState.value
        if (state.isLoggingSet) return
        val sessionId = state.sessionId ?: return
        val currentExercise = state.currentExercise ?: return
        val hint = state.currentSetHint
        val unit = state.currentUnit

        // Empty inputs fall back to the hint shown in them (suggestion, last time, or the plan).
        val reps = if (state.isTimedInput) null else state.repsInput.toIntOrNull() ?: hint.reps
        val durationSecs = if (state.isTimedInput) {
            state.workTimerElapsedSecs.takeIf { it > 0 } ?: hint.durationSecs
        } else {
            null
        }
        if (reps == null && durationSecs == null) {
            showMessage(if (state.isTimedInput) R.string.workout_error_enter_time else R.string.workout_error_enter_reps)
            return
        }
        // weightInput is typed in the exercise's current unit — always stored in kg. A hint is
        // stored as the rounded value the lifter saw in the field, not the raw conversion.
        val weight = (state.weightInput.toFloatOrNull() ?: hint.weightKg?.kgToRounded(unit))?.toKgFrom(unit)
        val effort = state.effortInput.toFloatOrNull()?.let { value ->
            if (state.preferredEffortScale == "RPE") Effort.Rpe(value) else Effort.Rir(value.toInt())
        }
        val setNumber = state.currentSetNumber

        _uiState.update { it.copy(isLoggingSet = true) }
        viewModelScope.launch {
            try {
                val setId = logSetUseCase(
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    setNumber = setNumber,
                    reps = reps,
                    weight = weight,
                    durationSecs = durationSecs,
                    isWarmup = state.isWarmupInput,
                    effort = effort,
                    restSeconds = currentExercise.restBetweenSetsSecs,
                    weightUnit = unit,
                )
                val loggedSet = WorkoutSet(
                    id = setId,
                    sessionId = sessionId,
                    exerciseId = currentExercise.exerciseId,
                    exercise = currentExercise.exercise,
                    setNumber = setNumber,
                    reps = reps,
                    weight = weight,
                    durationSecs = durationSecs,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    isWarmup = state.isWarmupInput,
                    effort = effort,
                    weightUnit = unit,
                )
                resetWorkTimer()
                // Shown right away; the database emission that follows carries the same set.
                updateInputs {
                    it.copy(
                        completedSets = it.completedSets.filter { set -> set.id != setId } + loggedSet,
                        repsInput = "",
                        weightInput = "",
                        isWarmupInput = false,
                        effortInput = "",
                    )
                }
                checkPersonalRecord(loggedSet, earlierToday = state.setsFor(currentExercise.exerciseId), unit = unit)
            } catch (_: Exception) {
                showMessage(R.string.workout_error_log_set)
            } finally {
                _uiState.update { it.copy(isLoggingSet = false) }
            }
        }
    }

    /**
     * Celebrates a set that beats every earlier set of the exercise — from past sessions and
     * earlier today — by estimated 1RM. The first time an exercise is done there's nothing to
     * beat yet, so it isn't called a record.
     */
    private suspend fun checkPersonalRecord(set: WorkoutSet, earlierToday: List<WorkoutSet>, unit: WeightUnit) {
        if (set.isWarmup) return
        val estimate = estimatedOneRepMax(set.weight ?: return, set.reps ?: return) ?: return
        // Only a bonus: if history can't be read, the set is still logged, just not celebrated.
        val history = try {
            historyExcludingCurrentSession(set.exerciseId)
        } catch (_: Exception) {
            return
        }
        val previousBest = (history + earlierToday).bestEstimatedOneRepMax() ?: return
        if (estimate > previousBest + PR_MIN_GAIN_KG) {
            _uiEffect.emit(
                ActiveWorkoutUiEffect.PersonalRecord(
                    exerciseName = set.exercise?.displayName.orEmpty(),
                    estimatedOneRepMaxKg = estimate,
                    unit = unit,
                ),
            )
        }
    }

    private fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(set)
                _uiState.update { it.copy(completedSets = it.completedSets.filter { s -> s.id != set.id }) }
                _uiEffect.emit(ActiveWorkoutUiEffect.SetDeleted(set))
            } catch (_: Exception) {
                showMessage(R.string.workout_error_delete_set)
            }
        }
    }

    /** Undo for [deleteSet]: re-inserts the set with its original id and number. */
    private fun restoreSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutRepository.insertSet(set)
                _uiState.update { it.copy(completedSets = it.completedSets.filter { s -> s.id != set.id } + set) }
            } catch (_: Exception) {
                showMessage(R.string.workout_error_log_set)
            }
        }
    }

    private fun completeWorkout() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            try {
                completeWorkoutUseCase(sessionId)
                restTimerController.skipRest()
                _uiEffect.emit(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (_: Exception) {
                showMessage(R.string.workout_error_finish)
            }
        }
    }

    // ---------------------------------------------------------------- free workout exercises

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
        _uiState.update { it.copy(exercisePickerResults = source.searchExercises(query).take(PICKER_RESULTS)) }
    }

    private fun addExerciseToSession(exercise: Exercise) {
        val state = _uiState.value
        val existingIndex = state.routineExercises.indexOfFirst { it.exerciseId == exercise.id }
        _uiState.update {
            it.copy(
                isPickingExercise = false,
                exercisePickerQuery = "",
                exercisePickerResults = emptyList(),
            )
        }
        if (existingIndex >= 0) {
            selectExercise(existingIndex)
            return
        }
        val added = savedStateHandle.get<LongArray>(KEY_ADDED_EXERCISE_IDS)
            ?.takeIf { savedStateHandle.get<Long>(KEY_SESSION_ID) == state.sessionId }
            ?: LongArray(0)
        savedStateHandle[KEY_ADDED_EXERCISE_IDS] = added + exercise.id
        _uiState.update {
            it.copy(
                routineExercises = it.routineExercises + freeExercise(exercise, it.routineExercises.size),
            )
        }
        selectExercise(_uiState.value.routineExercises.lastIndex)
    }

    private fun showMessage(messageResId: Int) {
        viewModelScope.launch { _uiEffect.emit(ActiveWorkoutUiEffect.ShowMessage(messageResId)) }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        workTimerJob?.cancel()
    }

    private companion object {
        const val KEY_ROUTINE_ID = "routineId"
        const val KEY_START_HANDLED = "routineStartHandled"
        const val KEY_SESSION_ID = "loadedSessionId"
        const val KEY_EXERCISE_INDEX = "currentExerciseIndex"
        const val KEY_ADDED_EXERCISE_IDS = "addedExerciseIds"
        const val KEY_REPS_INPUT = "repsInput"
        const val KEY_WEIGHT_INPUT = "weightInput"
        const val KEY_EFFORT_INPUT = "effortInput"
        const val FREE_WORKOUT_NAME = "Entrenamiento libre"
        const val PICKER_RESULTS = 50

        /** Margin over the previous best e1RM for a set to count as a record (float noise). */
        const val PR_MIN_GAIN_KG = 0.1f

        /** "62.5" stays "62.5", "60.0" becomes "60" — what a lifter would type. */
        fun formatInput(value: Float): String =
            if (value % 1f == 0f) value.toInt().toString() else value.formatOneDecimal()
    }
}
