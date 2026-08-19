package com.example.heavyduty.feature.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.feature.calendar.domain.usecase.AssignRoutineToDayUseCase
import com.example.heavyduty.feature.calendar.domain.usecase.GetWeekScheduleUseCase
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
class CalendarViewModel @Inject constructor(
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val assignRoutineToDayUseCase: AssignRoutineToDayUseCase,
    private val calendarRepository: CalendarRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CalendarUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        observeSchedule()
        observeRoutines()
    }

    fun onEvent(event: CalendarUiEvent) {
        when (event) {
            is CalendarUiEvent.OnDaySelect -> {
                val newDay = if (_uiState.value.selectedDay == event.dayOfWeek) null else event.dayOfWeek
                _uiState.update { it.copy(selectedDay = newDay, isPickingRoutine = false) }
            }
            is CalendarUiEvent.OnRoutineClick -> {
                viewModelScope.launch {
                    _uiEffect.emit(CalendarUiEffect.NavigateToRoutine(event.routineId))
                }
            }
            CalendarUiEvent.OnToggleRoutinePicker -> {
                _uiState.update { it.copy(isPickingRoutine = !it.isPickingRoutine) }
            }
            is CalendarUiEvent.OnAssignRoutine -> {
                viewModelScope.launch {
                    try {
                        assignRoutineToDayUseCase(event.routineId, event.dayOfWeek)
                        _uiState.update { it.copy(isPickingRoutine = false) }
                        _uiEffect.emit(CalendarUiEffect.ShowMessage("Rutina asignada"))
                    } catch (e: Exception) {
                        _uiEffect.emit(CalendarUiEffect.ShowMessage("Error al asignar la rutina"))
                    }
                }
            }
            is CalendarUiEvent.OnRemoveEntry -> {
                viewModelScope.launch {
                    try {
                        calendarRepository.deleteEntry(event.entry)
                    } catch (e: Exception) {
                        _uiEffect.emit(CalendarUiEffect.ShowMessage("Error al quitar"))
                    }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            getWeekScheduleUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { schedule ->
                    _uiState.update { it.copy(weekSchedule = schedule, isLoading = false) }
                }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            routineRepository.getAllRoutines()
                .catch { /* silently fail */ }
                .collect { routines ->
                    _uiState.update { it.copy(routines = routines) }
                }
        }
    }
}
