package com.example.heavyduty.feature.workout.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The workout screen's key interactions, driven with a fixed state (no ViewModel): each test
 * checks the screen sends the right event, which the ViewModel tests then cover.
 */
@RunWith(AndroidJUnit4::class)
// Spanish, the app's default language, so the visible texts match the real app.
@Config(sdk = [35], qualifiers = "es")
class ActiveWorkoutContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val bench = Exercise(id = 1, name = "Press de banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)

    private fun state(loggedSets: Int) = ActiveWorkoutUiState(
        sessionId = 1,
        sessionName = "Torso",
        isLoading = false,
        isActive = true,
        routineExercises = listOf(RoutineExercise(exerciseId = 1, exercise = bench, targetSets = 3, targetReps = 8)),
        completedSets = (1..loggedSets).map {
            WorkoutSet(id = it.toLong(), sessionId = 1, exerciseId = 1, setNumber = it, reps = 8, weight = 60f, isCompleted = true)
        },
    )

    private fun show(state: ActiveWorkoutUiState): MutableList<ActiveWorkoutUiEvent> {
        val events = mutableListOf<ActiveWorkoutUiEvent>()
        compose.setContent {
            HeavyDutyTheme { ActiveWorkoutContent(uiState = state, onEvent = { events += it }) }
        }
        return events
    }

    @Test
    fun tappingTheCheckLogsTheSet() {
        val events = show(state(loggedSets = 0))

        compose.onNodeWithContentDescription("Registrar serie").performClick()

        assertTrue(ActiveWorkoutUiEvent.OnLogSet in events)
    }

    @Test
    fun swipingASetLeftDeletesIt() {
        val events = show(state(loggedSets = 2))

        // The first logged set's row: swipe from its delete button towards the left edge.
        compose.onAllNodesWithContentDescription("Eliminar serie").onFirst().performTouchInput { swipeLeft() }
        compose.waitForIdle()

        val deleted = events.filterIsInstance<ActiveWorkoutUiEvent.OnDeleteSet>()
        assertEquals(1, deleted.size)
        assertEquals(1, deleted.single().set.setNumber)
    }

    @Test
    fun finishingEarlySaysHowManyPlannedSetsAreMissing() {
        show(state(loggedSets = 2))

        compose.onNodeWithText("Finalizar").performClick()

        compose.onNodeWithText("Te falta 1 serie planificada (llevas 2 de 3). Quedará sin registrar.").assertIsDisplayed()
    }
}
