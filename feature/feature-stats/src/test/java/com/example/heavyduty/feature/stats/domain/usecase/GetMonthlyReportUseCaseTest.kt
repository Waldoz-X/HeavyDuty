package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeExerciseRepository
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GetMonthlyReportUseCaseTest {

    private fun at(month: Int, day: Int, hour: Int = 18, minute: Int = 0, second: Int = 0, millis: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, millis)
        }.timeInMillis

    private val september = at(Calendar.SEPTEMBER, 15)
    private val october = at(Calendar.OCTOBER, 15)

    private val sessions = listOf(
        session(1, start = at(Calendar.SEPTEMBER, 2), durationMins = 60),
        session(2, start = at(Calendar.SEPTEMBER, 20), durationMins = 45),
        session(3, start = at(Calendar.SEPTEMBER, 25), status = WorkoutStatus.IN_PROGRESS),
        session(4, start = at(Calendar.AUGUST, 10), durationMins = 50),
    )

    private val sets = listOf(
        set(1, BENCH, reps = 10, weightKg = 40f, at = at(Calendar.SEPTEMBER, 2, minute = 5), warmup = true),
        set(1, BENCH, reps = 8, weightKg = 100f, at = at(Calendar.SEPTEMBER, 2, minute = 10)),
        set(1, BENCH, reps = 8, weightKg = 100f, at = at(Calendar.SEPTEMBER, 2, minute = 15)),
        set(2, SQUAT, reps = 5, weightKg = 120f, at = at(Calendar.SEPTEMBER, 20, minute = 10)),
        set(3, SQUAT, reps = 5, weightKg = 200f, at = at(Calendar.SEPTEMBER, 25, minute = 10)),
        set(4, BENCH, reps = 10, weightKg = 80f, at = at(Calendar.AUGUST, 10, minute = 10)),
    )

    private fun report(reference: Long, sessions: List<com.example.heavyduty.core.domain.model.WorkoutSession> = this.sessions,
                       sets: List<com.example.heavyduty.core.domain.model.WorkoutSet> = this.sets) = runBlocking {
        GetMonthlyReportUseCase(FakeWorkoutRepository(sessions, sets), FakeExerciseRepository(EXERCISES), FakeClock(reference))().first()
    }

    @Test
    fun `month totals count finished workouts and working sets only`() {
        val report = report(september)

        assertEquals(at(Calendar.SEPTEMBER, 1, hour = 0), report.monthStartMillis)
        assertEquals(2, report.totalWorkouts)
        assertEquals(3, report.totalSets)
        assertEquals(2200f, report.totalVolumeKg, 0.01f) // 1600 bench + 600 squat
        assertEquals(105, report.totalDurationMinutes)
        assertEquals(52, report.avgSessionDurationMinutes) // 105 / 2, rounded down
    }

    @Test
    fun `comparison with the previous month`() {
        val report = report(september)

        assertEquals(1, report.prevMonthWorkoutsDelta) // 2 vs 1
        assertEquals(800f, report.prevMonthVolumeKg!!, 0.01f)
        assertEquals(175f, report.prevMonthVolumeDeltaPercent!!, 0.01f) // (2200 − 800) / 800
    }

    @Test
    fun `muscle breakdown splits compound volume and adds up to 100 percent`() {
        val report = report(september)

        assertEquals(MuscleGroup.CHEST, report.topMuscleGroup)
        val expected = listOf(MuscleGroup.CHEST to 960f, MuscleGroup.QUADRICEPS to 600f, MuscleGroup.TRICEPS to 400f, MuscleGroup.SHOULDERS to 240f)
        assertEquals(expected.map { it.first }, report.muscleContributions.map { it.muscleGroup })
        expected.zip(report.muscleContributions).forEach { (want, got) -> assertEquals(want.second, got.volumeKg, 0.01f) }
        assertEquals(100f, report.muscleContributions.sumOf { it.percentage.toDouble() }.toFloat(), 0.01f)
    }

    @Test
    fun `top exercises by volume`() {
        val top = report(september).topExercises

        assertEquals(listOf(BENCH.id, SQUAT.id), top.map { it.exerciseId })
        assertEquals(2, top[0].totalSets)
        assertEquals(1600f, top[0].totalVolumeKg, 0.01f)
        assertEquals(100f, top[0].maxWeightKg)
    }

    @Test
    fun `weekly breakdown covers every week of the month and adds up to the totals`() {
        val report = report(september)
        val weeks = Calendar.getInstance().apply { timeInMillis = september }.getActualMaximum(Calendar.WEEK_OF_MONTH)

        assertEquals((1..weeks).toList(), report.workoutsPerWeek.map { it.first })
        assertEquals((1..weeks).toList(), report.volumePerWeek.map { it.first })
        assertEquals(2, report.workoutsPerWeek.sumOf { it.second })
        assertEquals(2200f, report.volumePerWeek.sumOf { it.second.toDouble() }.toFloat(), 0.01f)

        val weekOfFirstWorkout = Calendar.getInstance().apply { timeInMillis = at(Calendar.SEPTEMBER, 2) }.get(Calendar.WEEK_OF_MONTH)
        assertEquals(1600f, report.volumePerWeek.first { it.first == weekOfFirstWorkout }.second, 0.01f)
    }

    @Test
    fun `a workout that runs past midnight at month end counts whole in the month it started`() {
        val lateSessions = listOf(session(1, start = at(Calendar.SEPTEMBER, 30, hour = 23, minute = 40)))
        val lateSets = listOf(
            set(1, SQUAT, reps = 5, weightKg = 100f, at = at(Calendar.SEPTEMBER, 30, hour = 23, minute = 55)),
            set(1, SQUAT, reps = 5, weightKg = 100f, at = at(Calendar.OCTOBER, 1, hour = 0, minute = 10)),
        )

        val sept = report(september, lateSessions, lateSets)
        assertEquals(1, sept.totalWorkouts)
        assertEquals(2, sept.totalSets)
        assertEquals(1000f, sept.totalVolumeKg, 0.01f)

        val oct = report(october, lateSessions, lateSets)
        assertEquals(0, oct.totalWorkouts)
        assertEquals(0, oct.totalSets)
    }

    @Test
    fun `a workout started in the month's last second still belongs to it`() {
        val start = at(Calendar.SEPTEMBER, 30, hour = 23, minute = 59, second = 59, millis = 500)
        val report = report(
            september,
            listOf(session(1, start = start)),
            listOf(set(1, SQUAT, reps = 5, weightKg = 100f, at = start + 60_000)),
        )

        assertEquals(1, report.totalWorkouts)
        assertEquals(500f, report.totalVolumeKg, 0.01f)
    }

    @Test
    fun `empty month has no comparison and no top muscle`() {
        val report = report(september, emptyList(), emptyList())

        assertEquals(0, report.totalWorkouts)
        assertNull(report.prevMonthWorkoutsDelta)
        assertNull(report.prevMonthVolumeDeltaPercent)
        assertNull(report.topMuscleGroup)
        assertEquals(0, report.avgSessionDurationMinutes)
    }
}
