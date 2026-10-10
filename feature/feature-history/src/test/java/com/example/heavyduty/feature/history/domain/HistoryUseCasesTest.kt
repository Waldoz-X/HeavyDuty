package com.example.heavyduty.feature.history.domain

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import com.example.heavyduty.feature.history.domain.usecase.GetExercisePersonalRecordsUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetExerciseProgressUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetStatsUseCase
import com.example.heavyduty.feature.history.domain.usecase.GetWorkoutHistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryUseCasesTest {

    private val bench = 1L
    private val squat = 2L

    private fun set(id: Long, exercise: Long, reps: Int, weight: Float, at: Long, warmup: Boolean = false) = WorkoutSet(
        id = id,
        sessionId = 1,
        exerciseId = exercise,
        reps = reps,
        weight = weight,
        isCompleted = true,
        completedAt = at,
        isWarmup = warmup
    )

    private val sets = listOf(
        set(1, bench, reps = 1, weight = 150f, at = 1, warmup = true), // warm-ups never count
        set(2, bench, reps = 10, weight = 80f, at = 2), // most reps; 1RM 106.7
        set(3, bench, reps = 3, weight = 100f, at = 3), // heaviest; 1RM 110
        set(4, bench, reps = 5, weight = 0f, at = 4), // no weight: not a record
        set(5, squat, reps = 5, weight = 140f, at = 5)
    )
    private val workouts = FakeWorkoutRepository(listOf(WorkoutSession(id = 1, startTime = 0, status = WorkoutStatus.COMPLETED)), sets)

    @Test
    fun `records of one exercise`() = runBlocking {
        val records = GetExercisePersonalRecordsUseCase(workouts)(bench).first()

        assertEquals(100f, records.maxWeight)
        assertEquals(3, records.maxWeightReps)
        assertEquals(3L, records.maxWeightAchievedAtMillis)
        assertEquals(10, records.maxReps)
        assertEquals(80f, records.maxRepsWeight)
        assertEquals(110f, records.estimatedOneRepMax!!, 0.01f)
    }

    @Test
    fun `an exercise never lifted with weight has no records`() = runBlocking {
        val records = GetExercisePersonalRecordsUseCase(workouts)(99).first()

        assertFalse(records.hasRecords)
        assertNull(records.estimatedOneRepMax)
    }

    @Test
    fun `progress chart leaves out warm-ups`() = runBlocking {
        val progress = GetExerciseProgressUseCase(workouts)(bench).first()

        assertEquals(listOf(4L, 3L, 2L), progress.map { it.id })
    }

    @Test
    fun `history is newest first`() = runBlocking {
        val repo = FakeWorkoutRepository(
            listOf(WorkoutSession(id = 1, startTime = 10), WorkoutSession(id = 2, startTime = 30), WorkoutSession(id = 3, startTime = 20))
        )

        assertEquals(listOf(2L, 3L, 1L), GetWorkoutHistoryUseCase(repo)().first().map { it.id })
    }

    @Test
    fun `history totals and average duration`() = runBlocking {
        val repo = FakeWorkoutRepository(
            listOf(
                WorkoutSession(id = 1, startTime = 1, totalDurationSecs = 3600, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 2, startTime = 2, totalDurationSecs = 1800, status = WorkoutStatus.COMPLETED),
                WorkoutSession(id = 3, startTime = 3, totalDurationSecs = 600, status = WorkoutStatus.CANCELLED),
                WorkoutSession(id = 4, startTime = 4, status = WorkoutStatus.IN_PROGRESS)
            )
        )

        val stats = GetStatsUseCase(repo)().first()

        assertEquals(3, stats.totalSessions)
        assertEquals(2, stats.completedSessions)
        assertEquals(1, stats.cancelledSessions)
        assertEquals(90, stats.totalDurationMinutes)
        assertEquals(45, stats.averageDurationMinutes)
    }
}
