package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightSanityTest {

    @Test
    fun `a slip of the decimal point is caught`() {
        assertTrue(isUnusuallyHeavy(609f, bestKg = 60f))
        assertTrue(isUnusuallyHeavy(1000f, bestKg = 100f))
    }

    @Test
    fun `real progress is not questioned`() {
        assertFalse(isUnusuallyHeavy(62.5f, bestKg = 60f))
        assertFalse(isUnusuallyHeavy(80f, bestKg = 60f)) // +20 kg: not more than 20 over
        assertFalse(isUnusuallyHeavy(8f, bestKg = 4f)) // light dumbbells may double
        assertFalse(isUnusuallyHeavy(160f, bestKg = 140f))
    }

    @Test
    fun `with nothing to compare there is no telling`() {
        assertFalse(isUnusuallyHeavy(609f, bestKg = null))
        assertFalse(isUnusuallyHeavy(609f, bestKg = 0f))
        assertFalse(isUnusuallyHeavy(null, bestKg = 60f))
    }
}
