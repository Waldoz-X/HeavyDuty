package com.example.heavyduty.feature.routines.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.routines.domain.usecase.DeleteRoutineUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val getRoutinesUseCase: GetRoutinesUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<RoutineListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        observeRoutines()
    }

    fun onEvent(event: RoutineListUiEvent) {
        when (event) {
            is RoutineListUiEvent.OnRoutineClick -> {
                viewModelScope.launch {
                    _uiEffect.emit(RoutineListUiEffect.NavigateToEditor(event.routineId))
                }
            }
            RoutineListUiEvent.OnCreateRoutine -> {
                viewModelScope.launch {
                    _uiEffect.emit(RoutineListUiEffect.NavigateToEditor(null))
                }
            }
            is RoutineListUiEvent.OnDeleteRoutine -> {
                viewModelScope.launch {
                    try {
                        deleteRoutineUseCase(event.routine)
                        _uiEffect.emit(RoutineListUiEffect.ShowMessage("Routine deleted"))
                    } catch (e: Exception) {
                        _uiEffect.emit(RoutineListUiEffect.ShowMessage("Failed to delete routine"))
                    }
                }
            }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            getRoutinesUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Unknown error")
                    }
                }
                .collect { routines ->
                    _uiState.update {
                        it.copy(routines = routines, isLoading = false, error = null)
                    }
                }
        }
    }
}
