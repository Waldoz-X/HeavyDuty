package com.example.heavyduty.feature.calendar.presentation

import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Routine

data class CalendarUiState(
    val weekSchedule: Map<Int, List<CalendarEntry>> = emptyMap(),
    val routines: List<Routine> = emptyList(),
    val selectedDay: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface CalendarUiEvent {
    data class OnDaySelect(val dayOfWeek: Int) : CalendarUiEvent
    data class OnRoutineClick(val routineId: Long) : CalendarUiEvent
    data class OnAssignRoutine(val routineId: Long, val dayOfWeek: Int) : CalendarUiEvent
    data class OnRemoveEntry(val entry: CalendarEntry) : CalendarUiEvent
}

sealed interface CalendarUiEffect {
    data class NavigateToRoutine(val routineId: Long) : CalendarUiEffect
    data class ShowMessage(val message: String) : CalendarUiEffect
}
