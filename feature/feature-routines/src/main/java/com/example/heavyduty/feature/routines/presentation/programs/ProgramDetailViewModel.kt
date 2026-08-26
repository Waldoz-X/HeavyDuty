package com.example.heavyduty.feature.routines.presentation.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Program
import com.example.heavyduty.core.domain.model.ProgramWeek
import com.example.heavyduty.core.domain.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramDetailUiState(
    val program: Program? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val programId: Long = checkNotNull(savedStateHandle["programId"])
    
    private val _uiState = MutableStateFlow(ProgramDetailUiState())
    val uiState: StateFlow<ProgramDetailUiState> = _uiState.asStateFlow()

    init {
        loadProgram()
    }

    private fun loadProgram() {
        viewModelScope.launch {
            val program = programRepository.getProgramById(programId)
            _uiState.update { it.copy(program = program, isLoading = false) }
        }
    }

    fun addWeek() {
        val currentProgram = _uiState.value.program ?: return
        viewModelScope.launch {
            val newWeek = ProgramWeek(
                programId = programId,
                weekNumber = currentProgram.weeks.size + 1
            )
            programRepository.addWeekToProgram(newWeek)
            loadProgram() // reload to get new ID
        }
    }
}
