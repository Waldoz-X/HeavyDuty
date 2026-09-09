package com.example.heavyduty.feature.routines.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.pairWithNext
import com.example.heavyduty.core.domain.model.unpair
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.feature.routines.domain.usecase.CreateRoutineUseCase
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

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val updateRoutineUseCase: UpdateRoutineUseCase,
    private val getRoutineWithExercisesUseCase: GetRoutineWithExercisesUseCase,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")

    private var cachedExercises: List<Exercise>? = null

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<RoutineEditorUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        if (routineId != null && routineId > 0) {
            loadRoutine(routineId)
        }
    }

    fun onEvent(event: RoutineEditorUiEvent) {
        when (event) {
            is RoutineEditorUiEvent.OnNameChange -> {
                _uiState.update { it.copy(name = event.name) }
            }
            is RoutineEditorUiEvent.OnDescriptionChange -> {
                _uiState.update { it.copy(description = event.description) }
            }
            is RoutineEditorUiEvent.OnTypeChange -> {
                _uiState.update { it.copy(type = event.type) }
            }
            RoutineEditorUiEvent.OnToggleExercisePicker -> toggleExercisePicker()
            is RoutineEditorUiEvent.OnExercisePickerQueryChange -> onExercisePickerQueryChange(event.query)
            is RoutineEditorUiEvent.OnAddExercise -> addExercise(event.exercise)
            is RoutineEditorUiEvent.OnUpdateExercise -> updateExercise(event.index, event.updated)
            is RoutineEditorUiEvent.OnMoveExercise -> moveExercise(event.index, event.direction)
            is RoutineEditorUiEvent.OnRemoveExercise -> {
                _uiState.update { state ->
                    state.copy(exercises = state.exercises.filter { it !== event.routineExercise })
                }
            }
            is RoutineEditorUiEvent.OnPairWithNext -> {
                _uiState.update { state -> state.copy(exercises = state.exercises.pairWithNext(event.index)) }
            }
            is RoutineEditorUiEvent.OnUnpair -> {
                _uiState.update { state -> state.copy(exercises = state.exercises.unpair(event.index)) }
            }
            RoutineEditorUiEvent.OnSave -> saveRoutine()
            RoutineEditorUiEvent.OnNavigateBack -> {
                viewModelScope.launch { _uiEffect.emit(RoutineEditorUiEffect.NavigateBack) }
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

    private fun addExercise(exercise: Exercise) {
        _uiState.update { state ->
            val newEntry = RoutineExercise(
                exerciseId = exercise.id,
                exercise = exercise,
                orderIndex = state.exercises.size,
            )
            state.copy(
                exercises = state.exercises + newEntry,
                isPickingExercise = false,
                exercisePickerQuery = "",
                exercisePickerResults = emptyList(),
            )
        }
    }

    private fun updateExercise(index: Int, updated: RoutineExercise) {
        _uiState.update { state ->
            if (index !in state.exercises.indices) return@update state
            state.copy(
                exercises = state.exercises.toMutableList().apply { this[index] = updated },
            )
        }
    }

    private fun moveExercise(index: Int, direction: Int) {
        _uiState.update { state ->
            val target = index + direction
            if (index !in state.exercises.indices || target !in state.exercises.indices) return@update state
            val reordered = state.exercises.toMutableList()
            val moved = reordered.removeAt(index)
            reordered.add(target, moved)
            state.copy(exercises = reordered)
        }
    }

    private fun loadRoutine(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getRoutineWithExercisesUseCase(id)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
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
                        )
                    }
                }
        }
    }

    private fun saveRoutine() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(RoutineEditorUiEffect.ShowMessage("El nombre de la rutina es obligatorio"))
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val updatedExercises = state.exercises.mapIndexed { index, exercise ->
                    exercise.copy(orderIndex = index)
                }
                
                val routine = state.originalRoutine?.copy(
                    name = state.name,
                    description = state.description,
                    type = state.type,
                    exercises = updatedExercises,
                    updatedAt = System.currentTimeMillis()
                ) ?: Routine(
                    id = state.routineId ?: 0,
                    name = state.name,
                    description = state.description,
                    type = state.type,
                    exercises = updatedExercises,
                )
                
                if (state.isEditMode) {
                    updateRoutineUseCase(routine)
                } else {
                    createRoutineUseCase(routine)
                }
                _uiEffect.emit(RoutineEditorUiEffect.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.emit(RoutineEditorUiEffect.ShowMessage("Error al guardar la rutina"))
            }
        }
    }
}
