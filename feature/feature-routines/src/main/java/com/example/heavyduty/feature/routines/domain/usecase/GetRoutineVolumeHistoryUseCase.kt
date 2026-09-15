package com.example.heavyduty.feature.routines.domain.usecase

import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Total volume (reps * weight, warmups excluded) per completed session of this routine,
 * oldest first — feeds the routine editor's "volume over time" chart.
 */
class GetRoutineVolumeHistoryUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(routineId: Long): Flow<List<Pair<Long, Float>>> =
        workoutRepository.getCompletedSessionsByRoutineId(routineId).map { sessions ->
            sessions.map { session ->
                val volumeKg = session.sets.excludeWarmups()
                    .sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }
                    .toFloat()
                session.startTime to volumeKg
            }
        }
}
