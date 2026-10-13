package com.example.heavyduty.feature.workout.presentation

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * What the lifter has typed but not logged yet — reps, weight and effort — kept in the
 * [SavedStateHandle] too, so it survives Android closing the app in the background mid-set.
 */
internal class WorkoutDraft(private val savedStateHandle: SavedStateHandle, private val uiState: MutableStateFlow<ActiveWorkoutUiState>) {

    /** Changes the state and remembers the inputs it ends up with. */
    fun update(change: (ActiveWorkoutUiState) -> ActiveWorkoutUiState) {
        uiState.update(change)
        val state = uiState.value
        savedStateHandle[KEY_REPS_INPUT] = state.repsInput
        savedStateHandle[KEY_WEIGHT_INPUT] = state.weightInput
        savedStateHandle[KEY_EFFORT_INPUT] = state.effortInput
    }

    /** [state] with the inputs remembered for its workout; a different workout starts with none. */
    fun restore(state: ActiveWorkoutUiState, sameSession: Boolean): ActiveWorkoutUiState = state.copy(
        repsInput = saved(KEY_REPS_INPUT, sameSession),
        weightInput = saved(KEY_WEIGHT_INPUT, sameSession),
        effortInput = saved(KEY_EFFORT_INPUT, sameSession)
    )

    private fun saved(key: String, sameSession: Boolean): String = savedStateHandle.get<String>(key).takeIf { sameSession }.orEmpty()
}

private const val KEY_REPS_INPUT = "repsInput"
private const val KEY_WEIGHT_INPUT = "weightInput"
private const val KEY_EFFORT_INPUT = "effortInput"
