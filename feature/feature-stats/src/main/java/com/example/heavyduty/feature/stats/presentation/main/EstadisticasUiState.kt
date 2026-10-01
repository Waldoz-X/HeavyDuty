package com.example.heavyduty.feature.stats.presentation.main

import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary

data class EstadisticasUiState(
    val days: List<DayChipUi> = emptyList(),
    val selectedDayVolumeByMuscle: Map<MuscleGroup, Float> = emptyMap(),
    val gender: BodyGender = BodyGender.MALE,
    /** Last 30 days vs the 30 before, for the summary at the top. */
    val summary: PeriodSummary = PeriodSummary(),
    val isLoading: Boolean = true,
)

data class DayChipUi(
    val dateMillis: Long,
    val dayLetter: String,
    val dayNumber: String,
    val isSelected: Boolean,
)

sealed interface EstadisticasUiEvent {
    data class OnDaySelected(val dateMillis: Long) : EstadisticasUiEvent
}
