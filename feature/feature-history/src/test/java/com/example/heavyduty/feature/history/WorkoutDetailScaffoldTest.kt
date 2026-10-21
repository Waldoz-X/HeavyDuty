package com.example.heavyduty.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.feature.history.domain.model.ShareExerciseLine
import com.example.heavyduty.feature.history.domain.model.ShareRecord
import com.example.heavyduty.feature.history.domain.model.WorkoutShareSummary
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailScaffold
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailUiEvent
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailUiState
import com.example.heavyduty.feature.history.presentation.list.openHistorySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The workout detail's ways in and out — edit, delete, share, back — driven with a fixed state. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "es")
class WorkoutDetailScaffoldTest {

    @get:Rule
    val compose = createComposeRule()

    private val bench = Exercise(id = 1, name = "Press de banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)
    private val set =
        WorkoutSet(id = 7, sessionId = 1, exerciseId = 1, exercise = bench, setNumber = 1, reps = 8, weight = 80f, isCompleted = true)
    private val session = WorkoutSession(
        id = 1,
        sessionName = "Torso",
        startTime = 1_800_000_000_000,
        totalDurationSecs = 3_600,
        status = WorkoutStatus.COMPLETED,
        sets = listOf(set)
    )

    private val summary = WorkoutShareSummary(
        startTime = session.startTime,
        durationSecs = session.totalDurationSecs,
        workingSets = 1,
        volumeKg = 640f,
        exercises = listOf(ShareExerciseLine("Press de banca", workingSets = 1, bestSet = set)),
        records = listOf(ShareRecord("Press de banca", set)),
        setsByMuscle = mapOf(MuscleGroup.CHEST to 1f)
    )

    private fun show(justFinished: Boolean = false): Pair<MutableList<WorkoutDetailUiEvent>, MutableList<Unit>> {
        val events = mutableListOf<WorkoutDetailUiEvent>()
        val backs = mutableListOf<Unit>()
        compose.setContent {
            HeavyDutyTheme {
                WorkoutDetailScaffold(
                    uiState = WorkoutDetailUiState(session = session, isLoading = false, shareSummary = summary),
                    onEvent = { events += it },
                    onNavigateBack = { backs += Unit },
                    justFinished = justFinished
                )
            }
        }
        return events to backs
    }

    @Test
    fun tappingASetEditsItsWeightAndReps() {
        val (events, _) = show()

        compose.onNodeWithText("80 kg × 8").performClick()
        compose.onNodeWithText("Editar serie 1").assertIsDisplayed()
        compose.onNodeWithText("Repeticiones").performTextReplacement("6")
        compose.onNodeWithText("Guardar").performClick()

        assertEquals(listOf(WorkoutDetailUiEvent.OnSaveSet(set, weight = "80", reps = "6", unit = WeightUnit.KG)), events)
    }

    @Test
    fun deletingTheWorkoutAsksFirst() {
        val (events, _) = show()

        compose.onNodeWithContentDescription("Eliminar entrenamiento").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(events.isEmpty())

        compose.onNodeWithContentDescription("Eliminar entrenamiento").performClick()
        compose.onNodeWithText("Eliminar").performClick()
        assertEquals(listOf(WorkoutDetailUiEvent.OnDeleteWorkout), events)
    }

    @Test
    fun shareShowsTheCardBeforeSharing() {
        show()

        compose.onNodeWithContentDescription("Compartir").performClick()

        compose.onNodeWithText("Compartir entrenamiento").assertIsDisplayed()
        compose.onNodeWithText("Historia").assertIsDisplayed()
        compose.onNodeWithText("Guardar").assertIsDisplayed()
        compose.onNodeWithText("1 serie · 80 kg × 8").assertExists()
        compose.onNodeWithText("1 récord nuevo").assertExists()

        // The post format has no room for the body map; the story does.
        compose.onNodeWithText("Publicación").performClick()
        compose.onNodeWithText("1 serie · 80 kg × 8").assertExists()
    }

    @Test
    fun aJustFinishedWorkoutOffersSharingRightAway() {
        show(justFinished = true)

        compose.onNodeWithText("Compartir entrenamiento").performClick()

        compose.onNodeWithText("Historia").assertIsDisplayed()
    }

    @Test
    fun theBackArrowLeaves() {
        val (_, backs) = show()

        compose.onNodeWithContentDescription("Atrás").performClick()

        assertEquals(1, backs.size)
    }

    @Test
    fun aFinishedWorkoutOpensItsDetailAndOneInProgressItsWorkout() {
        val opened = mutableListOf<String>()

        openHistorySession(session, onContinueWorkout = { opened += "workout" }, onOpenDetail = { opened += "detail $it" })
        openHistorySession(session.copy(status = WorkoutStatus.IN_PROGRESS), onContinueWorkout = { opened += "workout" }, onOpenDetail = {
            opened +=
                "detail $it"
        })

        assertEquals(listOf("detail 1", "workout"), opened)
    }
}
