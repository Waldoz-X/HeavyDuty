package com.example.heavyduty.feature.calendar.presentation

import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Recurrence
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.testing.FakeCalendarRepository
import com.example.heavyduty.core.testing.FakeRoutineRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.calendar.domain.usecase.AssignRoutineToDayUseCase
import com.example.heavyduty.feature.calendar.domain.usecase.GetWeekScheduleUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = 1
    private val wednesday = 3
    private val calendar = FakeCalendarRepository(
        listOf(
            CalendarEntry(id = 1, routineId = 10, dayOfWeek = monday),
            CalendarEntry(id = 2, routineId = 11, dayOfWeek = monday), // two routines the same day
            CalendarEntry(id = 3, routineId = 10, dayOfWeek = 5, isActive = false) // switched off
        )
    )
    private val routines = FakeRoutineRepository(listOf(Routine(id = 10, name = "Torso"), Routine(id = 11, name = "Pierna")))

    private fun viewModel() = CalendarViewModel(
        GetWeekScheduleUseCase(calendar),
        AssignRoutineToDayUseCase(calendar),
        calendar,
        routines
    )

    @Test
    fun `the week shows active entries grouped by day`() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(setOf(monday), state.weekSchedule.keys)
        assertEquals(listOf(10L, 11L), state.weekSchedule.getValue(monday).map { it.routineId })
        assertEquals(2, state.routines.size)
    }

    @Test
    fun `assigning a routine adds a weekly entry and confirms it`() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        val effects = mutableListOf<CalendarUiEffect>()
        backgroundScope.launch { vm.uiEffect.collect { effects += it } }
        vm.onEvent(CalendarUiEvent.OnToggleRoutinePicker)

        vm.onEvent(CalendarUiEvent.OnAssignRoutine(routineId = 10, dayOfWeek = wednesday))

        val added = vm.uiState.value.weekSchedule.getValue(wednesday).single()
        assertEquals(10L, added.routineId)
        assertEquals(Recurrence.WEEKLY, added.recurrence)
        assertTrue(added.isActive)
        assertFalse("picker closes", vm.uiState.value.isPickingRoutine)
        assertEquals(listOf(CalendarUiEffect.ShowMessage(R.string.calendar_routine_assigned)), effects)
    }

    @Test
    fun `assigning the same routine twice to a day is refused`() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()
        val effects = mutableListOf<CalendarUiEffect>()
        backgroundScope.launch { vm.uiEffect.collect { effects += it } }

        vm.onEvent(CalendarUiEvent.OnAssignRoutine(routineId = 10, dayOfWeek = monday))

        assertEquals(2, vm.uiState.value.weekSchedule.getValue(monday).size)
        assertEquals(listOf(CalendarUiEffect.ShowMessage(R.string.calendar_already_assigned)), effects)
    }

    @Test
    fun `a switched-off entry doesn't block assigning that routine again`() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        vm.onEvent(CalendarUiEvent.OnAssignRoutine(routineId = 10, dayOfWeek = 5))

        assertEquals(listOf(10L), vm.uiState.value.weekSchedule.getValue(5).map { it.routineId })
    }

    @Test
    fun `removing an entry leaves the rest of the day`() {
        val vm = viewModel()

        vm.onEvent(CalendarUiEvent.OnRemoveEntry(vm.uiState.value.weekSchedule.getValue(monday).first()))

        assertEquals(listOf(11L), vm.uiState.value.weekSchedule.getValue(monday).map { it.routineId })
    }

    @Test
    fun `a schedule that can't be read shows the error, not an empty week`() {
        val broken = object : CalendarRepository by calendar {
            override fun getWeekSchedule(): Flow<Map<Int, List<CalendarEntry>>> = flow { throw IOException("disk") }
        }
        val vm = CalendarViewModel(GetWeekScheduleUseCase(broken), AssignRoutineToDayUseCase(broken), broken, routines)

        val state = vm.uiState.value
        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a message sent while the screen isn't listening is still delivered`() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel()

        // Nobody collects yet — e.g. mid-rotation. A plain SharedFlow would drop this.
        vm.onEvent(CalendarUiEvent.OnAssignRoutine(routineId = 10, dayOfWeek = monday))

        assertEquals(CalendarUiEffect.ShowMessage(R.string.calendar_already_assigned), vm.uiEffect.first())
    }

    @Test
    fun `tapping the selected day again unselects it`() {
        val vm = viewModel()

        vm.onEvent(CalendarUiEvent.OnDaySelect(wednesday))
        assertEquals(wednesday, vm.uiState.value.selectedDay)
        vm.onEvent(CalendarUiEvent.OnDaySelect(wednesday))
        assertNull(vm.uiState.value.selectedDay)
    }
}
