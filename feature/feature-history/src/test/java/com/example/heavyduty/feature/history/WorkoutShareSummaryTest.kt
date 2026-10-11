package com.example.heavyduty.feature.history

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.feature.history.presentation.detail.ExerciseSets
import com.example.heavyduty.feature.history.presentation.detail.shareExerciseLines
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
}
