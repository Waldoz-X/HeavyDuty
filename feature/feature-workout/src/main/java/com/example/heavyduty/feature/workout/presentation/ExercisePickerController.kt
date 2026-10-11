package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.searchExercises
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The search for an exercise to add to a free workout; the catalog is read the first time it opens. */
internal class ExercisePickerController(
    private val scope: CoroutineScope,
    private val exerciseRepository: ExerciseRepository,
    private val uiState: MutableStateFlow<ActiveWorkoutUiState>
) {
    private var catalog: List<Exercise>? = null

    fun toggle() {
        val opening = !uiState.value.isPickingExercise
        uiState.update { it.copy(isPickingExercise = opening) }
        if (opening && catalog == null) {
            scope.launch {
                catalog = exerciseRepository.getAllExercises().firstOrNull().orEmpty()
                showResults(uiState.value.exercisePickerQuery)
            }
        }
    }

    fun onQueryChange(query: String) {
        uiState.update { it.copy(exercisePickerQuery = query) }
        showResults(query)
    }

    fun close() {
        uiState.update { it.copy(isPickingExercise = false, exercisePickerQuery = "", exercisePickerResults = emptyList()) }
    }

    private fun showResults(query: String) {
        uiState.update { it.copy(exercisePickerResults = catalog.orEmpty().searchExercises(query).take(RESULTS)) }
    }
}

private const val RESULTS = 50
