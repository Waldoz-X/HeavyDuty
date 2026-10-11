package com.example.heavyduty.feature.profile.presentation

import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import com.example.heavyduty.core.testing.FakeUserPreferencesRepository
import com.example.heavyduty.core.testing.MainDispatcherRule
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingPage
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingUiEffect
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingUiEvent
import com.example.heavyduty.feature.profile.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()
    private val scheduled = mutableListOf<ReminderSettings>()
    private val scheduler = TrainingReminderScheduler { scheduled += it }

    private fun viewModel(): Pair<OnboardingViewModel, List<OnboardingUiEffect>> {
        val vm = OnboardingViewModel(preferences, scheduler)
        val effects = mutableListOf<OnboardingUiEffect>()
        CoroutineScope(mainDispatcherRule.testDispatcher).launch { vm.uiEffect.collect { effects += it } }
        return vm to effects
    }

    @Test
    fun `finishing saves every choice and arms the reminder`() {
        val (vm, effects) = viewModel()
        vm.onEvent(OnboardingUiEvent.OnNameChange("  Waldo "))
        vm.onEvent(OnboardingUiEvent.OnUnitChange(WeightUnit.LB))
        vm.onEvent(OnboardingUiEvent.OnReminderEnabledChange(true))
        vm.onEvent(OnboardingUiEvent.OnReminderTimeChange(7, 30))

        vm.onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = true))

        assertEquals("Waldo", preferences.displayName.value)
        assertEquals(WeightUnit.LB, preferences.preferredWeightUnit.value)
        val reminder = ReminderSettings(enabled = true, hour = 7, minute = 30)
        assertEquals(reminder, preferences.trainingReminder.value)
        assertEquals(listOf(reminder), scheduled)
        // Left to the host, which first arranges where to go (MainViewModel.completeOnboarding).
        assertEquals(false, preferences.onboardingCompleted.value)
        assertEquals(listOf(OnboardingUiEffect.Finished(createFirstRoutine = true)), effects)
    }

    @Test
    fun `skipping keeps the defaults and an unnamed athlete, and finishing twice saves once`() {
        val (vm, effects) = viewModel()

        vm.onEvent(OnboardingUiEvent.OnNameChange("   "))
        vm.onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = false))
        vm.onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = false))

        assertEquals("", preferences.displayName.value)
        assertEquals(ReminderSettings(), preferences.trainingReminder.value)
        assertEquals(1, effects.size)
    }

    @Test
    fun `next and back walk the pages without running past either end`() {
        val (vm, _) = viewModel()

        vm.onEvent(OnboardingUiEvent.OnBack)
        assertEquals(OnboardingPage.WELCOME, vm.uiState.value.page)

        repeat(10) { vm.onEvent(OnboardingUiEvent.OnNext) }
        assertEquals(OnboardingPage.READY, vm.uiState.value.page)

        vm.onEvent(OnboardingUiEvent.OnBack)
        assertEquals(OnboardingPage.REMINDER, vm.uiState.value.page)
    }
}
