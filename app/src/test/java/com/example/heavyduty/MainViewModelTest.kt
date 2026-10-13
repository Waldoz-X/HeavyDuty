package com.example.heavyduty

import com.example.heavyduty.core.common.navigation.AppSections
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.ThemeMode
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.domain.repository.StoredDataCheck
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeRoutineRepository
import com.example.heavyduty.core.testing.FakeUserPreferencesRepository
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.DiscardWorkoutUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val minute = 60_000L
    private val hour = 60 * minute
    private val start = 1_800_000_000_000
    private val clock = FakeClock(start)
    private val session = WorkoutSession(id = 1, sessionName = "Torso", startTime = start, status = WorkoutStatus.IN_PROGRESS)
    private val restTimer = FakeRestTimer()

    private fun setAt(id: Long, at: Long) =
        WorkoutSet(id = id, sessionId = 1, exerciseId = 1, reps = 8, weight = 60f, isCompleted = true, completedAt = at)

    private fun viewModel(repository: FakeWorkoutRepository): MainViewModel {
        val preferences = mockk<UserPreferencesRepository> {
            every { preferredWeightUnit } returns flowOf(WeightUnit.KG)
            every { themeMode } returns flowOf(ThemeMode.DARK)
            every { onboardingCompleted } returns flowOf(true)
        }
        return MainViewModel(
            userPreferences = preferences,
            workoutRepository = repository,
            trainingHistoryRepository = repository.history,
            routineRepository = FakeRoutineRepository(),
            storedDataCheck = StoredDataCheck { true },
            restTimerController = restTimer,
            completeWorkout = CompleteWorkoutUseCase(repository, clock),
            discardWorkout = DiscardWorkoutUseCase(repository),
            clock = clock
        ).also { vm -> CoroutineScope(mainDispatcherRule.testDispatcher).launch { vm.forgottenWorkout.collect {} } }
    }

    /** Lets the once-a-minute check run. */
    private fun aMinuteLater() {
        clock.advanceBy(minute)
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(minute + 1)
    }

    @Test
    fun `a workout being trained is left alone`() {
        val repository = FakeWorkoutRepository(listOf(session), listOf(setAt(1, start + 50 * minute)))
        clock.now = start + hour

        assertNull(viewModel(repository).forgottenWorkout.value)
    }

    @Test
    fun `three hours with nothing logged asks whether it's still going`() {
        val repository = FakeWorkoutRepository(listOf(session), listOf(setAt(1, start + hour)))
        clock.now = start + 5 * hour

        val forgotten = viewModel(repository).forgottenWorkout.value

        assertEquals(ForgottenWorkout(sessionId = 1, sessionName = "Torso", setsLogged = 1, idleHours = 4), forgotten)
    }

    @Test
    fun `it asks while the app is open, as soon as the three hours pass`() {
        val repository = FakeWorkoutRepository(listOf(session))
        clock.now = start + 3 * hour - minute
        val viewModel = viewModel(repository)
        assertNull(viewModel.forgottenWorkout.value)

        aMinuteLater()

        assertNotNull(viewModel.forgottenWorkout.value)
    }

    @Test
    fun `finishing it saves the workout up to its last set and stops the rest timer`() {
        val repository = FakeWorkoutRepository(listOf(session), listOf(setAt(1, start + 30 * minute), setAt(2, start + hour)))
        clock.now = start + 35 * hour
        val viewModel = viewModel(repository)
        restTimer.startRest(90)

        viewModel.finishForgottenWorkout()

        val finished = repository.sessions.value.single()
        assertEquals(WorkoutStatus.COMPLETED, finished.status)
        assertEquals(start + hour, finished.endTime)
        assertEquals(60 * 60, finished.totalDurationSecs)
        assertNull(restTimer.restState.value)
        assertNull(viewModel.forgottenWorkout.value)
    }

    @Test
    fun `discarding it deletes the workout`() {
        val repository = FakeWorkoutRepository(listOf(session), listOf(setAt(1, start)))
        clock.now = start + 4 * hour

        viewModel(repository).discardForgottenWorkout()

        assertTrue(repository.sessions.value.isEmpty())
        assertTrue(repository.sets.value.isEmpty())
    }

    @Test
    fun `still training stops asking, until it goes quiet again after a new set`() {
        val repository = FakeWorkoutRepository(listOf(session))
        clock.now = start + 3 * hour
        val viewModel = viewModel(repository)

        viewModel.keepTraining()
        assertNull(viewModel.forgottenWorkout.value)

        repository.sets.value = listOf(setAt(1, clock.now))
        clock.advanceBy(3 * hour)
        aMinuteLater()

        assertNotNull(viewModel.forgottenWorkout.value)
    }

    private fun onboardingFor(
        completed: Boolean,
        routines: List<Routine>,
        sessions: List<WorkoutSession>,
        databaseExisted: Boolean = true
    ): Boolean? {
        val preferences = mockk<UserPreferencesRepository> {
            every { preferredWeightUnit } returns flowOf(WeightUnit.KG)
            every { themeMode } returns flowOf(ThemeMode.DARK)
            every { onboardingCompleted } returns flowOf(completed)
        }
        val repository = FakeWorkoutRepository(sessions)
        return MainViewModel(
            userPreferences = preferences,
            workoutRepository = repository,
            trainingHistoryRepository = repository.history,
            routineRepository = FakeRoutineRepository(routines),
            storedDataCheck = StoredDataCheck { databaseExisted },
            restTimerController = restTimer,
            completeWorkout = CompleteWorkoutUseCase(repository, clock),
            discardWorkout = DiscardWorkoutUseCase(repository),
            clock = clock
        ).needsOnboarding.value
    }

    @Test
    fun `the welcome is for a new install only`() {
        assertEquals(true, onboardingFor(completed = false, routines = emptyList(), sessions = emptyList()))
        // Already finished or skipped.
        assertEquals(false, onboardingFor(completed = true, routines = emptyList(), sessions = emptyList()))
        // Someone already using the app, from before the welcome existed.
        assertEquals(false, onboardingFor(completed = false, routines = listOf(Routine(id = 1, name = "Torso")), sessions = emptyList()))
        assertEquals(false, onboardingFor(completed = false, routines = emptyList(), sessions = listOf(session)))
        // First launch ever: decided without waiting on the database.
        assertEquals(true, onboardingFor(completed = false, routines = emptyList(), sessions = listOf(session), databaseExisted = false))
    }

    @Test
    fun `finishing the welcome opens the routine editor first, then leaves the welcome for good`() {
        val preferences = FakeUserPreferencesRepository()
        val repository = FakeWorkoutRepository()
        val viewModel = MainViewModel(
            userPreferences = preferences,
            workoutRepository = repository,
            trainingHistoryRepository = repository.history,
            routineRepository = FakeRoutineRepository(),
            storedDataCheck = StoredDataCheck { false },
            restTimerController = restTimer,
            completeWorkout = CompleteWorkoutUseCase(repository, clock),
            discardWorkout = DiscardWorkoutUseCase(repository),
            clock = clock
        )
        assertEquals(true, viewModel.needsOnboarding.value)

        viewModel.completeOnboarding(createFirstRoutine = true)

        assertEquals(AppSections.SECTION_NEW_ROUTINE, viewModel.pendingSection.value)
        assertTrue(preferences.onboardingCompleted.value)
        assertEquals(false, viewModel.needsOnboarding.value)
    }

    private class FakeRestTimer : RestTimerController {
        private val state = MutableStateFlow<RestTimerState?>(null)
        override val restState: StateFlow<RestTimerState?> = state
        override fun startRest(seconds: Int) {
            state.value = RestTimerState(totalSeconds = seconds, remainingSeconds = seconds)
        }
        override fun adjustRest(deltaSeconds: Int) = Unit
        override fun skipRest() {
            state.value = null
        }
    }
}
