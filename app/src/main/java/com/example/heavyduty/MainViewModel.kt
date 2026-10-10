package com.example.heavyduty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide state the whole UI reads: settings, the workout in progress, deep-link requests. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    workoutRepository: WorkoutRepository,
    restTimerController: RestTimerController
) : ViewModel() {
    val preferredWeightUnit: StateFlow<WeightUnit> = userPreferencesRepository.preferredWeightUnit
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    /** The running workout, for the "workout in progress" bar shown on every other screen. */
    val activeWorkout: StateFlow<ActiveWorkoutSummary?> = workoutRepository.getActiveSession()
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(null) else workoutRepository.getSessionWithSets(sessionId)
        }
        .combine(restTimerController.restState) { session, rest ->
            session?.let {
                ActiveWorkoutSummary(
                    sessionName = it.sessionName,
                    startTime = it.startTime,
                    setsLogged = it.sets.count { set -> !set.isWarmup },
                    rest = rest
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _pendingSection = MutableStateFlow<String?>(null)

    /** A section a notification asked to open (see core.common.navigation.AppSections). */
    val pendingSection: StateFlow<String?> = _pendingSection.asStateFlow()

    fun openSection(section: String?) {
        if (section != null) _pendingSection.value = section
    }

    fun onSectionOpen() {
        _pendingSection.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class ActiveWorkoutSummary(val sessionName: String, val startTime: Long, val setsLogged: Int, val rest: RestTimerState?)
