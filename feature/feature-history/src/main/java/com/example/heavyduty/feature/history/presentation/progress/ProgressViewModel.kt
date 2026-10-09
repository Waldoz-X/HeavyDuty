package com.example.heavyduty.feature.history.presentation.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.feature.history.domain.usecase.GetExercisePersonalRecordsUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetExerciseProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getExerciseProgressUseCase: GetExerciseProgressUseCase,
    private val getExercisePersonalRecordsUseCase: GetExercisePersonalRecordsUseCase,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ProgressUiState(exerciseId = exerciseId))
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<ProgressUiState> = _uiState.whileSubscribed(viewModelScope) {
        loadProgress()
    }

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<ProgressUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ProgressUiEffect> = _uiEffect.receiveAsFlow()

    fun onEvent(event: ProgressUiEvent) {
        when (event) {
            ProgressUiEvent.OnNavigateBack -> {
                viewModelScope.launch { _uiEffect.send(ProgressUiEffect.NavigateBack) }
            }
        }
    }

    private fun CoroutineScope.loadProgress() {
        launch {
            combine(
                getExerciseProgressUseCase(exerciseId),
                getExercisePersonalRecordsUseCase(exerciseId),
                exerciseRepository.getExerciseById(exerciseId),
            ) { sets, records, exercise -> Triple(sets, records, exercise?.weightUnit ?: WeightUnit.KG) }
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { (sets, records, weightUnit) ->
                    _uiState.update {
                        it.copy(sets = sets, personalRecords = records, weightUnit = weightUnit, isLoading = false)
                    }
                }
        }
    }
}
