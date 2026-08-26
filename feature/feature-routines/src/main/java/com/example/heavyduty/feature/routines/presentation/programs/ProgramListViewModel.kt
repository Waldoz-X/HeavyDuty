package com.example.heavyduty.feature.routines.presentation.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Program
import com.example.heavyduty.core.domain.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramListUiState(
    val programs: List<Program> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProgramListViewModel @Inject constructor(
    private val programRepository: ProgramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgramListUiState())
    val uiState: StateFlow<ProgramListUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            programRepository.getAllPrograms().collect { programs ->
                _uiState.update {
                    it.copy(programs = programs, isLoading = false)
                }
            }
        }
    }

    fun createNewProgram(name: String, totalWeeks: Int) {
        viewModelScope.launch {
            val newProgram = Program(
                name = name,
                totalWeeks = totalWeeks,
            )
            programRepository.createProgram(newProgram)
        }
    }

    fun setActiveProgram(programId: Long) {
        viewModelScope.launch {
            programRepository.setActiveProgram(programId)
        }
    }
}
