package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetTrainingActivityHeatmapUseCaseTest {

    private class FakeWorkoutRepository(
        private val sessions: List<WorkoutSession>,
    ) : WorkoutRepository {
        override fun getAllSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override fun getSessionById(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> = flowOf(null)
        override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> = flowOf(emptyList())
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
        val useCase = GetTrainingActivityHeatmapUseCase(repo)

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
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        // Set to today at 10:00 AM (Mañana)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        val today10am = cal.timeInMillis

        // Yesterday at 10:00 AM (Mañana)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday10am = cal.timeInMillis

        val sessions = listOf(
            WorkoutSession(
                id = 1,
                startTime = today10am,
                totalDurationSecs = 3600, // 60 mins
                status = WorkoutStatus.COMPLETED,
            ),
            WorkoutSession(
                id = 2,
                startTime = yesterday10am,
                totalDurationSecs = 1800, // 30 mins
                status = WorkoutStatus.COMPLETED,
            ),
            // In progress session should be excluded
            WorkoutSession(
                id = 3,
                startTime = now,
                totalDurationSecs = 600,
                status = WorkoutStatus.IN_PROGRESS,
            ),
        )

        val repo = FakeWorkoutRepository(sessions)
        val useCase = GetTrainingActivityHeatmapUseCase(repo)

        val report = useCase().first()

        assertEquals(2, report.totalSessions)
        assertEquals(2, report.totalDaysTrained)
        assertEquals(2, report.currentStreakDays)
        assertEquals(2, report.longestStreakDays)
        assertEquals(2, report.currentStreakWeeks)
        assertEquals(2, report.longestStreakWeeks)
        assertEquals(1.5f, report.totalHoursTrained, 0.05f)
        assertEquals(45, report.avgMinutesPerSession)

        // Time slots: both sessions at 10am -> Mañana
        val mananaSlot = report.timeSlotDistribution.find { it.slotName == "Mañana" }
        assertNotNull(mananaSlot)
        assertEquals(2, mananaSlot?.count)
        assertEquals(100f, mananaSlot?.percentage ?: 0f, 0.1f)
        assertEquals("Mañana", report.favoriteTimeSlotName)

        // Day of week distribution has 7 items
        assertEquals(7, report.dayOfWeekDistribution.size)
        assertTrue(report.favoriteDayName != null)
    }
}
