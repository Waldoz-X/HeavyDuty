package com.example.heavyduty

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The bottom bar (portrait) and the side rail (landscape) take the user where they tap. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "es")
class NavigationChromeTest {

    @get:Rule
    val compose = createComposeRule()

    // Resting, so the rail shows the countdown rather than a ticking session clock.
    private val workout = ActiveWorkoutSummary(
        sessionId = 1,
        sessionName = "Torso",
        startTime = 0L,
        setsLogged = 3,
        lastActivityAt = 0L,
        rest = RestTimerState(remainingSeconds = 75, totalSeconds = 90)
    )

    @Test
    fun theBottomBarOpensEachSection() {
        val opened = mutableListOf<TopLevelDestination>()
        compose.setContent {
            HeavyDutyTheme { HeavyDutyNavigationBar(isSelected = { it == TopLevelDestination.DASHBOARD }, onNavigate = { opened += it }) }
        }

        compose.onNodeWithContentDescription("Entrenar", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Temporizador", useUnmergedTree = true).performClick()

        assertEquals(listOf(TopLevelDestination.WORKOUT, TopLevelDestination.TIMER), opened)
    }

    @Test
    fun theRailOpensSectionsTheMenuAndTheWorkoutInProgress() {
        val calls = mutableListOf<String>()
        compose.setContent {
            HeavyDutyTheme {
                HeavyDutyNavigationRail(
                    isSelected = { it == TopLevelDestination.DASHBOARD },
                    onNavigate = { calls += it.name },
                    onOpenMenu = { calls += "menu" },
                    workout = workout,
                    showWorkout = true,
                    onOpenWorkout = { calls += "workout" }
                )
            }
        }

        compose.onNodeWithContentDescription("Ejercicios", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        compose.onNodeWithContentDescription("Volver al entrenamiento").performClick()

        assertEquals(listOf(TopLevelDestination.EXERCISES.name, "menu", "workout"), calls)
    }

    @Test
    fun theRailHidesTheWorkoutShortcutWhenThereIsNone() {
        compose.setContent {
            HeavyDutyTheme {
                HeavyDutyNavigationRail(
                    isSelected = { false },
                    onNavigate = {},
                    onOpenMenu = {},
                    workout = null,
                    showWorkout = false,
                    onOpenWorkout = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Volver al entrenamiento").assertDoesNotExist()
    }
}
