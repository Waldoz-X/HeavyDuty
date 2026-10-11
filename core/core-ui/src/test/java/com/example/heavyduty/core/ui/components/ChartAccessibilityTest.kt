package com.example.heavyduty.core.ui.components

import com.example.heavyduty.core.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartAccessibilityTest {

    @Test
    fun `the radar reads each part's share of the whole, largest first`() {
        val shares = shareOfTotal(mapOf("Espalda" to 1500f, "Pecho" to 3000f, "Piernas" to 0f, "Hombros" to 500f))

        assertEquals(listOf("Pecho" to 60, "Espalda" to 30, "Hombros" to 10), shares)
    }

    @Test
    fun `nothing worked reads as nothing, not as a division by zero`() {
        assertTrue(shareOfTotal(mapOf("Pecho" to 0f)).isEmpty())
        assertTrue(workedMuscles(mapOf(MuscleGroup.CHEST to 0f)).isEmpty())
    }

    @Test
    fun `the body map names the worked muscles, most worked first`() {
        val muscles = workedMuscles(mapOf(MuscleGroup.TRICEPS to 0.4f, MuscleGroup.CHEST to 1f, MuscleGroup.BACK to 0f))

        assertEquals(listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), muscles)
    }
}
