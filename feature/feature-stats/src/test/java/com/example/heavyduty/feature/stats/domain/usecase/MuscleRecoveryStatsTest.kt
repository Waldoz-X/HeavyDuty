package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeExerciseRepository
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.pow

/** Fatigue and strength retention per muscle; both are measured against the real clock. */
class MuscleRecoveryStatsTest {

    private val now = System.currentTimeMillis()
    private val exercises = FakeExerciseRepository(EXERCISES)

    /** One workout per session id, started when its first set was done. */
    private fun sessionsOf(sets: List<com.example.heavyduty.core.domain.model.WorkoutSet>, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
        sets.groupBy { it.sessionId }.map { (id, its) -> session(id, start = its.minOf { it.completedAt!! } - 60_000, status = status) }

    private fun fatigue(vararg sets: com.example.heavyduty.core.domain.model.WorkoutSet, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
        runBlocking {
            val sessions = sessionsOf(sets.toList(), status)
            GetMuscleGroupFatigueUseCase(FakeWorkoutRepository(sessions, sets.toList()), exercises, FakeClock(now))().first()
        }

    private fun retention(vararg sets: com.example.heavyduty.core.domain.model.WorkoutSet) = runBlocking {
        val sessions = sessionsOf(sets.toList())
        GetMuscleGroupStrengthRetentionUseCase(FakeWorkoutRepository(sessions, sets.toList()), exercises, FakeClock(now))().first()
    }

    @Test
    fun `one hard bench set an hour ago fatigues chest most, then triceps, then shoulders`() {
        val levels = fatigue(set(1, BENCH, reps = 8, weightKg = 100f, at = now - HOUR))

        // Worked by hand: 1RM = 100 × (1 + 8/30); 800 kg × (100/1RM)^1.5 / 400, 36 h half-life.
        val stimulus = 800.0 * (100.0 / (100.0 * (1 + 8 / 30.0))).pow(1.5) / 400.0
        val decay = 0.5.pow(1.0 / 36.0)
        fun expected(share: Double) = (1 - exp(-stimulus * share * decay)).toFloat()

        assertEquals(expected(0.6), levels.getValue(MuscleGroup.CHEST), 0.002f)
        assertEquals(expected(0.25), levels.getValue(MuscleGroup.TRICEPS), 0.002f)
        assertEquals(expected(0.15), levels.getValue(MuscleGroup.SHOULDERS), 0.002f)
        assertEquals(0f, levels.getValue(MuscleGroup.CALVES))
        assertEquals(MuscleGroup.entries.size, levels.size)
        assertTrue(levels.values.all { it in 0f..1f })
    }

    @Test
    fun `fatigue fades with time`() {
        val recent = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR)).getValue(MuscleGroup.QUADRICEPS)
        val twoDays = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - 2 * DAY)).getValue(MuscleGroup.QUADRICEPS)
        val month = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - 30 * DAY)).getValue(MuscleGroup.QUADRICEPS)

        assertTrue("$recent > $twoDays", recent > twoDays)
        assertTrue("$twoDays > $month", twoDays > month)
        assertTrue("a month later it's practically gone: $month", month < 0.01f)
        // The 14-day window only drops what had already faded: a 13-day-old set still counts a little.
        val almostTwoWeeks = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - 13 * DAY)).getValue(MuscleGroup.QUADRICEPS)
        assertTrue("$almostTwoWeeks", almostTwoWeeks > 0f && almostTwoWeeks < 0.01f)
    }

    @Test
    fun `more sets mean more fatigue`() {
        val one = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR)).getValue(MuscleGroup.QUADRICEPS)
        val three = fatigue(
            set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR - 2),
            set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR - 1),
            set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR),
        ).getValue(MuscleGroup.QUADRICEPS)

        assertTrue("$three > $one", three > one)
    }

    @Test
    fun `warm-ups and unfinished workouts cause no fatigue`() {
        val warmup = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR, warmup = true))
        val unfinished = fatigue(set(1, SQUAT, reps = 5, weightKg = 120f, at = now - HOUR), status = WorkoutStatus.IN_PROGRESS)

        assertTrue(warmup.values.all { it == 0f })
        assertTrue(unfinished.values.all { it == 0f })
    }

    @Test
    fun `strength is fully kept for two weeks after a hard set, then fades toward half`() {
        val levels = retention(
            set(1, SQUAT, reps = 5, weightKg = 120f, at = now - 3 * DAY),
            // 42 days = 14 full + one 28-day half-life: 0.5 + 0.5 × 0.5
            set(2, BENCH, reps = 5, weightKg = 100f, at = now - 42 * DAY),
        )

        assertEquals(1f, levels.getValue(MuscleGroup.QUADRICEPS), 0.001f)
        assertEquals(0.75f, levels.getValue(MuscleGroup.CHEST), 0.001f)
        // A bench set is hard for triceps and shoulders too.
        assertEquals(0.75f, levels.getValue(MuscleGroup.TRICEPS), 0.001f)
        assertEquals(0.75f, levels.getValue(MuscleGroup.SHOULDERS), 0.001f)
        // Never trained hard: the floor.
        assertEquals(0.5f, levels.getValue(MuscleGroup.CALVES), 0.001f)
    }

    @Test
    fun `light sets don't refresh strength retention`() {
        val levels = retention(
            set(1, CURL, reps = 10, weightKg = 20f, at = now - 42 * DAY), // the hard set: 1RM 26.7 kg
            set(2, CURL, reps = 10, weightKg = 10f, at = now - DAY), // 37 % of 1RM: not hard
        )

        assertEquals(0.75f, levels.getValue(MuscleGroup.BICEPS), 0.001f)
    }
}
