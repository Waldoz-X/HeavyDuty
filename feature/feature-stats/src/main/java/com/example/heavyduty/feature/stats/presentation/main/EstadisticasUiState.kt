package com.example.heavyduty.feature.stats.presentation.main

data class EstadisticasUiState(
    val days: List<DayChipUi> = emptyList(),
    val selectedDayVolumeByMuscle: Map<String, Float> = emptyMap(),
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
