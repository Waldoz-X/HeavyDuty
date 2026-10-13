package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.domain.model.ExerciseSetSummary
import com.example.heavyduty.core.domain.model.HARD_SET_INTENSITY
import com.example.heavyduty.core.domain.model.TrainingTotals
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.repository.TrainingStatsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** The stats answered by aggregate SQL — TrainingStatsFromHistory defines what they mean. */
@Singleton
class TrainingStatsRepositoryImpl @Inject constructor(private val workoutDao: WorkoutDao) : TrainingStatsRepository {

    override fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummary>> = workoutDao.getExerciseSetSummaries().map { rows ->
        rows.map { row ->
            val hasBest = row.bestOneRepMaxKg != null
            ExerciseSetSummary(
                exerciseId = row.exerciseId,
                workingSets = row.workingSets,
                bestOneRepMaxKg = row.bestOneRepMaxKg?.toFloat(),
                bestSetWeightKg = row.bestSetWeightKg.takeIf { hasBest },
                bestSetReps = row.bestSetReps.takeIf { hasBest },
                bestSetAt = row.bestSetAt.takeIf { hasBest }
            )
        }
    }

    override fun getLastHardSetTimes(): Flow<Map<Long, Long>> =
        workoutDao.getLastHardSetTimes(HARD_SET_INTENSITY).map { rows -> rows.associate { it.exerciseId to it.lastHardSetAt } }

    override fun getAllTimeTotals(): Flow<TrainingTotals> =
        combine(workoutDao.getCompletedSessionTotals(), workoutDao.getWorkingSetTotals()) { sessions, sets ->
            TrainingTotals(
                workouts = sessions.workouts,
                durationSecs = sessions.durationSecs,
                volumeKg = sets.volumeKg.toFloat(),
                sets = sets.sets
            )
        }

    override fun getLatestCompletedSession(): Flow<WorkoutSession?> = workoutDao.getLatestCompletedSession().map { it?.toDomain() }

    override fun getBestWeightsBefore(time: Long): Flow<Map<Long, Float>> =
        workoutDao.getBestWeightsBefore(time).map { rows -> rows.associate { it.exerciseId to it.weightKg } }
}
