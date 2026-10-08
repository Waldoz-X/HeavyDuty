package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/*
 * Shared fakes for the stats use-case tests. The workout fake answers its queries the way
 * WorkoutDao does, so the tests check the use cases against the data the app really hands them.
 */

const val HOUR = 3_600_000L
const val DAY = 24 * HOUR

/** A compound lift: most of each set goes to chest, the rest to triceps and shoulders. */
val BENCH = Exercise(
    id = 1, name = "Press de banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH,
    muscleWeights = mapOf(MuscleGroup.CHEST to 0.6f, MuscleGroup.TRICEPS to 0.25f, MuscleGroup.SHOULDERS to 0.15f),
)
val SQUAT = Exercise(id = 2, name = "Sentadilla", muscleGroup = MuscleGroup.QUADRICEPS, exerciseType = ExerciseType.STRENGTH)
val CURL = Exercise(id = 3, name = "Curl de bíceps", muscleGroup = MuscleGroup.BICEPS, exerciseType = ExerciseType.STRENGTH)
val EXERCISES = listOf(BENCH, SQUAT, CURL)

/** An exercise id with no row — sets of a deleted exercise. */
const val DELETED_EXERCISE_ID = 99L

fun session(id: Long, start: Long, durationMins: Int = 60, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
    WorkoutSession(id = id, startTime = start, totalDurationSecs = durationMins * 60, status = status)

fun set(
    session: Long,
    exercise: Exercise,
    reps: Int,
    weightKg: Float,
    at: Long,
    warmup: Boolean = false,
    completed: Boolean = true,
    unit: WeightUnit? = null,
) = WorkoutSet(
    sessionId = session, exerciseId = exercise.id, reps = reps, weight = weightKg,
    isCompleted = completed, completedAt = if (completed) at else null, isWarmup = warmup, weightUnit = unit,
)

class FakeWorkoutRepository(
    private val sessions: List<WorkoutSession>,
    private val sets: List<WorkoutSet>,
) : WorkoutRepository {
    private val sessionById = sessions.associateBy { it.id }

    /** Logged sets of finished workouts (WorkoutDao.getAllCompletedSets). */
    private fun countedSets() = sets.filter { it.isCompleted && sessionById[it.sessionId]?.status == WorkoutStatus.COMPLETED }

    override fun getAllSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
    override fun getSessionsByDateRange(startTime: Long, endTime: Long) =
        flowOf(sessions.filter { it.startTime in startTime..endTime })
    override fun getAllCompletedSets() = flowOf(countedSets())

    /** A set belongs to the period its workout started in (WorkoutDao.getCompletedSetsByDateRange). */
    override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long) =
        flowOf(countedSets().filter { sessionById.getValue(it.sessionId).startTime in startTime..endTime })

    override fun getSessionById(id: Long): Flow<WorkoutSession?> = flowOf(sessionById[id])
    override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> = flowOf(null)
    override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
    override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> = flowOf(sets.filter { it.exerciseId == exerciseId })
    override fun getActiveSession(): Flow<WorkoutSession?> = flowOf(sessions.firstOrNull { it.status == WorkoutStatus.IN_PROGRESS })
    override suspend fun insertSession(session: WorkoutSession): Long = error("read-only")
    override suspend fun updateSession(session: WorkoutSession) = error("read-only")
    override suspend fun deleteSession(session: WorkoutSession) = error("read-only")
    override suspend fun insertSet(set: WorkoutSet): Long = error("read-only")
    override suspend fun updateSet(set: WorkoutSet) = error("read-only")
    override suspend fun deleteSet(set: WorkoutSet) = error("read-only")
}

class FakeExerciseRepository(private val exercises: List<Exercise> = EXERCISES) : ExerciseRepository {
    override fun getAllExercises(): Flow<List<Exercise>> = flowOf(exercises)
    override fun getExerciseById(id: Long): Flow<Exercise?> = flowOf(exercises.firstOrNull { it.id == id })
    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup) = flowOf(exercises.filter { it.muscleGroup == muscleGroup })
    override fun getExercisesByType(type: ExerciseType) = flowOf(exercises.filter { it.exerciseType == type })
    override fun searchExercises(query: String): Flow<List<Exercise>> = flowOf(emptyList())
    override suspend fun insertExercise(exercise: Exercise): Long = error("read-only")
    override suspend fun updateExercise(exercise: Exercise) = error("read-only")
    override suspend fun deleteExercise(exercise: Exercise) = error("read-only")
}

class FakeBodyWeightRepository(private val entries: List<BodyWeightEntry>) : BodyWeightRepository {
    override fun getAllEntries(): Flow<List<BodyWeightEntry>> = flowOf(entries)
    override fun getEntriesByDateRange(startTime: Long, endTime: Long) = flowOf(entries.filter { it.loggedAt in startTime..endTime })
    override fun getLatestEntry(): Flow<BodyWeightEntry?> = flowOf(entries.maxByOrNull { it.loggedAt })
    override suspend fun logEntry(weightKg: Float, loggedAt: Long): Long = error("read-only")
    override suspend fun deleteEntry(entry: BodyWeightEntry) = error("read-only")
}

class FakeUserPreferencesRepository(goalKg: Float?) : UserPreferencesRepository {
    override val goalWeightKg: Flow<Float?> = flowOf(goalKg)
    override val displayName: Flow<String> = flowOf("")
    override val bodyDiagramGender: Flow<String> = flowOf("male")
    override val effortTrackingEnabled: Flow<Boolean> = flowOf(false)
    override val preferredEffortScale: Flow<String> = flowOf("RIR")
    override val preferredWeightUnit: Flow<WeightUnit> = flowOf(WeightUnit.KG)
    override suspend fun setDisplayName(name: String) = error("read-only")
    override suspend fun setBodyDiagramGender(gender: String) = error("read-only")
    override suspend fun setGoalWeightKg(kg: Float?) = error("read-only")
    override suspend fun setEffortTrackingEnabled(enabled: Boolean) = error("read-only")
    override suspend fun setPreferredEffortScale(scale: String) = error("read-only")
    override suspend fun setPreferredWeightUnit(unit: WeightUnit) = error("read-only")
}
