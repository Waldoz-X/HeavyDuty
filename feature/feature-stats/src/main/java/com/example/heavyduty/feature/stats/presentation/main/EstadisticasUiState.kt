package com.example.heavyduty.feature.stats.presentation.main

import com.example.heavyduty.core.ui.components.BodyGender

data class EstadisticasUiState(
    val days: List<DayChipUi> = emptyList(),
    val selectedDayVolumeByMuscle: Map<String, Float> = emptyMap(),
    val gender: BodyGender = BodyGender.MALE,
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
