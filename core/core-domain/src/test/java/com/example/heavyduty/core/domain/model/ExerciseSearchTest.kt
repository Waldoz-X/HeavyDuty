package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSearchTest {

    private val benchPress = exercise(1, "barbell bench press", MuscleGroup.CHEST, "barbell")
    private val inclineDumbbell = exercise(2, "dumbbell incline bench press", MuscleGroup.CHEST, "dumbbell")
    private val squat = exercise(3, "barbell full squat", MuscleGroup.QUADRICEPS, "barbell")
    private val deadlift = exercise(4, "barbell deadlift", MuscleGroup.BACK, "barbell")
    private val pullUp = exercise(5, "pull-up", MuscleGroup.BACK, "body weight")
    private val lateralRaise = exercise(6, "dumbbell lateral raise", MuscleGroup.SHOULDERS, "dumbbell")
    private val catalog = listOf(benchPress, inclineDumbbell, squat, deadlift, pullUp, lateralRaise)

    private fun search(query: String) = catalog.searchExercises(query).map { it.id }

    @Test
    fun `empty query returns everything, alphabetically by the name shown`() {
        assertEquals(catalog.sortedBy { it.displayName.lowercase() }.map { it.id }, search("  "))
    }

    @Test
    fun `spanish words map to the catalog's english names`() {
        assertEquals(listOf(1L, 2L), search("press banca"))
        assertEquals(listOf(2L), search("press banca inclinado con mancuernas"))
        assertEquals(listOf(3L), search("sentadilla"))
        assertEquals(listOf(4L), search("peso muerto"))
        assertEquals(listOf(5L), search("dominadas"))
        assertEquals(listOf(6L), search("elevaciones laterales"))
    }

    @Test
    fun `accents, case and hyphens don't matter`() {
        assertEquals(listOf(5L), search("PULL UP"))
        assertEquals(listOf(5L), search("pullup"))
        assertEquals(listOf(6L), search("elevación lateral"))
    }

    @Test
    fun `every word must match`() {
        assertEquals(listOf(1L), search("banca barra"))
        assertEquals(emptyList<Long>(), search("sentadilla mancuerna"))
    }

    @Test
    fun `name matches rank before equipment-only matches`() {
        val results = listOf(squat, benchPress).searchExercises("barbell")
        assertEquals(listOf(squat.id, benchPress.id).sorted(), results.map { it.id }.sorted())
    }

    @Test
    fun `a match in the spanish name ranks before one found only in the english name`() {
        val jm =
            Exercise(
                id = 10,
                name = "Press JM con barra",
                nameEn = "barbell jm bench press",
                muscleGroup = MuscleGroup.TRICEPS,
                exerciseType = ExerciseType.STRENGTH
            )
        val bench =
            Exercise(
                id = 11,
                name = "Press de banca con barra",
                nameEn = "barbell bench press",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.STRENGTH
            )

        assertEquals(listOf(11L, 10L), listOf(jm, bench).searchExercises("press banca barra").map { it.id })
    }

    private fun exercise(id: Long, name: String, muscleGroup: MuscleGroup, equipment: String) = Exercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        exerciseType = ExerciseType.STRENGTH,
        equipment = equipment
    )
}
