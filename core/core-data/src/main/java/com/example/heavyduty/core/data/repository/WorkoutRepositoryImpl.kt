package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.database.mapper.toEntity
import com.example.heavyduty.core.domain.model.ExerciseSetSummary
import com.example.heavyduty.core.domain.model.HARD_SET_INTENSITY
import com.example.heavyduty.core.domain.model.TrainingTotals
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {

    override fun getAllSessions(): Flow<List<WorkoutSession>> =
        workoutDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override fun getSessionById(id: Long): Flow<WorkoutSession?> =
        workoutDao.getSessionById(id).map { it?.toDomain() }

    override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> =
        workoutDao.getSessionWithSets(id).map { it?.toDomain() }

    override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> =
        workoutDao.getSessionsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> =
        workoutDao.getCompletedSessionsByRoutineId(routineId).map { list -> list.map { it.toDomain() } }

    override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutDao.getSetsByExerciseId(exerciseId).map { list -> list.map { it.toDomain() } }

    override fun getAllCompletedSets(): Flow<List<WorkoutSet>> =
        workoutDao.getAllCompletedSets().map { list -> list.map { it.toDomain() } }

    override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>> =
        workoutDao.getCompletedSetsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getActiveSession(): Flow<WorkoutSession?> =
        workoutDao.getActiveSession().map { it?.toDomain() }

    override suspend fun insertSession(session: WorkoutSession): Long =
        workoutDao.insertSession(session.toEntity())

    override suspend fun updateSession(session: WorkoutSession) =
        workoutDao.updateSession(session.toEntity())

    override suspend fun deleteSession(session: WorkoutSession) =
        workoutDao.deleteSession(session.toEntity())

    override suspend fun insertSet(set: WorkoutSet): Long =
        workoutDao.insertSet(set.toEntity())

    override suspend fun updateSet(set: WorkoutSet) =
        workoutDao.updateSet(set.toEntity())

    override suspend fun deleteSet(set: WorkoutSet) =
        workoutDao.deleteSet(set.toEntity())

    // Aggregates answered by SQL — see the defaults in WorkoutRepository for what they mean.

    override fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummary>> =
        workoutDao.getExerciseSetSummaries().map { rows ->
            rows.map { row ->
                val hasBest = row.bestOneRepMaxKg != null
                ExerciseSetSummary(
                    exerciseId = row.exerciseId,
                    workingSets = row.workingSets,
                    bestOneRepMaxKg = row.bestOneRepMaxKg?.toFloat(),
                    bestSetWeightKg = row.bestSetWeightKg.takeIf { hasBest },
                    bestSetReps = row.bestSetReps.takeIf { hasBest },
                    bestSetAt = row.bestSetAt.takeIf { hasBest },
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
                sets = sets.sets,
            )
        }

    override fun getLatestCompletedSession(): Flow<WorkoutSession?> =
        workoutDao.getLatestCompletedSession().map { it?.toDomain() }

    override fun getBestWeightsBefore(time: Long): Flow<Map<Long, Float>> =
        workoutDao.getBestWeightsBefore(time).map { rows -> rows.associate { it.exerciseId to it.weightKg } }
}
