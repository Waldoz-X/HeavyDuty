package com.example.heavyduty.feature.history

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeTrainingHistoryRepository
import com.example.heavyduty.feature.history.domain.model.ExerciseSets
import com.example.heavyduty.feature.history.domain.model.shareExerciseLines
import com.example.heavyduty.feature.history.domain.usecase.BuildWorkoutShareSummaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutShareSummaryTest {

    private fun set(id: Long, kg: Float?, reps: Int?, warmup: Boolean = false) =
        WorkoutSet(id = id, weight = kg, reps = reps, isCompleted = true, isWarmup = warmup)

    @Test
    fun `each exercise shows its working sets and its heaviest set`() {
        val lines = shareExerciseLines(
            listOf(
                ExerciseSets(1, "Press de banca", listOf(set(1, 40f, 10, warmup = true), set(2, 80f, 8), set(3, 85f, 5), set(4, 80f, 6))),
                ExerciseSets(2, "Remo", listOf(set(5, 60f, 10), set(6, 60f, 12)))
            )
        )

        assertEquals(listOf("Press de banca", "Remo"), lines.map { it.name })
        assertEquals(3, lines[0].workingSets)
        assertEquals(3L, lines[0].bestSet?.id)
        // Same weight: more reps wins.
        assertEquals(6L, lines[1].bestSet?.id)
    }

    @Test
    fun `an exercise with only warm-ups is left out, and one without numbers has no best set`() {
        val lines = shareExerciseLines(
            listOf(
                ExerciseSets(1, "Movilidad", listOf(set(1, null, 10, warmup = true))),
                ExerciseSets(2, "Plancha", listOf(set(2, null, null)))
            )
        )

        assertEquals(listOf("Plancha"), lines.map { it.name })
        assertNull(lines.single().bestSet)
    }

    // --- The whole summary, with records measured against earlier workouts ---

    private val bench = Exercise(
        id = 1,
        name = "Press de banca",
        muscleGroup = MuscleGroup.CHEST,
        exerciseType = ExerciseType.STRENGTH,
        muscleWeights = mapOf(MuscleGroup.CHEST to 0.7f, MuscleGroup.TRICEPS to 0.3f)
    )
    private val row = Exercise(id = 2, name = "Remo", muscleGroup = MuscleGroup.BACK, exerciseType = ExerciseType.STRENGTH)

    private fun logged(id: Long, session: Long, exercise: Exercise, kg: Float, reps: Int, warmup: Boolean = false) = WorkoutSet(
        id = id, sessionId = session, exerciseId = exercise.id, exercise = exercise, setNumber = id.toInt(),
        weight = kg, reps = reps, isCompleted = true, completedAt = id, isWarmup = warmup
    )

    private fun session(id: Long, start: Long, sets: List<WorkoutSet>, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
        WorkoutSession(id = id, startTime = start, totalDurationSecs = 3_000, status = status, sets = sets)

    private fun summaryOf(target: WorkoutSession, vararg others: WorkoutSession) = runBlocking {
        val all = listOf(target) + others
        BuildWorkoutShareSummaryUseCase(FakeTrainingHistoryRepository(all, all.flatMap { it.sets }))(target)
    }

    @Test
    fun `a set heavier than every earlier workout is a record, one below it is not`() {
        val before = session(1, start = 100, sets = listOf(logged(1, 1, bench, 80f, 8), logged(2, 1, row, 70f, 10)))
        val today = session(2, start = 200, sets = listOf(logged(3, 2, bench, 85f, 5), logged(4, 2, row, 65f, 12)))

        val records = summaryOf(today, before).records

        assertEquals(listOf("Press de banca"), records.map { it.exerciseName })
        assertEquals(85f, records.single().set.weight)
    }

    @Test
    fun `as heavy as the best with more reps is a record, as heavy with as many is not`() {
        val before = session(1, start = 100, sets = listOf(logged(1, 1, bench, 80f, 8), logged(2, 1, row, 70f, 10)))
        val today = session(2, start = 200, sets = listOf(logged(3, 2, bench, 80f, 9), logged(4, 2, row, 70f, 10)))

        assertEquals(listOf("Press de banca"), summaryOf(today, before).records.map { it.exerciseName })
    }

    @Test
    fun `a first time, warm-ups, later and unfinished workouts never make or block a record`() {
        val earlierUnfinished = session(1, start = 50, sets = listOf(logged(1, 1, bench, 200f, 1)), status = WorkoutStatus.IN_PROGRESS)
        val before = session(2, start = 100, sets = listOf(logged(2, 2, bench, 80f, 8)))
        val today = session(
            3,
            start = 200,
            sets = listOf(logged(3, 3, bench, 120f, 1, warmup = true), logged(4, 3, bench, 82.5f, 6), logged(5, 3, row, 60f, 10))
        )
        val later = session(4, start = 300, sets = listOf(logged(6, 4, bench, 100f, 5)))

        val records = summaryOf(today, before, later, earlierUnfinished).records

        // Bench beats the 80 kg of the one finished workout before it; the row has no history yet.
        assertEquals(listOf(82.5f), records.map { it.set.weight })
    }

    @Test
    fun `numbers and muscles count working sets only, compound lifts credit every muscle`() {
        val today = session(
            1,
            start = 100,
            sets = listOf(logged(1, 1, bench, 40f, 10, warmup = true), logged(2, 1, bench, 80f, 10), logged(3, 1, row, 50f, 10))
        )

        val summary = summaryOf(today)

        assertEquals(2, summary.workingSets)
        assertEquals(1_300f, summary.volumeKg)
        assertEquals(0.7f, summary.setsByMuscle.getValue(MuscleGroup.CHEST), 0.001f)
        assertEquals(0.3f, summary.setsByMuscle.getValue(MuscleGroup.TRICEPS), 0.001f)
        assertEquals(1f, summary.setsByMuscle.getValue(MuscleGroup.BACK), 0.001f)
        assertEquals(listOf("Press de banca", "Remo"), summary.exercises.map { it.name })
    }
}
