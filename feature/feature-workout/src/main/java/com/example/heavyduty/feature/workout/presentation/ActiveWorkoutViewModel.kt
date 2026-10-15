package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.DiscardWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the active workout. The in-progress session is observed straight from the database,
 * so this screen is correct no matter how many times it's (re)created — a second instance
 * (another nav entry, process death) shows the same logged sets instead of a stale copy.
 * Only what the lifter is typing, and which exercise is on screen, lives here.
 *
 * The ViewModel follows the session and the list of exercises; the rest is in its parts:
 * [WorkoutLifecycle] starts and ends the workout, [SetLogger] logs and deletes sets,
 * [CurrentExerciseController] reads the exercise's history, [WorkoutDraft] keeps what's typed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    startWorkoutUseCase: StartWorkoutUseCase,
    logSetUseCase: LogSetUseCase,
    completeWorkoutUseCase: CompleteWorkoutUseCase,
    discardWorkoutUseCase: DiscardWorkoutUseCase,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    trainingHistoryRepository: TrainingHistoryRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val restTimerController: RestTimerController,
    private val savedStateHandle: SavedStateHandle,
    clock: Clock
) : ViewModel() {

    /** Session whose exercise list is loaded into state; any other id is a different workout. */
    private var loadedSessionId: Long? = null

    /** Routine to start on arrival (from "start today's routine"), until a session exists. */
    private val routineIdToStart: Long? = savedStateHandle.get<Long>(KEY_ROUTINE_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val effects = WorkoutEffects(viewModelScope)
    val uiEffect: Flow<ActiveWorkoutUiEffect> = effects.flow

    private val clocks = WorkoutClocks(viewModelScope, clock, _uiState)
    private val exercisePicker = ExercisePickerController(viewModelScope, exerciseRepository, _uiState)
    private val draft = WorkoutDraft(savedStateHandle, _uiState)
    private val currentExercise = CurrentExerciseController(
        viewModelScope,
        _uiState,
        draft,
        trainingHistoryRepository,
        routineRepository,
        exerciseRepository
    )
    private val setLogger = SetLogger(
        viewModelScope,
        _uiState,
        draft,
        effects,
        clocks,
        currentExercise,
        logSetUseCase,
        workoutRepository,
        clock
    )
    private val lifecycle = WorkoutLifecycle(
        viewModelScope,
        _uiState,
        effects,
        startWorkoutUseCase,
        completeWorkoutUseCase,
        discardWorkoutUseCase,
        routineRepository,
        restTimerController,
        clock
    )

    init {
        observeRoutinesSettingsAndRest()
        observeActiveSession()
    }

    fun onEvent(event: ActiveWorkoutUiEvent) {
        when (event) {
            is ActiveWorkoutUiEvent.OnRepsChange -> draft.update { it.copy(repsInput = event.reps) }

            is ActiveWorkoutUiEvent.OnWeightChange -> draft.update { it.copy(weightInput = event.weight) }

            ActiveWorkoutUiEvent.OnToggleWarmupInput -> _uiState.update { it.copy(isWarmupInput = !it.isWarmupInput) }

            is ActiveWorkoutUiEvent.OnEffortInputChange -> draft.update { it.copy(effortInput = event.value) }

            // Switches the current set between reps and a hold timer (planks, hangs, carries).
            ActiveWorkoutUiEvent.OnToggleTimedInput -> {
                clocks.resetHold()
                _uiState.update { it.copy(isTimedInput = !it.isTimedInput) }
            }

            ActiveWorkoutUiEvent.OnToggleWorkTimer -> clocks.toggleHold()

            is ActiveWorkoutUiEvent.OnSelectExercise -> selectExercise(event.index)

            is ActiveWorkoutUiEvent.OnDeleteSet -> setLogger.deleteSet(event.set)

            is ActiveWorkoutUiEvent.OnUndoDeleteSet -> setLogger.restoreSet(event.set)

            ActiveWorkoutUiEvent.OnLogSet -> setLogger.logSet()

            ActiveWorkoutUiEvent.OnCompleteWorkout -> lifecycle.complete()

            ActiveWorkoutUiEvent.OnDiscardWorkout -> lifecycle.discard()

            ActiveWorkoutUiEvent.OnStartFreeWorkout -> lifecycle.startFree()

            is ActiveWorkoutUiEvent.OnStartRoutine -> lifecycle.startFromRoutine(event.routineId)

            ActiveWorkoutUiEvent.OnNavigateToRoutines -> effects.post(ActiveWorkoutUiEffect.NavigateToRoutines)

            ActiveWorkoutUiEvent.OnToggleExercisePicker -> exercisePicker.toggle()

            is ActiveWorkoutUiEvent.OnExercisePickerQueryChange -> exercisePicker.onQueryChange(event.query)

            is ActiveWorkoutUiEvent.OnAddExercise -> addExerciseToSession(event.exercise)

            ActiveWorkoutUiEvent.OnUseSuggestion -> currentExercise.useSuggestion()

            is ActiveWorkoutUiEvent.OnAdjustRest -> restTimerController.adjustRest(event.deltaSeconds)

            ActiveWorkoutUiEvent.OnSkipRest -> restTimerController.skipRest()

            ActiveWorkoutUiEvent.OnToggleUnit -> currentExercise.toggleUnit()

            ActiveWorkoutUiEvent.OnConfirmUnusualWeight -> setLogger.confirmUnusualWeight()

            ActiveWorkoutUiEvent.OnDismissUnusualWeight -> setLogger.dismissUnusualWeight()
        }
    }

    // ---------------------------------------------------------------- observation

    /** What the screen shows around the workout: the routines to start, effort settings, the rest timer. */
    private fun observeRoutinesSettingsAndRest() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.effortTrackingEnabled,
                userPreferencesRepository.preferredEffortScale
            ) { enabled, scale -> enabled to scale }.collect { (enabled, scale) ->
                _uiState.update { it.copy(effortTrackingEnabled = enabled, preferredEffortScale = scale) }
            }
        }
        viewModelScope.launch {
            routineRepository.getAllRoutines().collect { routines ->
                _uiState.update { it.copy(routines = routines) }
            }
        }
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
            lifecycle.startFromRoutine(routineId)
            return
        }
        if (lifecycle.isStarting) return
        loadedSessionId = null
        clocks.stop()
        currentExercise.cancel()
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
        lifecycle.onSessionActive()
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
                val loaded = it.copy(
                    sessionId = session.id,
                    sessionName = session.sessionName,
                    sessionStartTime = session.startTime,
                    isActive = true,
                    isLoading = false,
                    allowAddingExercises = session.routineId == null,
                    routineExercises = exercises,
                    completedSets = sets,
                    currentExerciseIndex = index
                )
                // What was typed but not logged yet, if Android closed the app meanwhile.
                draft.restore(loaded, sameSession)
            }
            clocks.startSession(session.startTime)
            currentExercise.loadInfo()
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

    // ---------------------------------------------------------------- exercises on screen

    /**
     * Switching exercises only changes what's on screen: logged sets stay in the database,
     * and neither the session clock, the rest timer nor a running hold timer is touched.
     */
    private fun selectExercise(index: Int) {
        val state = _uiState.value
        if (index == state.currentExerciseIndex || index !in state.routineExercises.indices) return
        savedStateHandle[KEY_EXERCISE_INDEX] = index
        draft.update {
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
        currentExercise.loadInfo()
    }

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
        if (addedIndex == _uiState.value.currentExerciseIndex) currentExercise.loadInfo() else selectExercise(addedIndex)
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
    }
}
