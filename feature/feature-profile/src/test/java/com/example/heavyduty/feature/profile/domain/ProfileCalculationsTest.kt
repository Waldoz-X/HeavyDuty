package com.example.heavyduty.feature.profile.domain

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import com.example.heavyduty.feature.profile.domain.model.personalRecordSetIds
import com.example.heavyduty.feature.profile.domain.model.weeklyTrainingHours
import com.example.heavyduty.feature.profile.domain.usecase.GetProfileStatsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileCalculationsTest {

    private val day = 24 * 3_600_000L
    private val week = 7 * day

    private fun set(id: Long, exercise: Long, weight: Float, at: Long, session: Long = 1, warmup: Boolean = false, reps: Int = 5) =
        WorkoutSet(id = id, sessionId = session, exerciseId = exercise, reps = reps, weight = weight, isCompleted = true, completedAt = at, isWarmup = warmup)

    @Test
    fun `a set is a record when it beats every earlier weight of its exercise`() {
        val sets = listOf(
            set(4, exercise = 1, weight = 100f, at = 4), // listed out of order on purpose
            set(1, exercise = 1, weight = 80f, at = 1), // first ever bench: a record
            set(2, exercise = 1, weight = 80f, at = 2), // only ties it
            set(3, exercise = 1, weight = 70f, at = 3),
            set(5, exercise = 2, weight = 60f, at = 5), // first squat
        )

        assertEquals(setOf(1L, 4L, 5L), personalRecordSetIds(sets))
    }

    @Test
    fun `weekly hours are oldest first and only count finished workouts`() {
        val now = 100 * day
        val sessions = listOf(
            WorkoutSession(id = 1, startTime = now - 1 * day, totalDurationSecs = 3600, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 2, startTime = now - 2 * day, totalDurationSecs = 1800, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 3, startTime = now - 10 * day, totalDurationSecs = 7200, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 4, startTime = now - 3 * day, totalDurationSecs = 9999, status = WorkoutStatus.IN_PROGRESS),
            // Exactly on a week boundary: counted once, in the newer week.
            WorkoutSession(id = 5, startTime = now - week, totalDurationSecs = 900, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 6, startTime = now - 40 * day, totalDurationSecs = 3600, status = WorkoutStatus.COMPLETED),
        )

        val weeks = weeklyTrainingHours(sessions, now)

        assertEquals(listOf(now - 4 * week, now - 3 * week, now - 2 * week, now - week), weeks.map { it.first })
        assertEquals(listOf(0f, 0f, 2f, 1.75f), weeks.map { it.second })
    }

    @Test
    fun `profile totals`() = runBlocking {
        val sessions = listOf(
            WorkoutSession(id = 1, startTime = 5 * day, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 2, startTime = 2 * day, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 3, startTime = 9 * day, status = WorkoutStatus.IN_PROGRESS),
        )
        val sets = listOf(
            set(1, exercise = 1, weight = 100f, at = 5 * day, session = 1, reps = 8), // 800
            set(2, exercise = 1, weight = 40f, at = 5 * day, session = 1, warmup = true),
            set(3, exercise = 2, weight = 50f, at = 2 * day, session = 2, reps = 10), // 500
            set(4, exercise = 2, weight = 90f, at = 9 * day, session = 3), // workout not finished
        )

        val stats = GetProfileStatsUseCase(FakeWorkoutRepository(sessions, sets))().first()

        assertEquals(2, stats.totalWorkouts)
        assertEquals(2, stats.totalSets)
        assertEquals(1300f, stats.totalVolumeKg, 0.01f)
        assertEquals(2 * day, stats.memberSinceMillis)
    }

    @Test
    fun `a new user has empty totals`() = runBlocking {
        val stats = GetProfileStatsUseCase(FakeWorkoutRepository())().first()

        assertEquals(0, stats.totalWorkouts)
        assertEquals(0f, stats.totalVolumeKg)
        assertNull(stats.memberSinceMillis)
    }
}
