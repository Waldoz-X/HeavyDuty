package com.example.heavyduty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.isForgotten
import com.example.heavyduty.core.domain.model.lastActivityAt
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.workout.domain.usecase.CompleteWorkoutUseCase
import com.example.heavyduty.feature.workout.domain.usecase.DiscardWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state the whole UI reads: settings, the workout in progress, deep-link requests. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    workoutRepository: WorkoutRepository,
    private val restTimerController: RestTimerController,
    private val completeWorkout: CompleteWorkoutUseCase,
    private val discardWorkout: DiscardWorkoutUseCase,
    private val clock: Clock
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
                    sessionId = it.id,
                    sessionName = it.sessionName,
                    startTime = it.startTime,
                    setsLogged = it.sets.count { set -> !set.isWarmup },
                    lastActivityAt = lastActivityAt(it.startTime, it.sets),
                    rest = rest
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** The last activity the lifter said "I'm still training" to, per workout. */
    private val keptGoing = MutableStateFlow<Pair<Long, Long>?>(null)

    /** Re-checked every minute: a workout becomes forgotten just by time passing. */
    private val now = flow {
        while (true) {
            emit(clock.nowMillis())
            delay(FORGOTTEN_CHECK_MS)
        }
    }

    /**
     * The workout in progress when nothing's been logged in it for hours — most likely left
     * open by mistake, so the app asks instead of letting it run on for a day.
     */
    val forgottenWorkout: StateFlow<ForgottenWorkout?> = combine(activeWorkout, now, keptGoing) { workout, now, kept ->
        workout
            ?.takeIf { isForgotten(it.lastActivityAt, now) && kept != (it.sessionId to it.lastActivityAt) }
            ?.let {
                ForgottenWorkout(it.sessionId, it.sessionName, it.setsLogged, idleHours = ((now - it.lastActivityAt) / HOUR_MS).toInt())
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Ends the forgotten workout at its last set (see CompleteWorkoutUseCase). */
    fun finishForgottenWorkout() = endForgottenWorkout { completeWorkout(it) }

    fun discardForgottenWorkout() = endForgottenWorkout { discardWorkout(it) }

    /** Not forgotten after all: don't ask again until it goes quiet once more. */
    fun keepTraining() {
        val workout = activeWorkout.value ?: return
        keptGoing.value = workout.sessionId to workout.lastActivityAt
    }

    private fun endForgottenWorkout(end: suspend (sessionId: Long) -> Unit) {
        val workout = forgottenWorkout.value ?: return
        viewModelScope.launch {
            end(workout.sessionId)
            restTimerController.skipRest()
        }
    }

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
        const val FORGOTTEN_CHECK_MS = 60_000L
        const val HOUR_MS = 3_600_000L
    }
}

data class ActiveWorkoutSummary(
    val sessionId: Long,
    val sessionName: String,
    val startTime: Long,
    val setsLogged: Int,
    /** The latest set's time, or the start — see lastActivityAt. */
    val lastActivityAt: Long,
    val rest: RestTimerState?
)

/** A workout left open with nothing logged for [idleHours]; see MainViewModel.forgottenWorkout. */
data class ForgottenWorkout(val sessionId: Long, val sessionName: String, val setsLogged: Int, val idleHours: Int)
