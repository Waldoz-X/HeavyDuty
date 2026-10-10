package com.example.heavyduty.feature.timer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.timer.engine.TimerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Thin adapter over [TimerEngine]: the engine (a process-scoped singleton) owns the actual
 * countdown so it survives this ViewModel being cleared — this class only forwards events and
 * exposes the engine's state/effects in a lifecycle-aware way for Compose.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(private val timerEngine: TimerEngine) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = timerEngine.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = timerEngine.state.value
    )

    val uiEffect = timerEngine.effects

    fun onEvent(event: TimerUiEvent) {
        timerEngine.onEvent(event)
    }
}
