package com.example.heavyduty.feature.profile.presentation.onboarding

import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.WeightUnit

/** The first-run welcome's steps, in order. */
enum class OnboardingPage { WELCOME, UNIT, REMINDER, READY }

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val name: String = "",
    val unit: WeightUnit = WeightUnit.KG,
    val reminder: ReminderSettings = ReminderSettings(),
    /** Saving the choices; the buttons wait so nothing is sent twice. */
    val isFinishing: Boolean = false
)

sealed interface OnboardingUiEvent {
    data class OnNameChange(val name: String) : OnboardingUiEvent
    data class OnUnitChange(val unit: WeightUnit) : OnboardingUiEvent
    data class OnReminderEnabledChange(val enabled: Boolean) : OnboardingUiEvent
    data class OnReminderTimeChange(val hour: Int, val minute: Int) : OnboardingUiEvent
    data object OnNext : OnboardingUiEvent
    data object OnBack : OnboardingUiEvent

    /** "Omitir" or the last page's buttons: whatever was chosen so far is kept. */
    data class OnFinish(val createFirstRoutine: Boolean) : OnboardingUiEvent
}

sealed interface OnboardingUiEffect {
    data class Finished(val createFirstRoutine: Boolean) : OnboardingUiEffect
}
