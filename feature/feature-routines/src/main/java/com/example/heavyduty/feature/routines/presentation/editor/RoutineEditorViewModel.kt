package com.example.heavyduty.feature.routines.presentation.editor

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.displayName
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.pairWithNext
import com.example.heavyduty.core.domain.model.unpair
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.feature.routines.domain.usecase.CreateRoutineUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutineVolumeHistoryUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutineWithExercisesUseCase
import com.example.heavyduty.feature.routines.domain.usecase.UpdateRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Key used to hand exercise ids back from the exercise-picker route to this destination's
 * [androidx.navigation.NavBackStackEntry.savedStateHandle] (the Navigation-Compose result
 * pattern). That handle belongs to the nav entry — each ViewModel gets its own, separate
 * SavedStateHandle — so the screen reads the result there and forwards it as
 * [RoutineEditorUiEvent.OnExercisesPicked].
 */
const val SELECTED_EXERCISE_IDS_KEY = "selected_exercise_ids"

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val updateRoutineUseCase: UpdateRoutineUseCase,
    private val getRoutineWithExercisesUseCase: GetRoutineWithExercisesUseCase,
    private val getRoutineVolumeHistoryUseCase: GetRoutineVolumeHistoryUseCase,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<RoutineEditorUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    /** Last removed exercise and where it was, for "Deshacer". */
    private var lastRemoved: Pair<Int, RoutineExercise>? = null

    /** Temporary ids for rows added in this session (see [RoutineEditorUiState]). */
    private var nextTemporaryId = -1L

    init {
        if (routineId != null && routineId > 0) {
            loadRoutine(routineId)
            viewModelScope.launch {
                getRoutineVolumeHistoryUseCase(routineId).collect { history ->
                    _uiState.update { it.copy(volumeHistory = history) }
                }
            }
        }
    }

    fun onEvent(event: RoutineEditorUiEvent) {
        when (event) {
            is RoutineEditorUiEvent.OnNameChange -> _uiState.update {
                it.copy(name = event.name, showNameError = it.showNameError && event.name.isBlank())
            }
            is RoutineEditorUiEvent.OnDescriptionChange -> _uiState.update { it.copy(description = event.description) }
            is RoutineEditorUiEvent.OnTypeChange -> _uiState.update { it.copy(type = event.type) }
            is RoutineEditorUiEvent.OnUpdateExercise -> updateExercise(event.index, event.updated)
            is RoutineEditorUiEvent.OnMoveExercise -> moveExercise(event.from, event.to)
            is RoutineEditorUiEvent.OnRemoveExercise -> removeExercise(event.index)
            RoutineEditorUiEvent.OnUndoRemove -> undoRemove()
            is RoutineEditorUiEvent.OnDuplicateExercise -> duplicateExercise(event.index)
            is RoutineEditorUiEvent.OnPairWithNext -> _uiState.update { it.copy(exercises = it.exercises.pairWithNext(event.index)) }
            is RoutineEditorUiEvent.OnUnpair -> _uiState.update { it.copy(exercises = it.exercises.unpair(event.index)) }
            is RoutineEditorUiEvent.OnOpenExercise -> _uiState.update { it.copy(editingIndex = event.index) }
            RoutineEditorUiEvent.OnCloseExercise -> _uiState.update { it.copy(editingIndex = null) }
            is RoutineEditorUiEvent.OnSetAllUnits -> setAllUnits(event.unit)
            RoutineEditorUiEvent.OnSave -> saveRoutine()
            is RoutineEditorUiEvent.OnExercisesPicked -> viewModelScope.launch { addExercises(event.exerciseIds) }
        }
    }

    private suspend fun addExercises(ids: List<Long>) {
        val exercises = ids.mapNotNull { exerciseRepository.getExerciseById(it).firstOrNull() }
        if (exercises.isEmpty()) return
        _uiState.update { state ->
            state.copy(exercises = state.exercises + exercises.map { newRoutineExercise(it) })
        }
        _uiEffect.emit(RoutineEditorUiEffect.ExercisesAdded(exercises.size))
    }

    /**
     * A freshly added exercise: sensible defaults (3 × 10, 90 s rest) and — for bodyweight
     * moves — no load, so a routine can be built by picking exercises alone.
     */
    private fun newRoutineExercise(exercise: Exercise) = RoutineExercise(
        id = nextTemporaryId--,
        exerciseId = exercise.id,
        exercise = exercise,
        targetSets = DEFAULT_SETS,
        targetReps = DEFAULT_REPS,
        restBetweenSetsSecs = DEFAULT_REST_SECONDS,
    )

    private fun updateExercise(index: Int, updated: RoutineExercise) {
        _uiState.update { state ->
            if (index !in state.exercises.indices) return@update state
            state.copy(exercises = state.exercises.toMutableList().apply { this[index] = updated })
        }
    }

    private fun moveExercise(from: Int, to: Int) {
        _uiState.update { state ->
            if (from !in state.exercises.indices || to !in state.exercises.indices || from == to) return@update state
            val reordered = state.exercises.toMutableList().apply { add(to, removeAt(from)) }
            state.copy(exercises = reordered)
        }
    }

    private fun removeExercise(index: Int) {
        val removed = _uiState.value.exercises.getOrNull(index) ?: return
        lastRemoved = index to removed
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.filterIndexed { i, _ -> i != index },
                editingIndex = null,
            )
        }
        viewModelScope.launch {
            _uiEffect.emit(RoutineEditorUiEffect.ShowUndoRemove(removed.exercise?.displayName.orEmpty()))
        }
    }

    private fun undoRemove() {
        val (index, exercise) = lastRemoved ?: return
        lastRemoved = null
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            list.add(index.coerceIn(0, list.size), exercise)
            state.copy(exercises = list)
        }
    }

    private fun duplicateExercise(index: Int) {
        _uiState.update { state ->
            val original = state.exercises.getOrNull(index) ?: return@update state
            // A copy is its own entry: new id, and not part of the original's superset.
            val copy = original.copy(id = nextTemporaryId--, supersetGroupId = null)
            state.copy(exercises = state.exercises.toMutableList().apply { add(index + 1, copy) })
        }
    }

    private fun setAllUnits(unit: WeightUnit) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.map { it.copy(weightUnit = unit) })
        }
    }

    private fun loadRoutine(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getRoutineWithExercisesUseCase(id)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .firstOrNull()?.let { routine ->
                    _uiState.update {
                        it.copy(
                            originalRoutine = routine,
                            routineId = routine.id,
                            name = routine.name,
                            description = routine.description,
                            type = routine.type,
                            exercises = routine.exercises,
                            isEditMode = true,
                            isLoading = false,
                            savedSnapshot = EditorSnapshot(routine.name, routine.description, routine.type, routine.exercises),
                        )
                    }
                }
        }
    }

    private fun saveRoutine() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(showNameError = true) }
            viewModelScope.launch { _uiEffect.emit(RoutineEditorUiEffect.ShowMessage(R.string.routine_error_name_required)) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val exercises = state.exercises.mapIndexed { index, exercise ->
                    exercise.copy(id = exercise.id.coerceAtLeast(0), orderIndex = index)
                }
                val routine = state.originalRoutine?.copy(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    type = state.type,
                    exercises = exercises,
                    updatedAt = clock.nowMillis(),
                ) ?: Routine(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    type = state.type,
                    exercises = exercises,
                )
                if (state.isEditMode) updateRoutineUseCase(routine) else createRoutineUseCase(routine)
                _uiState.update {
                    it.copy(isSaving = false, savedSnapshot = EditorSnapshot(it.name, it.description, it.type, it.exercises))
                }
                _uiEffect.emit(RoutineEditorUiEffect.NavigateBack)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.emit(RoutineEditorUiEffect.ShowMessage(R.string.routine_error_save))
            }
        }
    }

    private companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 10
        const val DEFAULT_REST_SECONDS = 90
    }
}
