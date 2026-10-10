package com.example.heavyduty.feature.exercises.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.feature.exercises.domain.usecase.GetExerciseDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
class ExerciseDetailViewModel @Inject constructor(
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseDetailUiState(exerciseId = exerciseId))
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<ExerciseDetailUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ExerciseDetailUiEffect> = _uiEffect.receiveAsFlow()

    init {
        loadExercise()
    }

    fun onEvent(event: ExerciseDetailUiEvent) {
        when (event) {
            ExerciseDetailUiEvent.OnNavigateBack -> {
                viewModelScope.launch {
                    _uiEffect.send(ExerciseDetailUiEffect.NavigateBack)
                }
            }

            is ExerciseDetailUiEvent.OnWeightUnitChange -> changeWeightUnit(event.unit)
        }
    }

    private fun changeWeightUnit(unit: WeightUnit) {
        val exercise = _uiState.value.exercise ?: return
        if (exercise.weightUnit == unit) return
        viewModelScope.launch {
            exerciseRepository.updateExercise(exercise.copy(weightUnit = unit))
        }
    }

    private fun loadExercise() {
        viewModelScope.launch {
            getExerciseDetailUseCase(exerciseId)
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = true)
                    }
                }
                .collect { exercise ->
                    _uiState.update {
                        it.copy(exercise = exercise, isLoading = false)
                    }
                }
        }
    }
}
