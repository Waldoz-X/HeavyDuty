package com.example.heavyduty.feature.exercises.presentation.list

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.exercises.domain.usecase.GetExercisesUseCase
import com.example.heavyduty.feature.exercises.domain.usecase.SearchExercisesUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [MainDispatcherRule] installs an [kotlinx.coroutines.test.UnconfinedTestDispatcher] as Main, so
 * every launch/flow update below settles synchronously — assertions read `uiState.value` directly
 * with no manual advancing needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val benchPress = exercise(id = 1, name = "Bench Press", muscleGroup = MuscleGroup.CHEST)
    private val squat = exercise(id = 2, name = "Squat", muscleGroup = MuscleGroup.QUADRICEPS)
    private val allExercises = listOf(benchPress, squat)

    private val getExercisesUseCase = mockk<GetExercisesUseCase>()
    private val searchExercisesUseCase = mockk<SearchExercisesUseCase>()

    private fun createViewModel(): ExerciseListViewModel {
        every { getExercisesUseCase(null) } returns flowOf(allExercises)
        every { getExercisesUseCase(MuscleGroup.CHEST) } returns flowOf(listOf(benchPress))
        every { searchExercisesUseCase("Bench") } returns flowOf(listOf(benchPress))
        return ExerciseListViewModel(getExercisesUseCase, searchExercisesUseCase)
    }

    @Test
    fun `loads all exercises on init`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertEquals(2, state.exercises.size)
        assertTrue(state.isLoading.not())
    }

    @Test
    fun `selecting a filter narrows the exercise list`() {
        val viewModel = createViewModel()

        viewModel.onEvent(ExerciseListUiEvent.OnFilterSelect(MuscleGroup.CHEST))
        val state = viewModel.uiState.value

        assertEquals(MuscleGroup.CHEST, state.selectedFilter)
        assertEquals(listOf(benchPress), state.exercises)
    }

    @Test
    fun `selecting the same filter twice clears it`() {
        val viewModel = createViewModel()

        viewModel.onEvent(ExerciseListUiEvent.OnFilterSelect(MuscleGroup.CHEST))
        viewModel.onEvent(ExerciseListUiEvent.OnFilterSelect(MuscleGroup.CHEST))
        val state = viewModel.uiState.value

        assertNull(state.selectedFilter)
        assertEquals(allExercises, state.exercises)
    }

    @Test
    fun `typing a search query switches to search results`() {
        val viewModel = createViewModel()

        viewModel.onEvent(ExerciseListUiEvent.OnSearchQueryChange("Bench"))
        val state = viewModel.uiState.value

        assertEquals("Bench", state.searchQuery)
        assertEquals(listOf(benchPress), state.exercises)
    }

    private fun exercise(id: Long, name: String, muscleGroup: MuscleGroup) = Exercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        exerciseType = ExerciseType.STRENGTH,
    )
}
