package com.example.heavyduty.feature.exercises.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.feature.exercises.domain.usecase.GetExercisesUseCase
import com.example.heavyduty.feature.exercises.domain.usecase.SearchExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase,
    private val searchExercisesUseCase: SearchExercisesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseListUiState())
    val uiState: StateFlow<ExerciseListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ExerciseListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    // Triggers re-collection when search or filter changes
    private val _queryTrigger = MutableStateFlow(QueryFilter())

    init {
        observeExercises()
    }

    fun onEvent(event: ExerciseListUiEvent) {
        when (event) {
            is ExerciseListUiEvent.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                _queryTrigger.value = QueryFilter(event.query, _uiState.value.selectedFilter)
            }
            is ExerciseListUiEvent.OnFilterSelect -> {
                val newFilter = if (_uiState.value.selectedFilter == event.filter) null else event.filter
                _uiState.update { it.copy(selectedFilter = newFilter) }
                _queryTrigger.value = QueryFilter(_uiState.value.searchQuery, newFilter)
            }
            is ExerciseListUiEvent.OnExerciseClick -> {
                viewModelScope.launch {
                    _uiEffect.emit(ExerciseListUiEffect.NavigateToDetail(event.exerciseId))
                }
            }
        }
    }

    private fun observeExercises() {
        viewModelScope.launch {
            _queryTrigger.flatMapLatest { filter ->
                if (filter.query.isNotBlank()) {
                    searchExercisesUseCase(filter.query)
                } else {
                    getExercisesUseCase(filter.muscleGroup)
                }
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Error desconocido")
                    }
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(
                            exercises = exercises,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private data class QueryFilter(
        val query: String = "",
        val muscleGroup: MuscleGroup? = null,
    )
}
