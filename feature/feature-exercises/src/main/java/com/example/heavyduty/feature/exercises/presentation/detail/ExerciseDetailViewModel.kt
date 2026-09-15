package com.example.heavyduty.feature.exercises.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.feature.exercises.domain.usecase.GetExerciseDetailUseCase
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
class ExerciseDetailViewModel @Inject constructor(
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ExerciseDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        loadExercise()
    }

    fun onEvent(event: ExerciseDetailUiEvent) {
        when (event) {
            ExerciseDetailUiEvent.OnNavigateBack -> {
                viewModelScope.launch {
                    _uiEffect.emit(ExerciseDetailUiEffect.NavigateBack)
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
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Error al cargar el ejercicio")
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
