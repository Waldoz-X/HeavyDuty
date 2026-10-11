package com.example.heavyduty.feature.history.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.feature.history.domain.usecase.GetStatsUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetWorkoutHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val getStatsUseCase: GetStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryListUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<HistoryListUiState> = _uiState.whileSubscribed(viewModelScope) {
        observeHistory()
        observeStats()
    }

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<HistoryListUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HistoryListUiEffect> = _uiEffect.receiveAsFlow()

    fun onEvent(event: HistoryListUiEvent) {
        when (event) {
            is HistoryListUiEvent.OnExerciseProgressClick -> {
                viewModelScope.launch {
                    _uiEffect.send(HistoryListUiEffect.NavigateToProgress(event.exerciseId))
                }
            }
        }
    }

    private fun CoroutineScope.observeHistory() {
        launch {
            getWorkoutHistoryUseCase()
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
        }
    }

    private fun CoroutineScope.observeStats() {
        launch {
            getStatsUseCase()
                .catch { _uiState.update { it.copy(loadFailed = true) } }
                .collect { stats ->
                    _uiState.update { it.copy(stats = stats) }
                }
        }
    }
}
