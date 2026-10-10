package com.example.heavyduty.feature.routines.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.R
import com.example.heavyduty.feature.routines.domain.usecase.DeleteRoutineUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val getRoutinesUseCase: GetRoutinesUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<RoutineListUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<RoutineListUiEffect> = _uiEffect.receiveAsFlow()

    init {
        observeRoutines()
    }

    fun onEvent(event: RoutineListUiEvent) {
        when (event) {
            is RoutineListUiEvent.OnRoutineClick -> {
                viewModelScope.launch {
                    _uiEffect.send(RoutineListUiEffect.NavigateToEditor(event.routineId))
                }
            }

            RoutineListUiEvent.OnCreateRoutine -> {
                viewModelScope.launch {
                    _uiEffect.send(RoutineListUiEffect.NavigateToEditor(null))
                }
            }

            is RoutineListUiEvent.OnStartRoutine -> {
                viewModelScope.launch {
                    _uiEffect.send(RoutineListUiEffect.StartRoutine(event.routineId))
                }
            }

            is RoutineListUiEvent.OnDeleteRoutine -> {
                viewModelScope.launch {
                    try {
                        deleteRoutineUseCase(event.routine)
                        _uiEffect.send(RoutineListUiEffect.RoutineDeleted(event.routine.name))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        _uiEffect.send(RoutineListUiEffect.ShowMessage(R.string.routine_error_delete))
                    }
                }
            }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            getRoutinesUseCase()
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = true)
                    }
                }
                .collect { routines ->
                    _uiState.update {
                        it.copy(routines = routines, isLoading = false, loadFailed = false)
                    }
                }
        }
    }
}
