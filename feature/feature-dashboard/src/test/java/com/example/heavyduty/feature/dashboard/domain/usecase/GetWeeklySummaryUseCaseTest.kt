package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeeklySummaryUseCaseTest {

    private class FakeWorkoutRepository(private val sessions: List<WorkoutSession>, private val sets: List<WorkoutSet>) :
        WorkoutRepository {
        var requestedSessionRange: LongRange? = null
        var requestedSetRange: LongRange? = null

        override fun getAllSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override fun getSessionById(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> {
            requestedSessionRange = startTime..endTime
            return flowOf(sessions)
        }
        override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> = flowOf(emptyList())
        override fun getAllCompletedSets(): Flow<List<WorkoutSet>> = flowOf(sets)
        override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>> {
            requestedSetRange = startTime..endTime
            return flowOf(sets)
        }
        override fun getActiveSession(): Flow<WorkoutSession?> = flowOf(null)
        override suspend fun insertSession(session: WorkoutSession): Long = 0L
        override suspend fun updateSession(session: WorkoutSession) {}
        override suspend fun deleteSession(session: WorkoutSession) {}
        override suspend fun insertSet(set: WorkoutSet): Long = 0L
        override suspend fun updateSet(set: WorkoutSet) {}
        override suspend fun deleteSet(set: WorkoutSet) {}
    }

    @Test
    fun `queries sessions and sets over the whole given range`() = runBlocking {
        val repo = FakeWorkoutRepository(emptyList(), emptyList())
        val range = 1_000L..9_999L

        GetWeeklySummaryUseCase(repo)(range).first()

        assertEquals(range, repo.requestedSessionRange)
        assertEquals(range, repo.requestedSetRange)
    }

    @Test
    fun `sets of an unfinished workout do not count`() = runBlocking {
        val repo = FakeWorkoutRepository(
            sessions = listOf(
                WorkoutSession(id = 1, totalDurationSecs = 1800, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 2, status = WorkoutStatus.IN_PROGRESS)
            ),
            sets = listOf(
                WorkoutSet(sessionId = 1, reps = 5, weight = 100f, isCompleted = true),
                WorkoutSet(sessionId = 2, reps = 5, weight = 100f, isCompleted = true)
            )
        )

        val totals = GetWeeklySummaryUseCase(repo)(0L..Long.MAX_VALUE).first()

        assertEquals(1, totals.workouts)
        assertEquals(1800, totals.durationSecs)
        assertEquals(500f, totals.volumeKg, 0.001f)
        assertEquals(1, totals.sets)
    }

    @Test
    fun `recent workout is the last completed one, ignoring the one in progress`() = runBlocking {
        val repo = FakeWorkoutRepository(
            sessions = listOf(
                WorkoutSession(id = 1, endTime = 1_000L, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 2, endTime = 5_000L, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 3, startTime = 9_000L, status = WorkoutStatus.IN_PROGRESS)
            ),
            sets = emptyList()
        )

        assertEquals(2L, GetRecentWorkoutUseCase(repo)().first()?.id)
    }

    private class FakeExerciseRepository(private val exercises: List<Exercise>) : ExerciseRepository {
        override fun getAllExercises(): Flow<List<Exercise>> = flowOf(exercises)
        override fun getExerciseById(id: Long): Flow<Exercise?> = flowOf(exercises.find { it.id == id })
        override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> = flowOf(emptyList())
        override fun getExercisesByType(type: ExerciseType): Flow<List<Exercise>> = flowOf(emptyList())
        override fun searchExercises(query: String): Flow<List<Exercise>> = flowOf(emptyList())
        override suspend fun insertExercise(exercise: Exercise): Long = 0L
        override suspend fun updateExercise(exercise: Exercise) {}
        override suspend fun deleteExercise(exercise: Exercise) {}
    }

    @Test
    fun `radar only counts sets of finished workouts, like the weekly volume`() = runBlocking {
        val bench = Exercise(id = 10, name = "Bench", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)
        val repo = FakeWorkoutRepository(
            sessions = listOf(
                WorkoutSession(id = 1, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 2, status = WorkoutStatus.IN_PROGRESS)
            ),
            sets = listOf(
                WorkoutSet(sessionId = 1, exerciseId = 10, reps = 5, weight = 100f, isCompleted = true),
                WorkoutSet(sessionId = 2, exerciseId = 10, reps = 5, weight = 100f, isCompleted = true)
            )
        )

        val radar = GetWeeklyMuscleVolumeUseCase(repo, FakeExerciseRepository(listOf(bench)))(0L, Long.MAX_VALUE).first()
        val totals = GetWeeklySummaryUseCase(repo)(0L..Long.MAX_VALUE).first()

        assertEquals(mapOf(MuscleGroup.CHEST to 500f), radar)
        assertEquals(totals.volumeKg, radar.values.sum(), 0.001f)
    }
}
