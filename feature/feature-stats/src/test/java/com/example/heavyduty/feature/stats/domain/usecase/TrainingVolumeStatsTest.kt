package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Period summary, sets/volume per muscle, most-used exercises and PRs, checked against a small
 * history whose totals are worked out by hand in the comments.
 */
class TrainingVolumeStatsTest {

    private val deleted = Exercise(id = DELETED_EXERCISE_ID, name = "Borrado", muscleGroup = MuscleGroup.CALVES, exerciseType = ExerciseType.STRENGTH)

    // Current window: days 10–19. Previous window: days 0–9.
    private val currentRange = 10 * DAY until 20 * DAY
    private val previousRange = 0L until 10 * DAY

    private val sessions = listOf(
        session(1, start = 12 * DAY, durationMins = 60),
        session(2, start = 15 * DAY, durationMins = 45),
        session(3, start = 16 * DAY, status = WorkoutStatus.IN_PROGRESS),
        session(4, start = 17 * DAY, status = WorkoutStatus.CANCELLED),
        session(5, start = 3 * DAY, durationMins = 30),
    )

    private val sets = listOf(
        // Workout 1: bench 8×100 + 8×100 = 1600 kg, plus a warm-up and an unfinished set.
        set(1, BENCH, reps = 10, weightKg = 40f, at = 12 * DAY + 1, warmup = true),
        set(1, BENCH, reps = 8, weightKg = 100f, at = 12 * DAY + 2),
        set(1, BENCH, reps = 8, weightKg = 100f, at = 12 * DAY + 3),
        set(1, BENCH, reps = 8, weightKg = 100f, at = 12 * DAY + 4, completed = false),
        // Workout 2: squat 5×120 = 600 kg, a 10×100 lb curl (stored as 45.36 kg) = 453.6 kg,
        // and a set of an exercise that was deleted since.
        set(2, SQUAT, reps = 5, weightKg = 120f, at = 15 * DAY + 1),
        set(2, CURL, reps = 10, weightKg = 45.36f, at = 15 * DAY + 2, unit = WeightUnit.LB),
        set(2, deleted, reps = 10, weightKg = 50f, at = 15 * DAY + 3),
        // Unfinished and cancelled workouts never count.
        set(3, SQUAT, reps = 5, weightKg = 200f, at = 16 * DAY + 1),
        set(4, SQUAT, reps = 5, weightKg = 200f, at = 17 * DAY + 1),
        // Previous window: curl 10×20 = 200 kg.
        set(5, CURL, reps = 10, weightKg = 20f, at = 3 * DAY + 1),
    )

    private val workouts = FakeWorkoutRepository(sessions, sets)
    private val exercises = FakeExerciseRepository()

    @Test
    fun `period summary counts only finished workouts and their working sets`() = runBlocking {
        val summary = GetPeriodSummaryUseCase(workouts)(currentRange, previousRange).first()

        assertEquals(2, summary.workouts)
        assertEquals(105, summary.durationMinutes)
        // 1600 + 600 + 453.6 + 500 (the deleted exercise's set still happened)
        assertEquals(3153.6f, summary.volumeKg, 0.01f)
        assertEquals(5, summary.sets)

        assertEquals(1, summary.previousWorkouts)
        assertEquals(30, summary.previousDurationMinutes)
        assertEquals(200f, summary.previousVolumeKg, 0.01f)
        assertEquals(1, summary.previousSets)

        assertEquals(1, summary.workoutsDelta)
        assertEquals(75, summary.durationMinutesDelta)
        assertEquals(2953.6f, summary.volumeKgDelta, 0.01f)
        assertEquals(4, summary.setsDelta)
    }

    @Test
    fun `period summary of an empty history is all zeros`() = runBlocking {
        val summary = GetPeriodSummaryUseCase(FakeWorkoutRepository(emptyList(), emptyList()))(currentRange, previousRange).first()

        assertEquals(0, summary.workouts)
        assertEquals(0f, summary.volumeKg)
        assertEquals(0, summary.workoutsDelta)
    }

    @Test
    fun `sets per muscle split a compound set across the muscles it trains`() = runBlocking {
        val counts = GetMuscleGroupSetCountUseCase(workouts, exercises)(currentRange.first, currentRange.last).first()

        // Two bench sets: 0.6 + 0.25 + 0.15 of a set each.
        assertEquals(1.2f, counts.getValue(MuscleGroup.CHEST), 0.001f)
        assertEquals(0.5f, counts.getValue(MuscleGroup.TRICEPS), 0.001f)
        assertEquals(0.3f, counts.getValue(MuscleGroup.SHOULDERS), 0.001f)
        assertEquals(1f, counts.getValue(MuscleGroup.QUADRICEPS), 0.001f)
        assertEquals(1f, counts.getValue(MuscleGroup.BICEPS), 0.001f)
        // Credits add up to the 4 counted sets of known exercises.
        assertEquals(4f, counts.values.sum(), 0.001f)
        assertFalse(MuscleGroup.CALVES in counts)
    }

    @Test
    fun `volume per muscle is reps times kilograms, split like the sets`() = runBlocking {
        val volume = GetMuscleGroupVolumeUseCase(workouts, exercises)(currentRange.first, currentRange.last).first()

        assertEquals(960f, volume.getValue(MuscleGroup.CHEST), 0.01f)
        assertEquals(400f, volume.getValue(MuscleGroup.TRICEPS), 0.01f)
        assertEquals(240f, volume.getValue(MuscleGroup.SHOULDERS), 0.01f)
        assertEquals(600f, volume.getValue(MuscleGroup.QUADRICEPS), 0.01f)
        // Logged in pounds, counted in kilograms.
        assertEquals(453.6f, volume.getValue(MuscleGroup.BICEPS), 0.01f)
    }

    @Test
    fun `volume of the previous window only has that window's workouts`() = runBlocking {
        val volume = GetMuscleGroupVolumeUseCase(workouts, exercises)(previousRange.first, previousRange.last).first()

        assertEquals(mapOf(MuscleGroup.BICEPS to 200f), volume)
    }

    @Test
    fun `most used exercises are ranked by working sets`() = runBlocking {
        val top = GetTopExercisesByFrequencyUseCase(workouts, exercises)().first()

        assertEquals(listOf("Press de banca", "Curl de bíceps", "Sentadilla"), top.map { it.exerciseName })
        assertEquals(listOf(2, 2, 1), top.map { it.setCount })
    }

    @Test
    fun `personal records keep each exercise's best estimated one-rep max`() = runBlocking {
        val history = listOf(session(1, start = DAY), session(2, start = 2 * DAY))
        val prSets = listOf(
            set(1, BENCH, reps = 1, weightKg = 140f, at = DAY + 1, warmup = true), // warm-ups never count
            set(1, BENCH, reps = 8, weightKg = 100f, at = DAY + 2), // 100 × (1 + 8/30) = 126.67
            set(2, BENCH, reps = 3, weightKg = 110f, at = 2 * DAY + 1), // 121
            set(2, BENCH, reps = 1, weightKg = 120f, at = 2 * DAY + 2), // a real single: 120
            set(1, SQUAT, reps = 15, weightKg = 60f, at = DAY + 3), // over 12 reps: no estimate
            set(2, SQUAT, reps = 5, weightKg = 100f, at = 2 * DAY + 3), // 116.67
            set(1, CURL, reps = 15, weightKg = 20f, at = DAY + 4), // only high-rep sets: no PR
        )

        val records = GetExercisePersonalRecordsListUseCase(FakeWorkoutRepository(history, prSets), exercises)().first()

        assertEquals(listOf(BENCH.id, SQUAT.id), records.map { it.exerciseId })
        val bench = records[0]
        assertEquals(126.67f, bench.estimatedOneRepMaxKg, 0.01f)
        assertEquals(100f, bench.sourceWeightKg)
        assertEquals(8, bench.sourceReps)
        assertEquals(DAY + 2, bench.achievedAtMillis)
        assertEquals(116.67f, records[1].estimatedOneRepMaxKg, 0.01f)
    }
}
