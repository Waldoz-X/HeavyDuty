package com.example.heavyduty.feature.history.presentation.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.history.domain.usecase.GetExerciseProgressUseCase
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
class ProgressViewModel @Inject constructor(
    private val getExerciseProgressUseCase: GetExerciseProgressUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ProgressUiState(exerciseId = exerciseId))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProgressUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        loadProgress()
    }

    fun onEvent(event: ProgressUiEvent) {
        when (event) {
            ProgressUiEvent.OnNavigateBack -> {
                viewModelScope.launch { _uiEffect.emit(ProgressUiEffect.NavigateBack) }
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            getExerciseProgressUseCase(exerciseId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { sets ->
                    _uiState.update { it.copy(sets = sets, isLoading = false) }
                }
        }
    }
}
