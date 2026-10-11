package com.example.heavyduty.feature.history

import androidx.lifecycle.SavedStateHandle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.history.domain.usecase.DeleteWorkoutUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetWorkoutDetailUseCase
import com.example.heavyduty.feature.history.domain.usecase.UpdateLoggedSetUseCase
import com.example.heavyduty.feature.history.domain.usecase.UpdateWorkoutDurationUseCase
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailUiEffect
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailUiEvent
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailUiState
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutEditingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val start = 1_800_000_000_000
    private val bench = Exercise(id = 1, name = "Press de banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)
    private val squat = Exercise(id = 2, name = "Sentadilla", muscleGroup = MuscleGroup.QUADRICEPS, exerciseType = ExerciseType.STRENGTH)

    // The 38-hour one from the emulator: an hour of training left open.
    private val session = WorkoutSession(
        id = 1,
        sessionName = "Torso",
        startTime = start,
        endTime = start + 38 * 3_600_000L,
        totalDurationSecs = 38 * 3_600,
        status = WorkoutStatus.COMPLETED
    )
    private val repository = FakeWorkoutRepository(
        sessions = listOf(session, session.copy(id = 2, sessionName = "Pierna")),
        sets = listOf(
            loggedSet(id = 10, session = 1, exercise = squat, number = 1, reps = 5, kg = 100f, at = start + 1_000),
            loggedSet(id = 11, session = 1, exercise = bench, number = 1, reps = 8, kg = 60f, at = start + 2_000),
            loggedSet(id = 12, session = 1, exercise = bench, number = 2, reps = 8, kg = 40f, at = start + 3_000, warmup = true),
            loggedSet(id = 20, session = 2, exercise = squat, number = 1, reps = 5, kg = 120f, at = start)
        )
    )

    private fun loggedSet(
        id: Long,
        session: Long,
        exercise: Exercise,
        number: Int,
        reps: Int,
        kg: Float,
        at: Long,
        warmup: Boolean = false
    ) = WorkoutSet(
        id = id, sessionId = session, exerciseId = exercise.id, exercise = exercise, setNumber = number,
        reps = reps, weight = kg, isCompleted = true, completedAt = at, isWarmup = warmup
    )

    private fun viewModel(): Pair<WorkoutDetailViewModel, List<WorkoutDetailUiEffect>> {
        val vm = WorkoutDetailViewModel(
            getWorkoutDetail = GetWorkoutDetailUseCase(repository),
            updateWorkoutDuration = UpdateWorkoutDurationUseCase(repository),
            updateLoggedSet = UpdateLoggedSetUseCase(repository),
            deleteWorkout = DeleteWorkoutUseCase(repository),
            workoutRepository = repository,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to 1L))
        )
        val effects = mutableListOf<WorkoutDetailUiEffect>()
        CoroutineScope(mainDispatcherRule.testDispatcher).launch { vm.uiEffect.collect { effects += it } }
        CoroutineScope(mainDispatcherRule.testDispatcher).launch { vm.uiState.collect {} }
        return vm to effects
    }

    // ---------------------------------------------------------------- use cases

    @Test
    fun `fixing the duration moves the end, never the start`() = runTest {
        assertTrue(UpdateWorkoutDurationUseCase(repository)(sessionId = 1, minutes = 60))

        val fixed = repository.sessions.value.first { it.id == 1L }
        assertEquals(start, fixed.startTime)
        assertEquals(start + 3_600_000L, fixed.endTime)
        assertEquals(3_600, fixed.totalDurationSecs)
    }

    @Test
    fun `a duration of nothing or of more than a day is refused`() = runTest {
        val update = UpdateWorkoutDurationUseCase(repository)

        assertFalse(update(sessionId = 1, minutes = 0))
        assertFalse(update(sessionId = 1, minutes = 24 * 60 + 1))
        assertEquals(38 * 3_600, repository.sessions.value.first { it.id == 1L }.totalDurationSecs)
    }

    @Test
    fun `a set's weight and reps can be fixed, but not to nonsense`() = runTest {
        val update = UpdateLoggedSetUseCase(repository)
        val set = repository.sets.value.first { it.id == 11L }

        assertFalse(update(set, weightKg = -5f, reps = 8))
        assertFalse(update(set, weightKg = 60f, reps = 0))
        assertTrue(update(set, weightKg = 62.5f, reps = 6))

        val fixed = repository.sets.value.first { it.id == 11L }
        assertEquals(62.5f, fixed.weight)
        assertEquals(6, fixed.reps)
        assertEquals(set.completedAt, fixed.completedAt)
    }

    @Test
    fun `deleting a workout takes its sets and leaves the others alone`() = runTest {
        DeleteWorkoutUseCase(repository)(sessionId = 1)

        assertEquals(listOf(2L), repository.sessions.value.map { it.id })
        assertEquals(listOf(20L), repository.sets.value.map { it.id })
    }

    // ---------------------------------------------------------------- screen

    @Test
    fun `the detail groups sets by exercise in the order they were trained, and counts working sets only`() {
        val sets = repository.sets.value.filter { it.sessionId == 1L }
        val state = WorkoutDetailUiState(session = session.copy(sets = sets), isLoading = false)

        assertEquals(listOf("Sentadilla", "Press de banca"), state.exercises.map { it.name })
        assertEquals(2, state.workingSets)
        assertEquals(100f * 5 + 60f * 8, state.volumeKg)
    }

    @Test
    fun `typed minutes fix the duration, and a typo explains itself instead of saving`() {
        val (vm, effects) = viewModel()

        vm.onEvent(WorkoutDetailUiEvent.OnSaveDuration("abc"))
        vm.onEvent(WorkoutDetailUiEvent.OnSaveDuration(" 75 "))

        assertEquals(WorkoutDetailUiEffect.ShowMessage(R.string.history_invalid_duration), effects.single())
        assertEquals(75 * 60, vm.uiState.value.session?.totalDurationSecs)
    }

    @Test
    fun `a weight typed in pounds is stored in kg`() {
        val (vm, _) = viewModel()
        val set = repository.sets.value.first { it.id == 11L }

        vm.onEvent(WorkoutDetailUiEvent.OnSaveSet(set, weight = "135", reps = "10", unit = WeightUnit.LB))

        val fixed = repository.sets.value.first { it.id == 11L }
        assertEquals(61.235f, fixed.weight!!, 0.01f)
        assertEquals(10, fixed.reps)
    }

    @Test
    fun `a deleted set can be put back with undo`() {
        val (vm, effects) = viewModel()
        val set = repository.sets.value.first { it.id == 11L }

        vm.onEvent(WorkoutDetailUiEvent.OnDeleteSet(set))
        assertTrue(repository.sets.value.none { it.id == 11L })
        assertEquals(WorkoutDetailUiEffect.SetDeleted(set), effects.single())

        vm.onEvent(WorkoutDetailUiEvent.OnUndoDeleteSet(set))
        assertTrue(repository.sets.value.any { it.id == 11L && it.setNumber == 1 })
    }

    @Test
    fun `deleting the workout leaves the screen`() {
        val (vm, effects) = viewModel()

        vm.onEvent(WorkoutDetailUiEvent.OnDeleteWorkout)

        assertTrue(repository.sessions.value.none { it.id == 1L })
        assertEquals(WorkoutDetailUiEffect.WorkoutDeleted, effects.single())
    }
}
