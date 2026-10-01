package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import com.example.heavyduty.feature.workout.domain.usecase.StartWorkoutUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [MainDispatcherRule] runs everything on an unconfined test dispatcher, so repository writes
 * reach the ViewModel's database observer synchronously — assertions read `uiState.value`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = Exercise(id = 1, name = "Press banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)
    private val row = Exercise(id = 2, name = "Remo", muscleGroup = MuscleGroup.BACK, exerciseType = ExerciseType.STRENGTH)
    private val routine = Routine(
        id = 7,
        name = "Torso",
        exercises = listOf(
            RoutineExercise(exerciseId = 1, exercise = bench, orderIndex = 0, targetSets = 3, targetReps = 8),
            RoutineExercise(exerciseId = 2, exercise = row, orderIndex = 1, targetSets = 3, targetReps = 10),
        ),
    )

    private val workoutRepository = FakeWorkoutRepository()
    private val restTimer = FakeRestTimerController()

    private fun createViewModel(routineId: Long? = null): ActiveWorkoutViewModel {
        val preferences = mockk<UserPreferencesRepository> {
            every { effortTrackingEnabled } returns flowOf(false)
            every { preferredEffortScale } returns flowOf("RIR")
        }
        return ActiveWorkoutViewModel(
            startWorkoutUseCase = StartWorkoutUseCase(workoutRepository),
            logSetUseCase = LogSetUseCase(workoutRepository, restTimer),
            completeWorkoutUseCase = CompleteWorkoutUseCase(workoutRepository),
            routineRepository = FakeRoutineRepository(listOf(routine)),
            workoutRepository = workoutRepository,
            exerciseRepository = FakeExerciseRepository(listOf(bench, row)),
            userPreferencesRepository = preferences,
            restTimerController = restTimer,
            savedStateHandle = SavedStateHandle(routineId?.let { mapOf("routineId" to it) } ?: emptyMap()),
        )
    }

    private fun ActiveWorkoutViewModel.logSet(reps: String, weight: String = "60") {
        onEvent(ActiveWorkoutUiEvent.OnRepsChange(reps))
        onEvent(ActiveWorkoutUiEvent.OnWeightChange(weight))
        onEvent(ActiveWorkoutUiEvent.OnLogSet)
    }

    @Test
    fun `switching exercises keeps each exercise's logged sets and next set number`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))

        viewModel.logSet("8")
        viewModel.logSet("8")
        assertEquals(3, viewModel.uiState.value.currentSetNumber)

        viewModel.onEvent(ActiveWorkoutUiEvent.OnSelectExercise(1))
        assertEquals(1, viewModel.uiState.value.currentSetNumber)
        viewModel.logSet("10")

        viewModel.onEvent(ActiveWorkoutUiEvent.OnSelectExercise(0))
        val state = viewModel.uiState.value
        assertEquals(3, state.currentSetNumber)
        assertEquals(2, state.setsFor(bench.id).size)
        assertEquals(1, state.setsFor(row.id).size)
        assertEquals(3, state.completedPlannedSetCount)
    }

    @Test
    fun `switching exercises resets neither the session clock nor a running hold timer`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))
        val startTime = viewModel.uiState.value.sessionStartTime
        viewModel.onEvent(ActiveWorkoutUiEvent.OnToggleTimedInput)
        viewModel.onEvent(ActiveWorkoutUiEvent.OnToggleWorkTimer)

        viewModel.onEvent(ActiveWorkoutUiEvent.OnSelectExercise(1))

        val state = viewModel.uiState.value
        assertEquals(startTime, state.sessionStartTime)
        assertTrue(state.isWorkTimerRunning)
    }

    @Test
    fun `sets beyond the planned ones can be logged and count once towards progress`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))

        repeat(5) { viewModel.logSet("8") }

        val state = viewModel.uiState.value
        assertEquals(5, state.setsFor(bench.id).size)
        assertEquals(6, state.currentSetNumber)
        assertEquals(3, state.completedPlannedSetCount)
        assertEquals(6, state.plannedSetCount)
    }

    @Test
    fun `another screen for the same workout shows the same sets and resumes where it left off`() {
        val first = createViewModel()
        first.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))
        first.logSet("8")
        first.onEvent(ActiveWorkoutUiEvent.OnSelectExercise(1))
        first.logSet("10")

        val second = createViewModel()
        first.logSet("10")

        val state = second.uiState.value
        assertTrue(state.isActive)
        assertEquals(1, state.currentExerciseIndex)
        assertEquals(2, state.setsFor(row.id).size)
        assertEquals(3, state.currentSetNumber)
    }

    @Test
    fun `routine from the nav argument starts once and never on top of a running workout`() {
        createViewModel(routineId = routine.id)
        createViewModel(routineId = routine.id)

        assertEquals(1, workoutRepository.sessions.value.size)
    }

    @Test
    fun `logging with empty inputs uses the planned reps`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))

        viewModel.onEvent(ActiveWorkoutUiEvent.OnLogSet)

        val logged = viewModel.uiState.value.setsFor(bench.id).single()
        assertEquals(8, logged.reps)
        assertEquals(1, logged.setNumber)
    }

    @Test
    fun `finishing the workout returns to the start screen`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))
        viewModel.logSet("8")

        viewModel.onEvent(ActiveWorkoutUiEvent.OnCompleteWorkout)

        val state = viewModel.uiState.value
        assertFalse(state.isActive)
        assertTrue(state.completedSets.isEmpty())
        assertEquals(WorkoutStatus.COMPLETED, workoutRepository.sessions.value.single().status)
    }

    @Test
    fun `a set typed in pounds is stored in kg and remembers its unit`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))

        viewModel.onEvent(ActiveWorkoutUiEvent.OnToggleUnit)
        viewModel.logSet(reps = "10", weight = "100")

        val state = viewModel.uiState.value
        val logged = state.setsFor(bench.id).single()
        assertEquals(WeightUnit.LB, state.currentUnit)
        assertEquals(WeightUnit.LB, logged.weightUnit)
        assertEquals(45.36f, logged.weight!!, 0.01f)
    }

    @Test
    fun `switching units only affects the current exercise and later sets`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id))
        viewModel.logSet(reps = "8", weight = "60")

        viewModel.onEvent(ActiveWorkoutUiEvent.OnToggleUnit)
        viewModel.logSet(reps = "8", weight = "135")
        viewModel.onEvent(ActiveWorkoutUiEvent.OnSelectExercise(1))

        val state = viewModel.uiState.value
        assertEquals(WeightUnit.KG, state.currentUnit)
        assertEquals(listOf(WeightUnit.KG, WeightUnit.LB), state.setsFor(bench.id).map { it.weightUnit })
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
        private val sets = MutableStateFlow<List<WorkoutSet>>(emptyList())
        private var nextId = 1L

        override fun getAllSessions(): Flow<List<WorkoutSession>> = sessions
        override fun getSessionById(id: Long): Flow<WorkoutSession?> = sessions.map { list -> list.find { it.id == id } }
        override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> =
            combine(sessions, sets) { list, allSets ->
                list.find { it.id == id }?.copy(sets = allSets.filter { it.sessionId == id })
            }
        override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> = sessions
        override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> =
            sets.map { list -> list.filter { it.exerciseId == exerciseId } }
        override fun getAllCompletedSets(): Flow<List<WorkoutSet>> = sets
        override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>> = sets
        override fun getActiveSession(): Flow<WorkoutSession?> =
            sessions.map { list -> list.firstOrNull { it.status == WorkoutStatus.IN_PROGRESS } }
        override suspend fun insertSession(session: WorkoutSession): Long {
            val id = nextId++
            sessions.value = sessions.value + session.copy(id = id)
            return id
        }
        override suspend fun updateSession(session: WorkoutSession) {
            sessions.value = sessions.value.map { if (it.id == session.id) session else it }
        }
        override suspend fun deleteSession(session: WorkoutSession) {
            sessions.value = sessions.value.filter { it.id != session.id }
        }
        override suspend fun insertSet(set: WorkoutSet): Long {
            val id = nextId++
            sets.value = sets.value + set.copy(id = id)
            return id
        }
        override suspend fun updateSet(set: WorkoutSet) {
            sets.value = sets.value.map { if (it.id == set.id) set else it }
        }
        override suspend fun deleteSet(set: WorkoutSet) {
            sets.value = sets.value.filter { it.id != set.id }
        }
    }

    private class FakeRoutineRepository(private val routines: List<Routine>) : RoutineRepository {
        override fun getAllRoutines(): Flow<List<Routine>> = flowOf(routines)
        override fun getRoutineById(id: Long): Flow<Routine?> = flowOf(routines.find { it.id == id })
        override fun getRoutineWithExercises(id: Long): Flow<Routine?> = flowOf(routines.find { it.id == id })
        override suspend fun insertRoutine(routine: Routine): Long = 0L
        override suspend fun updateRoutine(routine: Routine) {}
        override suspend fun deleteRoutine(routine: Routine) {}
        override suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long = 0L
        override suspend fun updateRoutineExercise(routineExercise: RoutineExercise) {}
        override suspend fun deleteRoutineExercise(routineExercise: RoutineExercise) {}
        override suspend fun updateRoutineExerciseOrder(routineId: Long, exercises: List<RoutineExercise>) {}
    }

    private class FakeExerciseRepository(private val exercises: List<Exercise>) : ExerciseRepository {
        override fun getAllExercises(): Flow<List<Exercise>> = flowOf(exercises)
        override fun getExerciseById(id: Long): Flow<Exercise?> = flowOf(exercises.find { it.id == id })
        override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getExercisesByType(type: ExerciseType): Flow<List<Exercise>> = flowOf(emptyList())
        override fun searchExercises(query: String): Flow<List<Exercise>> = flowOf(emptyList())
        override suspend fun insertExercise(exercise: Exercise): Long = 0L
        override suspend fun updateExercise(exercise: Exercise) {}
        override suspend fun deleteExercise(exercise: Exercise) {}
    }

    private class FakeRestTimerController : RestTimerController {
        private val state = MutableStateFlow<RestTimerState?>(null)
        override val restState: StateFlow<RestTimerState?> = state
        override fun startRest(seconds: Int) {
            state.value = RestTimerState(seconds, seconds)
        }
        override fun adjustRest(deltaSeconds: Int) {}
        override fun skipRest() {
            state.value = null
        }
    }
}
