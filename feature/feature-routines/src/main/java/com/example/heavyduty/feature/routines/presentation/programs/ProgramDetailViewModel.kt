package com.example.heavyduty.feature.routines.presentation.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Program
import com.example.heavyduty.core.domain.model.ProgramWeek
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.repository.ProgramRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
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
    /** Routines not currently assigned to any program week — candidates to reuse as a training day. */
    val availableRoutines: List<Routine> = emptyList(),
    val addDayTargetWeekId: Long? = null,
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val routineRepository: RoutineRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val programId: Long = checkNotNull(savedStateHandle["programId"])

    private val _uiState = MutableStateFlow(ProgramDetailUiState())
    val uiState: StateFlow<ProgramDetailUiState> = _uiState.asStateFlow()

    init {
        loadProgram()
        viewModelScope.launch {
            routineRepository.getAllRoutines().collect { routines ->
                val available = routines.filter { it.programWeekId == null }
                _uiState.update { it.copy(availableRoutines = available) }
            }
        }
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

    fun openAddDayDialog(weekId: Long) {
        _uiState.update { it.copy(addDayTargetWeekId = weekId) }
    }

    fun dismissAddDayDialog() {
        _uiState.update { it.copy(addDayTargetWeekId = null) }
    }

    /** Creates a brand-new blank routine for this training day. */
    fun addNewTrainingDay(weekId: Long, name: String) {
        val week = _uiState.value.program?.weeks?.find { it.id == weekId }
        viewModelScope.launch {
            val newRoutine = Routine(
                name = name,
                programWeekId = weekId,
                dayOfWeek = (week?.days?.size ?: 0) + 1,
            )
            routineRepository.insertRoutine(newRoutine)
            dismissAddDayDialog()
            loadProgram() // reload to get the new routine in the list
        }
    }

    /** Reuses one of the user's existing (unassigned) routines as this week's training day. */
    fun assignExistingRoutine(weekId: Long, routine: Routine) {
        val week = _uiState.value.program?.weeks?.find { it.id == weekId }
        viewModelScope.launch {
            routineRepository.updateRoutine(
                routine.copy(programWeekId = weekId, dayOfWeek = (week?.days?.size ?: 0) + 1),
            )
            dismissAddDayDialog()
            loadProgram()
        }
    }

    /** Unassigns a routine from this program week without deleting it — it stays in the routine library. */
    fun removeTrainingDay(routine: Routine) {
        viewModelScope.launch {
            routineRepository.updateRoutine(routine.copy(programWeekId = null, dayOfWeek = null))
            loadProgram()
        }
    }
}
