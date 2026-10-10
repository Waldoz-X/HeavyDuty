package com.example.heavyduty.feature.exercises.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.searchExercises
import com.example.heavyduty.feature.exercises.domain.usecase.GetExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The whole catalog (~1.3k exercises) is filtered in memory, so the search, the muscle filter
 * and the equipment filter always combine — typing no longer drops the selected muscle — and
 * the search can understand Spanish terms (see [searchExercises]).
 */
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseListUiState())
    val uiState: StateFlow<ExerciseListUiState> = _uiState.asStateFlow()

    // A Channel, not a SharedFlow: an effect sent while the screen isn't collecting (rotation,
    // background) waits for it instead of being dropped, and is delivered exactly once.
    private val _uiEffect = Channel<ExerciseListUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ExerciseListUiEffect> = _uiEffect.receiveAsFlow()

    private val filters = MutableStateFlow(Filters())

    init {
        observeExercises()
    }

    fun onEvent(event: ExerciseListUiEvent) {
        when (event) {
            is ExerciseListUiEvent.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                filters.update { it.copy(query = event.query) }
            }

            is ExerciseListUiEvent.OnFilterSelect -> {
                val newFilter = if (_uiState.value.selectedFilter == event.filter) null else event.filter
                _uiState.update { it.copy(selectedFilter = newFilter) }
                filters.update { it.copy(muscleGroup = newFilter) }
            }

            is ExerciseListUiEvent.OnEquipmentFilterSelect -> {
                val newEquipment = if (_uiState.value.selectedEquipment == event.equipment) null else event.equipment
                _uiState.update { it.copy(selectedEquipment = newEquipment) }
                filters.update { it.copy(equipment = newEquipment) }
            }

            is ExerciseListUiEvent.OnExerciseClick -> {
                viewModelScope.launch {
                    _uiEffect.send(ExerciseListUiEffect.NavigateToDetail(event.exerciseId))
                }
            }

            is ExerciseListUiEvent.OnToggleSelection -> {
                _uiState.update { state ->
                    val selected = state.selectedIds
                    state.copy(
                        // Insertion-ordered, so exercises are added in the order they were picked.
                        selectedIds = if (event.exerciseId in selected) {
                            selected - event.exerciseId
                        } else {
                            selected + event.exerciseId
                        }
                    )
                }
            }

            is ExerciseListUiEvent.OnPreviewExercise -> {
                _uiState.update { it.copy(previewExercise = event.exercise) }
            }
        }
    }

    private fun observeExercises() {
        viewModelScope.launch {
            combine(getExercisesUseCase(), filters) { all, filters -> filter(all, filters) }
                .flowOn(defaultDispatcher)
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { (exercises, availableEquipment) ->
                    _uiState.update {
                        it.copy(
                            exercises = exercises,
                            availableEquipment = availableEquipment,
                            isLoading = false,
                            loadFailed = false
                        )
                    }
                }
        }
    }

    /** Matching exercises, plus the equipment present among them before the equipment filter. */
    private fun filter(all: List<Exercise>, filters: Filters): Pair<List<Exercise>, List<String>> {
        val byMuscleAndQuery = all
            .filter { filters.muscleGroup == null || it.muscleGroup == filters.muscleGroup }
            .searchExercises(filters.query)
        val availableEquipment = byMuscleAndQuery
            .mapNotNull { it.equipment.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
        val result = filters.equipment?.let { eq -> byMuscleAndQuery.filter { it.equipment == eq } } ?: byMuscleAndQuery
        return result to availableEquipment
    }

    private data class Filters(val query: String = "", val muscleGroup: MuscleGroup? = null, val equipment: String? = null)
}
