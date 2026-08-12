package com.example.heavyduty.feature.routines.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Routine
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")

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
            is RoutineEditorUiEvent.OnRemoveExercise -> {
                _uiState.update { state ->
                    state.copy(exercises = state.exercises.filter { it.id != event.routineExercise.id })
                }
            }
            RoutineEditorUiEvent.OnSave -> saveRoutine()
            RoutineEditorUiEvent.OnNavigateBack -> {
                viewModelScope.launch { _uiEffect.emit(RoutineEditorUiEffect.NavigateBack) }
            }
            RoutineEditorUiEvent.OnNavigateToExercises -> {
                viewModelScope.launch { _uiEffect.emit(RoutineEditorUiEffect.NavigateToExercises) }
            }
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
                _uiEffect.emit(RoutineEditorUiEffect.ShowMessage("Routine name is required"))
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val routine = Routine(
                    id = state.routineId ?: 0,
                    name = state.name,
                    description = state.description,
                    type = state.type,
                    exercises = state.exercises,
                )
                if (state.isEditMode) {
                    updateRoutineUseCase(routine)
                } else {
                    createRoutineUseCase(routine)
                }
                _uiEffect.emit(RoutineEditorUiEffect.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.emit(RoutineEditorUiEffect.ShowMessage("Failed to save routine"))
            }
        }
    }
}
