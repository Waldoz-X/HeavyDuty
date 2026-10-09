package com.example.heavyduty.feature.calendar.presentation

import com.example.heavyduty.core.common.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.feature.calendar.domain.usecase.AssignRoutineToDayUseCase
import com.example.heavyduty.feature.calendar.domain.usecase.DuplicateCalendarEntryException
import com.example.heavyduty.feature.calendar.domain.usecase.GetWeekScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
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

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<CalendarUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<CalendarUiEffect> = _uiEffect.receiveAsFlow()

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
                    _uiEffect.send(CalendarUiEffect.NavigateToRoutine(event.routineId))
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
                        _uiEffect.send(CalendarUiEffect.ShowMessage(R.string.calendar_routine_assigned))
                    } catch (_: DuplicateCalendarEntryException) {
                        _uiEffect.send(CalendarUiEffect.ShowMessage(R.string.calendar_already_assigned))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        _uiEffect.send(CalendarUiEffect.ShowMessage(R.string.calendar_assign_error))
                    }
                }
            }
            is CalendarUiEvent.OnRemoveEntry -> {
                viewModelScope.launch {
                    try {
                        calendarRepository.deleteEntry(event.entry)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiEffect.send(CalendarUiEffect.ShowMessage(R.string.calendar_remove_error))
                    }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            getWeekScheduleUseCase()
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { schedule ->
                    _uiState.update { it.copy(weekSchedule = schedule, isLoading = false) }
                }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            routineRepository.getAllRoutines()
                .catch { _uiState.update { it.copy(loadFailed = true) } }
                .collect { routines ->
                    _uiState.update { it.copy(routines = routines) }
                }
        }
    }
}
