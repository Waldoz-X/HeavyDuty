package com.example.heavyduty.feature.profile.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingContent
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingPage
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingUiEvent
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The welcome's pages and the way out of each, driven with a fixed state (no ViewModel). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "es")
class OnboardingContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(page: OnboardingPage): MutableList<OnboardingUiEvent> {
        val events = mutableListOf<OnboardingUiEvent>()
        compose.setContent {
            HeavyDutyTheme { OnboardingContent(uiState = OnboardingUiState(page = page), onEvent = { events += it }) }
        }
        return events
    }

    @Test
    fun theFirstPageAsksForTheNameAndCanBeSkipped() {
        val events = show(OnboardingPage.WELCOME)

        compose.onNodeWithText("Bienvenido a HeavyDuty").assertIsDisplayed()
        compose.onNodeWithText("Atrás").assertDoesNotExist()
        compose.onNodeWithText("Tu nombre (opcional)").performTextInput("Waldo")
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Omitir").performClick()

        assertEquals(
            listOf(
                OnboardingUiEvent.OnNameChange("Waldo"),
                OnboardingUiEvent.OnNext,
                OnboardingUiEvent.OnFinish(createFirstRoutine = false)
            ),
            events
        )
    }

    @Test
    fun theUnitPageOffersKgAndLbAndGoesBack() {
        val events = show(OnboardingPage.UNIT)

        compose.onNodeWithText("Libras (lb)").performClick()
        compose.onNodeWithText("Atrás").performClick()

        assertEquals(listOf(OnboardingUiEvent.OnUnitChange(WeightUnit.LB), OnboardingUiEvent.OnBack), events)
    }

    @Test
    fun theReminderPageTurnsTheReminderOn() {
        val events = show(OnboardingPage.REMINDER)

        compose.onNodeWithText("Activado").performClick()

        assertTrue(OnboardingUiEvent.OnReminderEnabledChange(true) in events)
    }

    @Test
    fun theLastPageLeadsToTheFirstRoutineOrStraightIn() {
        val events = show(OnboardingPage.READY)

        compose.onNodeWithText("Omitir").assertDoesNotExist()
        compose.onNodeWithText("Crear mi primera rutina").performClick()
        compose.onNodeWithText("Empezar").performClick()

        assertEquals(
            listOf(OnboardingUiEvent.OnFinish(createFirstRoutine = true), OnboardingUiEvent.OnFinish(createFirstRoutine = false)),
            events
        )
    }
}
