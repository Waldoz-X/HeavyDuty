package com.example.heavyduty.feature.dashboard.presentation

import com.example.heavyduty.feature.dashboard.domain.model.BodyWeightSummaryUi
import com.example.heavyduty.feature.dashboard.domain.model.DayPlanUi

/**
 * Immutable UI state for the dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val weeklyWorkoutsCount: Int = 0,
    val totalTimeSeconds: Int = 0,
    val weeklyVolumeKg: Float = 0f,
    /** Name of the last finished workout; null when it has none (the screen shows a default). */
    val recentWorkoutName: String? = null,
    /** When the last finished workout ended; null when no workout has been completed yet. */
    val recentWorkoutEndTime: Long? = null,
    val weeklyMuscleVolume: Map<com.example.heavyduty.core.domain.model.MuscleGroup, Float> = emptyMap(),
    val weekPlan: List<DayPlanUi> = emptyList(),
    val weekOffset: Int = 0,
    val plannedDaysThisWeek: Int = 0,
    val streakDays: Int = 0,
    val bodyWeightSummary: BodyWeightSummaryUi = BodyWeightSummaryUi(),
    val showLogWeightSheet: Boolean = false,
    val error: String? = null,
)

/**
 * User-initiated events from the dashboard screen.
 */
sealed interface DashboardUiEvent {
    object OnStartFreeWorkout : DashboardUiEvent
    object OnChooseRoutine : DashboardUiEvent
    object OnWeekPrev : DashboardUiEvent
    object OnWeekNext : DashboardUiEvent
    data class OnStartTodayRoutine(val routineId: Long) : DashboardUiEvent
    object OnOpenLogWeight : DashboardUiEvent
    object OnDismissLogWeight : DashboardUiEvent
    data class OnLogWeight(val weightKg: Float) : DashboardUiEvent
}

/**
 * One-shot effects emitted by the ViewModel.
 */
sealed interface DashboardUiEffect {
    object NavigateToActiveWorkout : DashboardUiEffect
    object NavigateToRoutines : DashboardUiEffect
    data class NavigateToRoutine(val routineId: Long) : DashboardUiEffect
    data class ShowError(val message: String) : DashboardUiEffect
}
