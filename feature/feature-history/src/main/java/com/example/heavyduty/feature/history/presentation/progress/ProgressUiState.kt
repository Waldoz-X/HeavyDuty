package com.example.heavyduty.feature.history.presentation.progress

import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.feature.history.domain.model.ExercisePersonalRecords

data class ProgressUiState(
    val exerciseId: Long = 0,
    val sets: List<WorkoutSet> = emptyList(),
    val personalRecords: ExercisePersonalRecords = ExercisePersonalRecords(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface ProgressUiEvent {
    data object OnNavigateBack : ProgressUiEvent
}

sealed interface ProgressUiEffect {
    data object NavigateBack : ProgressUiEffect
}
