package com.example.heavyduty.feature.dashboard.presentation

/**
 * Immutable UI state for the dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val weeklyWorkoutsCount: Int = 0,
    val totalTimeSeconds: Int = 0,
    val recentWorkoutName: String? = null,
    val recentWorkoutTime: String? = null,
    val error: String? = null
)

/**
 * User-initiated events from the dashboard screen.
 */
sealed interface DashboardUiEvent {
    object OnStartFreeWorkout : DashboardUiEvent
    object OnChooseRoutine : DashboardUiEvent
}

/**
 * One-shot effects emitted by the ViewModel.
 */
sealed interface DashboardUiEffect {
    object NavigateToActiveWorkout : DashboardUiEffect
    object NavigateToRoutines : DashboardUiEffect
    data class ShowError(val message: String) : DashboardUiEffect
}
