package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.core.testing.FakeClock
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTrainingActivityHeatmapUseCaseTest {

    private class FakeWorkoutRepository(private val sessions: List<WorkoutSession>) : WorkoutRepository {
        override fun getAllSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override fun getSessionById(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> = flowOf(emptyList())
        override fun getAllCompletedSets(): Flow<List<WorkoutSet>> = flowOf(emptyList())
        override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>> = flowOf(emptyList())
        override fun getActiveSession(): Flow<WorkoutSession?> = flowOf(null)
        override suspend fun insertSession(session: WorkoutSession): Long = 0L
        override suspend fun updateSession(session: WorkoutSession) {}
        override suspend fun deleteSession(session: WorkoutSession) {}
        override suspend fun insertSet(set: WorkoutSet): Long = 0L
        override suspend fun updateSet(set: WorkoutSet) {}
        override suspend fun deleteSet(set: WorkoutSet) {}
    }

    @Test
    fun `empty sessions returns default report`() = runBlocking {
        val repo = FakeWorkoutRepository(emptyList())
        val useCase = GetTrainingActivityHeatmapUseCase(repo, FakeClock(System.currentTimeMillis()))

        val report = useCase().first()

        assertEquals(0, report.totalDaysTrained)
        assertEquals(0, report.totalSessions)
        assertEquals(0, report.currentStreakDays)
        assertEquals(0, report.longestStreakDays)
        assertEquals(7, report.dayOfWeekDistribution.size)
        assertEquals(4, report.timeSlotDistribution.size)
    }

    @Test
    fun `sessions calculate streaks, day of week and time slot distributions`() = runBlocking {
        // Pinned to Monday 2026-09-28 so "yesterday" (Sunday) falls in the previous
        // Monday-first week — with the real clock the weekly streak depended on the weekday.
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 28, 12, 0)
        }
        val now = cal.timeInMillis

        // Today at 10:00 AM (Mañana)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        val today10am = cal.timeInMillis

        // Yesterday at 10:00 AM (Mañana)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday10am = cal.timeInMillis

        val sessions = listOf(
            WorkoutSession(
                id = 1,
                startTime = today10am,
                totalDurationSecs = 3600, // 60 mins
                status = WorkoutStatus.COMPLETED
            ),
            WorkoutSession(
                id = 2,
                startTime = yesterday10am,
                totalDurationSecs = 1800, // 30 mins
                status = WorkoutStatus.COMPLETED
            ),
            // In progress session should be excluded
            WorkoutSession(
                id = 3,
                startTime = now,
                totalDurationSecs = 600,
                status = WorkoutStatus.IN_PROGRESS
            )
        )

        val repo = FakeWorkoutRepository(sessions)
        val useCase = GetTrainingActivityHeatmapUseCase(repo, FakeClock(now))

        val report = useCase().first()

        assertEquals(2, report.totalSessions)
        assertEquals(2, report.totalDaysTrained)
        assertEquals(2, report.currentStreakDays)
        assertEquals(2, report.longestStreakDays)
        assertEquals(2, report.currentStreakWeeks)
        assertEquals(2, report.longestStreakWeeks)
        assertEquals(1.5f, report.totalHoursTrained, 0.05f)
        assertEquals(45, report.avgMinutesPerSession)

        // Time slots: both sessions at 10am -> morning
        val mananaSlot = report.timeSlotDistribution.find { it.slot == TimeSlot.MORNING }
        assertNotNull(mananaSlot)
        assertEquals(2, mananaSlot?.count)
        assertEquals(100f, mananaSlot?.percentage ?: 0f, 0.1f)
        assertEquals(TimeSlot.MORNING, report.favoriteTimeSlot)

        // Day of week distribution has 7 items
        assertEquals(7, report.dayOfWeekDistribution.size)
        assertTrue(report.favoriteDayOfWeek != null)
    }

    @Test
    fun `weekly streak continues across new year`() = runBlocking {
        fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
            clear()
            set(year, month, day, 10, 0)
        }.timeInMillis
        // Tue Dec 23 2025 and Tue Dec 30 2025: consecutive weeks, the second one is ISO week 1 of 2026.
        val sessions = listOf(at(2025, Calendar.DECEMBER, 23), at(2025, Calendar.DECEMBER, 30)).mapIndexed { i, start ->
            WorkoutSession(id = i + 1L, startTime = start, totalDurationSecs = 1800, status = WorkoutStatus.COMPLETED)
        }

        val report = GetTrainingActivityHeatmapUseCase(
            FakeWorkoutRepository(sessions),
            FakeClock(at(2025, Calendar.DECEMBER, 31))
        )().first()

        assertEquals(2, report.currentStreakWeeks)
        assertEquals(2, report.longestStreakWeeks)
    }
}
