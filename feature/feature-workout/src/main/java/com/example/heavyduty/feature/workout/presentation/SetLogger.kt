package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.isUnusuallyHeavy
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.LogSetUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Logging the next set from what was typed, deleting a logged one and undoing that. */
internal class SetLogger(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ActiveWorkoutUiState>,
    private val draft: WorkoutDraft,
    private val effects: WorkoutEffects,
    private val clocks: WorkoutClocks,
    private val currentExercise: CurrentExerciseController,
    private val logSetUseCase: LogSetUseCase,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock
) {
    /**
     * Logs the next set. A weight far above the exercise's best is held back first (see
     * [isUnusuallyHeavy]) until [confirmUnusualWeight]; [weightConfirmed] skips that check.
     */
    fun logSet(weightConfirmed: Boolean = false) {
        val state = uiState.value
        if (state.isLoggingSet) return
        val sessionId = state.sessionId ?: return
        val exercise = state.currentExercise ?: return
        val entry = state.setEntry()
            ?: return effects.showMessage(if (state.isTimedInput) R.string.workout_error_enter_time else R.string.workout_error_enter_reps)
        val set = WorkoutSet(
            sessionId = sessionId,
            exerciseId = exercise.exerciseId,
            exercise = exercise.exercise,
            setNumber = state.currentSetNumber,
            reps = entry.reps,
            weight = entry.weightKg,
            durationSecs = entry.durationSecs,
            isCompleted = true,
            isWarmup = state.isWarmupInput,
            effort = entry.effort,
            weightUnit = state.currentUnit
        )

        uiState.update { it.copy(isLoggingSet = true) }
        scope.launch {
            try {
                if (!weightConfirmed && heldBackForUnusualWeight(set, state)) return@launch
                save(set, restSeconds = exercise.restBetweenSetsSecs, state = state)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                effects.showMessage(R.string.workout_error_log_set)
            } finally {
                uiState.update { it.copy(isLoggingSet = false) }
            }
        }
    }

    /** True when [set]'s weight is far above the exercise's best: it waits for [confirmUnusualWeight]. */
    private suspend fun heldBackForUnusualWeight(set: WorkoutSet, state: ActiveWorkoutUiState): Boolean {
        val best = bestWeightKg(set.exerciseId, earlierToday = state.setsFor(set.exerciseId))
        if (!isUnusuallyHeavy(set.weight, best)) return false
        uiState.update { it.copy(unusualWeight = UnusualWeight(set.weight!!, best!!, state.currentUnit)) }
        return true
    }

    private suspend fun save(set: WorkoutSet, restSeconds: Int, state: ActiveWorkoutUiState) {
        val setId = logSetUseCase(
            sessionId = set.sessionId,
            exerciseId = set.exerciseId,
            setNumber = set.setNumber,
            reps = set.reps,
            weight = set.weight,
            durationSecs = set.durationSecs,
            isWarmup = set.isWarmup,
            effort = set.effort,
            restSeconds = restSeconds,
            weightUnit = state.currentUnit
        )
        val loggedSet = set.copy(id = setId, completedAt = clock.nowMillis())
        clocks.resetHold()
        // Shown right away; the database emission that follows carries the same set.
        draft.update {
            it.copy(
                completedSets = it.completedSets.filter { logged -> logged.id != setId } + loggedSet,
                repsInput = "",
                weightInput = "",
                isWarmupInput = false,
                effortInput = ""
            )
        }
        celebrateIfRecord(loggedSet, earlierToday = state.setsFor(set.exerciseId), unit = state.currentUnit)
    }

    fun confirmUnusualWeight() {
        uiState.update { it.copy(unusualWeight = null) }
        logSet(weightConfirmed = true)
    }

    fun dismissUnusualWeight() {
        uiState.update { it.copy(unusualWeight = null) }
    }

    /** The heaviest working set of the exercise, past or today; null when there's none or history can't be read. */
    private suspend fun bestWeightKg(exerciseId: Long, earlierToday: List<WorkoutSet>): Float? {
        val history = try {
            currentExercise.historyExcludingCurrentSession(exerciseId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        return (history + earlierToday).filter { !it.isWarmup }.mapNotNull { it.weight }.maxOrNull()
    }

    /** Celebrates a set that beats every earlier set of the exercise (see [newRecordEstimate]). */
    private suspend fun celebrateIfRecord(set: WorkoutSet, earlierToday: List<WorkoutSet>, unit: WeightUnit) {
        // Only a bonus: if history can't be read, the set is still logged, just not celebrated.
        val history = try {
            currentExercise.historyExcludingCurrentSession(set.exerciseId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        val estimate = newRecordEstimate(set, earlierSets = history + earlierToday) ?: return
        effects.send(
            ActiveWorkoutUiEffect.PersonalRecord(
                exerciseName = set.exercise?.displayName.orEmpty(),
                estimatedOneRepMaxKg = estimate,
                unit = unit
            )
        )
    }

    fun deleteSet(set: WorkoutSet) {
        scope.launch {
            try {
                workoutRepository.deleteSet(set)
                uiState.update { it.copy(completedSets = it.completedSets.filter { s -> s.id != set.id }) }
                effects.send(ActiveWorkoutUiEffect.SetDeleted(set))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                effects.showMessage(R.string.workout_error_delete_set)
            }
        }
    }

    /** Undo for [deleteSet]: re-inserts the set with its original id and number. */
    fun restoreSet(set: WorkoutSet) {
        scope.launch {
            try {
                workoutRepository.insertSet(set)
                uiState.update { it.copy(completedSets = it.completedSets.filter { s -> s.id != set.id } + set) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                effects.showMessage(R.string.workout_error_log_set)
            }
        }
    }
}
