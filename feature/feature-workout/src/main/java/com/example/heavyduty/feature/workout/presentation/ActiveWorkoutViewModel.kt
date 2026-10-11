package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.nextPrescription
import com.example.heavyduty.core.domain.model.other
import com.example.heavyduty.core.domain.model.toProgressionConfig
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.DiscardWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val discardWorkoutUseCase: DiscardWorkoutUseCase,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val restTimerController: RestTimerController,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock
) : ViewModel() {

    private var exerciseInfoJob: Job? = null
    private var isStartingSession = false

    /** Session whose exercise list is loaded into state; any other id is a different workout. */
    private var loadedSessionId: Long? = null

    /** Routine to start on arrival (from "start today's routine"), until a session exists. */
    private val routineIdToStart: Long? = savedStateHandle.get<Long>(KEY_ROUTINE_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<ActiveWorkoutUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ActiveWorkoutUiEffect> = _uiEffect.receiveAsFlow()

    private val clocks = WorkoutClocks(viewModelScope, clock, _uiState)
    private val exercisePicker = ExercisePicker(viewModelScope, exerciseRepository, _uiState)

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

            // Switches the current set between reps and a hold timer (planks, hangs, carries).
            ActiveWorkoutUiEvent.OnToggleTimedInput -> {
                clocks.resetHold()
                _uiState.update { it.copy(isTimedInput = !it.isTimedInput) }
            }

            ActiveWorkoutUiEvent.OnToggleWorkTimer -> clocks.toggleHold()

            is ActiveWorkoutUiEvent.OnSelectExercise -> selectExercise(event.index)

            is ActiveWorkoutUiEvent.OnDeleteSet -> deleteSet(event.set)

            is ActiveWorkoutUiEvent.OnUndoDeleteSet -> restoreSet(event.set)

            ActiveWorkoutUiEvent.OnLogSet -> logSet()

            ActiveWorkoutUiEvent.OnCompleteWorkout -> completeWorkout()

            ActiveWorkoutUiEvent.OnDiscardWorkout -> discardWorkout()

            ActiveWorkoutUiEvent.OnStartFreeWorkout -> startFreeWorkout()

            is ActiveWorkoutUiEvent.OnStartRoutine -> startWorkoutFromRoutine(event.routineId)

            ActiveWorkoutUiEvent.OnNavigateToRoutines -> {
                viewModelScope.launch { _uiEffect.send(ActiveWorkoutUiEffect.NavigateToRoutines) }
            }

            ActiveWorkoutUiEvent.OnToggleExercisePicker -> exercisePicker.toggle()

            is ActiveWorkoutUiEvent.OnExercisePickerQueryChange -> exercisePicker.onQueryChange(event.query)

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
                userPreferencesRepository.preferredEffortScale
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
        clocks.stop()
        exerciseInfoJob?.cancel()
        _uiState.update { current ->
            ActiveWorkoutUiState(
                isLoading = false,
                routines = current.routines,
                restTimer = current.restTimer,
                effortTrackingEnabled = current.effortTrackingEnabled,
                preferredEffortScale = current.preferredEffortScale
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
                    effortInput = savedStateHandle.get<String>(KEY_EFFORT_INPUT).takeIf { sameSession }.orEmpty()
                )
            }
            clocks.startSession(session.startTime)
            loadCurrentExerciseInfo()
        } else {
            _uiState.update {
                it.copy(
                    sessionName = session.sessionName,
                    completedSets = sets,
                    routineExercises = mergeLoggedExercises(it.routineExercises, sets)
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
                previousSets = emptyMap()
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
                    previousSets = previousSets
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
                weightInput = suggestion.weightKg?.takeIf { kg -> kg > 0f }?.kgToRounded(unit)?.let(::formatInput) ?: it.weightInput
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
                weightInput = ""
            )
        }
        viewModelScope.launch {
            try {
                if (!state.allowAddingExercises && routineExercise.id > 0) {
                    routineRepository.updateRoutineExercise(updated.copy(exercise = null))
                } else {
                    routineExercise.exercise?.let { exerciseRepository.updateExercise(it.copy(weightUnit = newUnit)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Only the remembered default failed; this session still uses the new unit.
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
                    startTime = clock.nowMillis()
                )
            }
        }
    }

    private fun startFreeWorkout() {
        if (_uiState.value.isActive || isStartingSession) return
        isStartingSession = true
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
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
            isStartingSession = false
            _uiState.update { it.copy(isLoading = false) }
            showMessage(R.string.workout_error_start)
        }
    }

    private fun logSet() {
        val state = _uiState.value
        if (state.isLoggingSet) return
        val sessionId = state.sessionId ?: return
        val exercise = state.currentExercise ?: return
        val entry = state.setEntry()
            ?: return showMessage(if (state.isTimedInput) R.string.workout_error_enter_time else R.string.workout_error_enter_reps)
        val set = WorkoutSet(
            sessionId = sessionId,
            exerciseId = exercise.exerciseId,
            exercise = exercise.exercise,
            setNumber = state.currentSetNumber,
            reps = entry.reps,
            weight = entry.weightKg,
            durationSecs = entry.durationSecs,
            isCompleted = true,
            isWarmup = state.isWarmupInput,
            effort = entry.effort,
            weightUnit = state.currentUnit
        )

        _uiState.update { it.copy(isLoggingSet = true) }
        viewModelScope.launch {
            try {
                val setId = logSetUseCase(
                    sessionId = sessionId,
                    exerciseId = set.exerciseId,
                    setNumber = set.setNumber,
                    reps = set.reps,
                    weight = set.weight,
                    durationSecs = set.durationSecs,
                    isWarmup = set.isWarmup,
                    effort = set.effort,
                    restSeconds = exercise.restBetweenSetsSecs,
                    weightUnit = state.currentUnit
                )
                val loggedSet = set.copy(id = setId, completedAt = clock.nowMillis())
                clocks.resetHold()
                // Shown right away; the database emission that follows carries the same set.
                updateInputs {
                    it.copy(
                        completedSets = it.completedSets.filter { logged -> logged.id != setId } + loggedSet,
                        repsInput = "",
                        weightInput = "",
                        isWarmupInput = false,
                        effortInput = ""
                    )
                }
                celebrateIfRecord(loggedSet, earlierToday = state.setsFor(exercise.exerciseId), unit = state.currentUnit)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showMessage(R.string.workout_error_log_set)
            } finally {
                _uiState.update { it.copy(isLoggingSet = false) }
            }
        }
    }

    /** Celebrates a set that beats every earlier set of the exercise (see [newRecordEstimate]). */
    private suspend fun celebrateIfRecord(set: WorkoutSet, earlierToday: List<WorkoutSet>, unit: WeightUnit) {
        // Only a bonus: if history can't be read, the set is still logged, just not celebrated.
        val history = try {
            historyExcludingCurrentSession(set.exerciseId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        val estimate = newRecordEstimate(set, earlierSets = history + earlierToday) ?: return
        _uiEffect.send(
            ActiveWorkoutUiEffect.PersonalRecord(
                exerciseName = set.exercise?.displayName.orEmpty(),
                estimatedOneRepMaxKg = estimate,
                unit = unit
            )
        )
    }

    private fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(set)
                _uiState.update { it.copy(completedSets = it.completedSets.filter { s -> s.id != set.id }) }
                _uiEffect.send(ActiveWorkoutUiEffect.SetDeleted(set))
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
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
                _uiEffect.send(ActiveWorkoutUiEffect.WorkoutCompleted)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showMessage(R.string.workout_error_finish)
            }
        }
    }

    private fun discardWorkout() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            try {
                discardWorkoutUseCase(sessionId)
                restTimerController.skipRest()
                _uiEffect.send(ActiveWorkoutUiEffect.WorkoutDiscarded)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showMessage(R.string.workout_error_discard)
            }
        }
    }

    // ---------------------------------------------------------------- free workout exercises

    private fun addExerciseToSession(exercise: Exercise) {
        val state = _uiState.value
        val existingIndex = state.routineExercises.indexOfFirst { it.exerciseId == exercise.id }
        exercisePicker.close()
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
                routineExercises = it.routineExercises + freeExercise(exercise, it.routineExercises.size)
            )
        }
        val addedIndex = _uiState.value.routineExercises.lastIndex
        // The first exercise of a free workout lands on the index already "selected" (0), where
        // selectExercise would do nothing — its history (last time's sets, hints) still has to load.
        if (addedIndex == _uiState.value.currentExerciseIndex) loadCurrentExerciseInfo() else selectExercise(addedIndex)
    }

    private fun showMessage(messageResId: Int) {
        viewModelScope.launch { _uiEffect.send(ActiveWorkoutUiEffect.ShowMessage(messageResId)) }
    }

    override fun onCleared() {
        super.onCleared()
        clocks.stop()
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

        /** "62.5" stays "62.5", "60.0" becomes "60" — what a lifter would type. */
        fun formatInput(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else value.formatOneDecimal()
    }
}
