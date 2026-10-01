package com.example.heavyduty.feature.routines.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.routines.domain.usecase.CreateRoutineUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutineVolumeHistoryUseCase
import com.example.heavyduty.feature.routines.domain.usecase.GetRoutineWithExercisesUseCase
import com.example.heavyduty.feature.routines.domain.usecase.UpdateRoutineUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = exercise(1, "bench press")
    private val squat = exercise(2, "squat")
    private val row = exercise(3, "row")

    private val routineRepository = FakeRoutineRepository()
    private val exerciseRepository = FakeExerciseRepository(listOf(bench, squat, row))
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(routineId: Long? = null): RoutineEditorViewModel {
        routineId?.let { savedStateHandle["routineId"] = it }
        val workoutRepository = mockk<WorkoutRepository> {
            every { getCompletedSessionsByRoutineId(any()) } returns flowOf(emptyList())
        }
        viewModel = RoutineEditorViewModel(
            createRoutineUseCase = CreateRoutineUseCase(routineRepository),
            updateRoutineUseCase = UpdateRoutineUseCase(routineRepository),
            getRoutineWithExercisesUseCase = GetRoutineWithExercisesUseCase(routineRepository),
            getRoutineVolumeHistoryUseCase = GetRoutineVolumeHistoryUseCase(workoutRepository),
            exerciseRepository = exerciseRepository,
            savedStateHandle = savedStateHandle,
        )
        return viewModel
    }

    private lateinit var viewModel: RoutineEditorViewModel

    /** What the screen forwards once the exercise picker hands its result back. */
    private fun pick(vararg ids: Long) {
        viewModel.onEvent(RoutineEditorUiEvent.OnExercisesPicked(ids.toList()))
    }

    private fun RoutineEditorViewModel.names() = uiState.value.exercises.map { it.exercise?.name }

    @Test
    fun `picked exercises are added in pick order with unique keys and sensible defaults`() {
        val viewModel = createViewModel()

        pick(3, 1)
        pick(1)

        val exercises = viewModel.uiState.value.exercises
        assertEquals(listOf("row", "bench press", "bench press"), viewModel.names())
        assertEquals(exercises.size, exercises.map { it.id }.toSet().size)
        assertEquals(3, exercises.first().targetSets)
        assertEquals(90, exercises.first().restBetweenSetsSecs)
    }

    @Test
    fun `moving an exercise reorders the list`() {
        val viewModel = createViewModel()
        pick(1, 2, 3)

        viewModel.onEvent(RoutineEditorUiEvent.OnMoveExercise(from = 2, to = 0))

        assertEquals(listOf("row", "bench press", "squat"), viewModel.names())
    }

    @Test
    fun `a removed exercise can be restored in place`() {
        val viewModel = createViewModel()
        pick(1, 2, 3)

        viewModel.onEvent(RoutineEditorUiEvent.OnRemoveExercise(1))
        assertEquals(listOf("bench press", "row"), viewModel.names())

        viewModel.onEvent(RoutineEditorUiEvent.OnUndoRemove)
        assertEquals(listOf("bench press", "squat", "row"), viewModel.names())
    }

    @Test
    fun `saving without a name shows the error and saves nothing`() {
        val viewModel = createViewModel()
        pick(1)

        viewModel.onEvent(RoutineEditorUiEvent.OnSave)

        assertTrue(viewModel.uiState.value.showNameError)
        assertTrue(routineRepository.routines.isEmpty())
    }

    @Test
    fun `saving stores the chosen order and lets the database assign new ids`() {
        val viewModel = createViewModel()
        viewModel.onEvent(RoutineEditorUiEvent.OnNameChange("Torso"))
        pick(1, 2, 3)
        viewModel.onEvent(RoutineEditorUiEvent.OnMoveExercise(from = 0, to = 2))

        viewModel.onEvent(RoutineEditorUiEvent.OnSave)

        val saved = routineRepository.routines.single()
        assertEquals(listOf(2L, 3L, 1L), saved.exercises.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2), saved.exercises.map { it.orderIndex })
        assertTrue(saved.exercises.all { it.id == 0L })
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `kg or lb is set per routine exercise and saved with the routine`() {
        val viewModel = createViewModel()
        viewModel.onEvent(RoutineEditorUiEvent.OnNameChange("Torso"))
        pick(1, 2, 1)
        val first = viewModel.uiState.value.exercises[0]

        // Same exercise twice: e.g. a pound machine early, kilo plates later in the routine.
        viewModel.onEvent(RoutineEditorUiEvent.OnUpdateExercise(0, first.copy(weightUnit = WeightUnit.LB)))
        viewModel.onEvent(RoutineEditorUiEvent.OnSave)

        val saved = routineRepository.routines.single().exercises
        assertEquals(listOf(WeightUnit.LB, null, null), saved.map { it.weightUnit })
        // The exercise's own default is untouched — other routines keep their units.
        assertEquals(WeightUnit.KG, exerciseRepository.byId(1)?.weightUnit)
    }

    @Test
    fun `all exercises can be switched to one unit at once`() {
        val viewModel = createViewModel()
        pick(1, 2)

        viewModel.onEvent(RoutineEditorUiEvent.OnSetAllUnits(WeightUnit.LB))

        assertEquals(listOf(WeightUnit.LB, WeightUnit.LB), viewModel.uiState.value.exercises.map { it.weightUnit })
    }

    @Test
    fun `editing an existing routine starts without unsaved changes`() {
        routineRepository.routines += Routine(
            id = 9,
            name = "Pierna",
            exercises = listOf(RoutineExercise(id = 5, routineId = 9, exerciseId = 2, exercise = squat)),
        )
        val viewModel = createViewModel(routineId = 9)

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        viewModel.onEvent(RoutineEditorUiEvent.OnNameChange("Pierna B"))
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertNull(viewModel.uiState.value.editingExercise)
    }

    private fun exercise(id: Long, name: String) = Exercise(
        id = id,
        name = name,
        muscleGroup = MuscleGroup.CHEST,
        exerciseType = ExerciseType.STRENGTH,
    )

    private class FakeRoutineRepository : RoutineRepository {
        val routines = mutableListOf<Routine>()
        override fun getAllRoutines(): Flow<List<Routine>> = flowOf(routines.toList())
        override fun getRoutineById(id: Long): Flow<Routine?> = flowOf(routines.find { it.id == id })
        override fun getRoutineWithExercises(id: Long): Flow<Routine?> = flowOf(routines.find { it.id == id })
        override suspend fun insertRoutine(routine: Routine): Long {
            val id = (routines.maxOfOrNull { it.id } ?: 0) + 1
            routines += routine.copy(id = id)
            return id
        }
        override suspend fun updateRoutine(routine: Routine) {
            routines.replaceAll { if (it.id == routine.id) routine else it }
        }
        override suspend fun deleteRoutine(routine: Routine) {
            routines.removeAll { it.id == routine.id }
        }
        override suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long = 0L
        override suspend fun updateRoutineExercise(routineExercise: RoutineExercise) {}
        override suspend fun deleteRoutineExercise(routineExercise: RoutineExercise) {}
        override suspend fun updateRoutineExerciseOrder(routineId: Long, exercises: List<RoutineExercise>) {}
    }

    private class FakeExerciseRepository(initial: List<Exercise>) : ExerciseRepository {
        private val exercises = initial.associateBy { it.id }.toMutableMap()
        fun byId(id: Long) = exercises[id]
        override fun getAllExercises(): Flow<List<Exercise>> = flowOf(exercises.values.toList())
        override fun getExerciseById(id: Long): Flow<Exercise?> = flowOf(exercises[id])
        override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getExercisesByType(type: ExerciseType): Flow<List<Exercise>> = flowOf(emptyList())
        override fun searchExercises(query: String): Flow<List<Exercise>> = flowOf(emptyList())
        override suspend fun insertExercise(exercise: Exercise): Long = exercise.id
        override suspend fun updateExercise(exercise: Exercise) {
            exercises[exercise.id] = exercise
        }
        override suspend fun deleteExercise(exercise: Exercise) {}
    }
}
