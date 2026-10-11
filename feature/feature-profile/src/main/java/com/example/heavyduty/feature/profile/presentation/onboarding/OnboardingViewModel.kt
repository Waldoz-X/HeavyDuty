package com.example.heavyduty.feature.profile.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The first-run welcome: a name, the unit, the training reminder, then into the app. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val reminderScheduler: TrainingReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<OnboardingUiEffect> = _uiEffect.receiveAsFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.OnNameChange -> _uiState.update { it.copy(name = event.name) }

            is OnboardingUiEvent.OnUnitChange -> _uiState.update { it.copy(unit = event.unit) }

            is OnboardingUiEvent.OnReminderEnabledChange -> _uiState.update {
                it.copy(reminder = it.reminder.copy(enabled = event.enabled))
            }

            is OnboardingUiEvent.OnReminderTimeChange -> _uiState.update {
                it.copy(reminder = it.reminder.copy(hour = event.hour, minute = event.minute))
            }

            OnboardingUiEvent.OnNext -> movePage(+1)

            OnboardingUiEvent.OnBack -> movePage(-1)

            is OnboardingUiEvent.OnFinish -> finish(event.createFirstRoutine)
        }
    }

    private fun movePage(step: Int) {
        val pages = OnboardingPage.entries
        _uiState.update { it.copy(page = pages[(it.page.ordinal + step).coerceIn(0, pages.lastIndex)]) }
    }

    private fun finish(createFirstRoutine: Boolean) {
        val state = _uiState.value
        if (state.isFinishing) return
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            state.name.trim().takeIf { it.isNotEmpty() }?.let { preferences.setDisplayName(it) }
            preferences.setPreferredWeightUnit(state.unit)
            preferences.setTrainingReminder(state.reminder)
            reminderScheduler.schedule(state.reminder)
            // Marking it completed is the host's job (see OnboardingScreen's onFinish): doing it here
            // swapped this screen out — and its effect collector with it — before this arrived.
            _uiEffect.send(OnboardingUiEffect.Finished(createFirstRoutine))
        }
    }
}
