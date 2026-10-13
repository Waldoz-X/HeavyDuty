package com.example.heavyduty.core.data.database

import com.example.heavyduty.core.data.database.SeedData.ExerciseDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** How one catalog entry turns into the exercise row seeded on first launch. */
class CatalogMappingTest {

    private val benchPress = ExerciseDto(
        id = "0025",
        name = "barbell bench press",
        category = "chest",
        body_part = "chest",
        equipment = "barbell",
        instructions = mapOf("es" to "Túmbate en el banco.", "en" to "Lie on the bench."),
        instruction_steps = null,
        muscle_group = null,
        secondary_muscles = listOf("triceps", "shoulders"),
        target = "pectorals",
        image = "images/0025.png",
        gif_url = "gifs/0025.gif",
        media_id = null
    )

    @Test
    fun `a catalog entry becomes a strength exercise with its Spanish name, media and muscles`() {
        val entity = benchPress.toEntity(spanishNames = mapOf("barbell bench press" to "Press de banca con barra"))

        assertEquals("Press de banca con barra", entity.name)
        assertEquals("barbell bench press", entity.nameEn)
        assertEquals("CHEST", entity.muscleGroup)
        assertEquals("STRENGTH", entity.exerciseType)
        assertEquals("Barbell", entity.equipment)
        assertEquals("Túmbate en el banco.", entity.instructions)
        assertEquals("Lie on the bench.", entity.instructionsEn)
        assertEquals("https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/0025.png", entity.imageUrl)
        assertEquals("https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/gifs/0025.gif", entity.gifUrl)
        assertEquals("""{"CHEST":0.6,"TRICEPS":0.2,"SHOULDERS":0.2}""", entity.muscleWeightsJson)
    }

    @Test
    fun `an entry without a translation, equipment or media keeps what it has`() {
        val entity = benchPress.copy(equipment = null, image = "", gif_url = null, secondary_muscles = null).toEntity(emptyMap())

        assertEquals("barbell bench press", entity.name)
        assertEquals("None", entity.equipment)
        assertEquals("", entity.imageUrl)
        assertEquals("", entity.gifUrl)
        assertEquals("""{"CHEST":1.0}""", entity.muscleWeightsJson)
    }

    @Test
    fun `the category decides cardio and functional, everything else is strength`() {
        assertEquals("CARDIO", exerciseTypeOf("Cardio"))
        assertEquals("FUNCTIONAL", exerciseTypeOf("stretching"))
        assertEquals("FUNCTIONAL", exerciseTypeOf("plyometrics"))
        assertEquals("STRENGTH", exerciseTypeOf("upper arms"))
        assertEquals("STRENGTH", exerciseTypeOf(null))
    }

    @Test
    fun `the target muscle wins over the body part, which is only the fallback`() {
        assertEquals("TRICEPS", muscleGroupOf(target = "triceps", bodyPart = "upper arms"))
        assertEquals("BICEPS", muscleGroupOf(target = null, bodyPart = "upper arms"))
        assertEquals("QUADRICEPS", muscleGroupOf(target = "Adductors", bodyPart = "upper legs"))
        assertEquals("FULL_BODY", muscleGroupOf(target = "unknown", bodyPart = null))
    }

    @Test
    fun `secondary muscles share 40 percent, never counting the primary twice`() {
        assertEquals(mapOf("BACK" to 1f), muscleWeights("BACK", listOf("upper back")))
        assertEquals(mapOf("LATS" to 0.6f, "BICEPS" to 0.4f), muscleWeights("LATS", listOf("biceps", "Biceps", "nonexistent")))
    }
}
