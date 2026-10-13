package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.nextPrescription
import com.example.heavyduty.core.domain.model.other
import com.example.heavyduty.core.domain.model.toProgressionConfig
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the screen knows about the exercise on it besides today's sets: the suggested next set
 * and last session's numbers, both from its history, and the unit its weight is entered in.
 */
internal class CurrentExerciseController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ActiveWorkoutUiState>,
    private val draft: WorkoutDraft,
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository
) {
    private var infoJob: Job? = null

    /**
     * Loads the current exercise's suggestion and last-session sets from its history in OTHER
     * sessions — today's own sets are excluded so the suggestion stays stable as sets are logged.
     */
    fun loadInfo() {
        val routineExercise = uiState.value.currentExercise
        infoJob?.cancel()
        if (routineExercise == null) {
            uiState.update { it.copy(suggestedPrescription = null, previousSets = emptyMap()) }
            return
        }
        infoJob = scope.launch {
            val history = historyExcludingCurrentSession(routineExercise.exerciseId)
            val prescription = nextPrescription(history, routineExercise.toProgressionConfig())
            val lastSessionId = history.filter { it.completedAt != null }.maxByOrNull { it.completedAt!! }?.sessionId
            val previousSets = history.filter { it.sessionId == lastSessionId }.associateBy { it.setNumber }
            uiState.update { state ->
                if (state.currentExercise?.exerciseId != routineExercise.exerciseId) return@update state
                state.copy(
                    // A free workout has no plan to progress, only last time's numbers.
                    suggestedPrescription = if (state.allowAddingExercises) null else prescription,
                    previousSets = previousSets
                )
            }
        }
    }

    /** Stops a load in progress: the workout it was for is over. */
    fun cancel() {
        infoJob?.cancel()
    }

    /** This exercise's logged sets from every OTHER session — excludes today's own sets. */
    suspend fun historyExcludingCurrentSession(exerciseId: Long): List<WorkoutSet> {
        val sessionId = uiState.value.sessionId
        return trainingHistoryRepository.getSetsByExerciseId(exerciseId).firstOrNull().orEmpty()
            .filter { it.sessionId != sessionId }
    }

    fun useSuggestion() {
        val state = uiState.value
        val suggestion = state.suggestedPrescription ?: return
        val unit = state.currentUnit
        draft.update {
            it.copy(
                repsInput = suggestion.reps?.toString() ?: it.repsInput,
                weightInput = suggestion.weightKg?.takeIf { kg -> kg > 0f }?.kgToRounded(unit)?.let(::formatInput) ?: it.weightInput
            )
        }
    }

    /**
     * Switches the current exercise between kg and lb, and remembers it: in a routine, for that
     * routine's exercise (the same machine next time); in a free workout, as the exercise's
     * default. Sets already logged keep the unit they were entered in.
     */
    fun toggleUnit() {
        val state = uiState.value
        val index = state.currentExerciseIndex
        val routineExercise = state.currentExercise ?: return
        val newUnit = routineExercise.displayUnit.other()
        val updated = routineExercise.copy(weightUnit = newUnit)
        draft.update {
            it.copy(
                routineExercises = it.routineExercises.mapIndexed { i, entry -> if (i == index) updated else entry },
                // What was typed was in the old unit; clear it rather than silently reinterpret it.
                weightInput = ""
            )
        }
        scope.launch {
            try {
                if (!state.allowAddingExercises && routineExercise.id > 0) {
                    routineRepository.updateRoutineExercise(updated.copy(exercise = null))
                } else {
                    routineExercise.exercise?.let { exerciseRepository.updateExercise(it.copy(weightUnit = newUnit)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Only the remembered default failed; this session still uses the new unit.
            }
        }
    }
}

/** "62.5" stays "62.5", "60.0" becomes "60" — what a lifter would type. */
private fun formatInput(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else value.formatOneDecimal()
