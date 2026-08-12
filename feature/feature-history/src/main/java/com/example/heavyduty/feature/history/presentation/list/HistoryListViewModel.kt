package com.example.heavyduty.feature.history.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.history.domain.usecase.GetStatsUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetWorkoutHistoryUseCase
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
class HistoryListViewModel @Inject constructor(
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val getStatsUseCase: GetStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryListUiState())
    val uiState: StateFlow<HistoryListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HistoryListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        observeHistory()
        observeStats()
    }

    fun onEvent(event: HistoryListUiEvent) {
        when (event) {
            is HistoryListUiEvent.OnSessionClick -> { /* future detail */ }
            is HistoryListUiEvent.OnExerciseProgressClick -> {
                viewModelScope.launch {
                    _uiEffect.emit(HistoryListUiEffect.NavigateToProgress(event.exerciseId))
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            getWorkoutHistoryUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            getStatsUseCase()
                .catch { /* silently fail stats */ }
                .collect { stats ->
                    _uiState.update { it.copy(stats = stats) }
                }
        }
    }
}
