package com.example.heavyduty.feature.history.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.history.domain.usecase.DeleteWorkoutUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetWorkoutDetailUseCase
import com.example.heavyduty.feature.history.domain.usecase.UpdateLoggedSetUseCase
import com.example.heavyduty.feature.history.domain.usecase.UpdateWorkoutDurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A finished workout from the history, open to corrections: duration, sets, or deleting it. */
@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    getWorkoutDetail: GetWorkoutDetailUseCase,
    private val updateWorkoutDuration: UpdateWorkoutDurationUseCase,
    private val updateLoggedSet: UpdateLoggedSetUseCase,
    private val deleteWorkout: DeleteWorkoutUseCase,
    private val workoutRepository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            getWorkoutDetail(sessionId)
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { session -> _uiState.update { it.copy(session = session, isLoading = false) } }
        }
    }

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<WorkoutDetailUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<WorkoutDetailUiEffect> = _uiEffect.receiveAsFlow()

    fun onEvent(event: WorkoutDetailUiEvent) {
        when (event) {
            is WorkoutDetailUiEvent.OnSaveDuration -> save(R.string.history_invalid_duration) {
                val minutes = event.minutes.trim().toIntOrNull() ?: return@save false
                updateWorkoutDuration(sessionId, minutes)
            }

            is WorkoutDetailUiEvent.OnSaveSet -> save(R.string.history_invalid_set) {
                val weight = event.weight.trim().replace(',', '.').toFloatOrNull() ?: return@save false
                val reps = event.reps.trim().toIntOrNull() ?: return@save false
                updateLoggedSet(event.set, weightKg = weight.toKgFrom(event.unit), reps = reps)
            }

            is WorkoutDetailUiEvent.OnDeleteSet -> save(R.string.history_error_save) {
                workoutRepository.deleteSet(event.set)
                _uiEffect.send(WorkoutDetailUiEffect.SetDeleted(event.set))
                true
            }

            // Re-inserted with its original id and number, so it's the same set as before.
            is WorkoutDetailUiEvent.OnUndoDeleteSet -> save(R.string.history_error_save) {
                workoutRepository.insertSet(event.set)
                true
            }

            WorkoutDetailUiEvent.OnDeleteWorkout -> save(R.string.history_error_save) {
                deleteWorkout(sessionId)
                _uiEffect.send(WorkoutDetailUiEffect.WorkoutDeleted)
                true
            }
        }
    }

    /** Runs [change]; a refused value shows [invalidMessage], a failure a generic error. */
    private fun save(invalidMessage: Int, change: suspend () -> Boolean) {
        viewModelScope.launch {
            try {
                if (!change()) _uiEffect.send(WorkoutDetailUiEffect.ShowMessage(invalidMessage))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiEffect.send(WorkoutDetailUiEffect.ShowMessage(R.string.history_error_save))
            }
        }
    }
}
