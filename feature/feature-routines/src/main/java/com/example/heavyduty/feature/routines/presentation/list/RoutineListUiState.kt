package com.example.heavyduty.feature.routines.presentation.list

import androidx.annotation.StringRes
import com.example.heavyduty.core.domain.model.Routine

/**
 * Immutable UI state for the routine list screen.
 */
data class RoutineListUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)

/**
 * User-initiated events from the routine list screen.
 */
sealed interface RoutineListUiEvent {
    data class OnRoutineClick(val routineId: Long) : RoutineListUiEvent
    data object OnCreateRoutine : RoutineListUiEvent
    data class OnStartRoutine(val routineId: Long) : RoutineListUiEvent
    /** Sent once the user confirmed the deletion dialog. */
    data class OnDeleteRoutine(val routine: Routine) : RoutineListUiEvent
}

/**
 * One-shot effects for the routine list screen.
 */
sealed interface RoutineListUiEffect {
    data class NavigateToEditor(val routineId: Long?) : RoutineListUiEffect
    data class StartRoutine(val routineId: Long) : RoutineListUiEffect
    data class RoutineDeleted(val name: String) : RoutineListUiEffect
    data class ShowMessage(@param:StringRes val messageResId: Int) : RoutineListUiEffect
}
