package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.stats.domain.usecase.GetTrainingActivityHeatmapUseCase
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapaActividadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 10, 0)
    }.timeInMillis

    private val now = at(2026, Calendar.OCTOBER, 10)

    private fun viewModel(vararg starts: Long): MapaActividadViewModel {
        val sessions = starts.mapIndexed { i, start ->
            WorkoutSession(id = i + 1L, startTime = start, totalDurationSecs = 3600, status = WorkoutStatus.COMPLETED)
        }
        val clock = FakeClock(now)
        return MapaActividadViewModel(GetTrainingActivityHeatmapUseCase(FakeWorkoutRepository(sessions), clock), clock)
    }

    @Test
    fun `opens on the current month`() = runTest {
        val vm = viewModel(at(2026, Calendar.OCTOBER, 7))
        backgroundScope.launch { vm.uiState.collect {} }

        val frequency = vm.uiState.first { it.frequency != null }.frequency!!

        assertEquals(FrequencyView.MONTH, frequency.view)
        assertEquals(1, frequency.sessions)
        assertFalse(frequency.canGoForward)
    }

    /** The use case computes off the main thread, so each step waits for the panel it expects. */
    private suspend fun MapaActividadViewModel.awaitFrequency(matching: (FrequencyPanel) -> Boolean = { true }): FrequencyPanel =
        uiState.first { state -> state.frequency?.let(matching) == true }.frequency!!

    @Test
    fun `arrows walk through the history, and switching views comes back to today`() = runTest {
        val vm = viewModel(at(2026, Calendar.AUGUST, 20), at(2026, Calendar.OCTOBER, 7))
        backgroundScope.launch { vm.uiState.collect {} }
        val october = vm.awaitFrequency()

        vm.onEvent(MapaActividadUiEvent.OnPreviousPeriod)
        vm.awaitFrequency { it.periodStart < october.periodStart }
        vm.onEvent(MapaActividadUiEvent.OnPreviousPeriod)
        val august = vm.awaitFrequency { it.sessions == 1 && !it.canGoBack }
        assertEquals(at(2026, Calendar.AUGUST, 1) - 10 * 3600_000L, august.periodStart)

        // Nothing before August: a further tap is ignored.
        vm.onEvent(MapaActividadUiEvent.OnPreviousPeriod)
        assertEquals(august, vm.awaitFrequency())

        vm.onEvent(MapaActividadUiEvent.OnSelectFrequencyView(FrequencyView.YEAR))
        val year = vm.awaitFrequency { it.view == FrequencyView.YEAR }
        assertEquals(2, year.sessions)
        assertFalse(year.canGoForward)

        // Already on the current year: forward does nothing.
        vm.onEvent(MapaActividadUiEvent.OnNextPeriod)
        assertEquals(year.periodStart, vm.awaitFrequency().periodStart)
    }
}
