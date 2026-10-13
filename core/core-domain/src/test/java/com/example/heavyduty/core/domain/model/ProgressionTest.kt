package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the workout screen suggests next, per policy, from the exercise's logged history. */
class ProgressionTest {

    private fun config(
        policy: ProgressionPolicy,
        low: Int = 8,
        high: Int = 12,
        stall: Int = 3,
        sets: Int = 3,
        startKg: Float = 20f,
        perSide: Boolean = false
    ) = ProgressionConfig(policy, low, high, stall, sets, startKg, perSide)

    private var nextId = 1L

    /** One finished session: its working sets, in order, at [weight] (null = bodyweight). */
    private fun session(sessionId: Long, weight: Float?, vararg reps: Int, at: Long = sessionId * 1_000_000) = reps.mapIndexed { i, r ->
        WorkoutSet(
            id = nextId++,
            sessionId = sessionId,
            setNumber = i + 1,
            reps = r,
            weight = weight,
            isCompleted = true,
            completedAt = at + i
        )
    }

    @Test
    fun `without history every policy starts from the routine`() {
        assertEquals(
            Prescription(sets = 3, reps = 8, weightKg = 20f),
            nextPrescription(emptyList(), config(ProgressionPolicy.DOUBLE_PROGRESSION))
        )
        assertEquals(Prescription(sets = 3, reps = 12, weightKg = 20f), nextPrescription(emptyList(), config(ProgressionPolicy.LINEAR)))
        assertEquals(Prescription(sets = 3, durationSecs = 8), nextPrescription(emptyList(), config(ProgressionPolicy.TIME)))
    }

    @Test
    fun `off repeats the routine whatever was done`() {
        val history = session(1, 50f, 12, 12, 12)
        assertEquals(Prescription(sets = 3, reps = 12, weightKg = 20f), nextPrescription(history, config(ProgressionPolicy.OFF)))
    }

    // ------------------------------------------------------------------ double progression

    @Test
    fun `double progression adds a rep at the same weight`() {
        val next = nextPrescription(session(1, 60f, 10, 9, 9), config(ProgressionPolicy.DOUBLE_PROGRESSION))
        assertEquals(10, next.reps) // the weakest set, plus one
        assertEquals(60f, next.weightKg)
    }

    @Test
    fun `double progression adds weight once every set reaches the top`() {
        val next = nextPrescription(session(1, 60f, 12, 12, 12), config(ProgressionPolicy.DOUBLE_PROGRESSION))
        assertEquals(8, next.reps)
        assertEquals(62.5f, next.weightKg)
    }

    @Test
    fun `double progression never suggests fewer reps than the range starts at`() {
        // A bad day: 3 reps in a 5–8 range. Next time is 5 at the same weight, not 4.
        val next = nextPrescription(session(1, 117.5f, 5, 4, 3), config(ProgressionPolicy.DOUBLE_PROGRESSION, low = 5, high = 8))
        assertEquals(5, next.reps)
        assertEquals(117.5f, next.weightKg)
    }

    @Test
    fun `a range typed backwards does not crash`() {
        val next = nextPrescription(session(1, 40f, 6, 6), config(ProgressionPolicy.DOUBLE_PROGRESSION, low = 10, high = 8))
        assertEquals(8, next.reps)
    }

    @Test
    fun `per-side reps move two at a time`() {
        val next =
            nextPrescription(session(1, 20f, 16, 16), config(ProgressionPolicy.DOUBLE_PROGRESSION, low = 16, high = 24, perSide = true))
        assertEquals(18, next.reps)
    }

    @Test
    fun `only the latest session counts, and warm-ups and unchecked sets never do`() {
        val old = session(1, 50f, 12, 12, 12)
        val latest = session(2, 60f, 9, 9, 9)
        val warmup =
            WorkoutSet(
                id = 99,
                sessionId = 2,
                setNumber = 0,
                reps = 2,
                weight = 30f,
                isCompleted = true,
                completedAt = 2_000_000,
                isWarmup = true
            )
        val unchecked = WorkoutSet(id = 98, sessionId = 2, setNumber = 4, reps = 1, weight = 60f, isCompleted = false)
        val next = nextPrescription((latest + old + warmup + unchecked).shuffled(), config(ProgressionPolicy.DOUBLE_PROGRESSION))
        assertEquals(10, next.reps)
        assertEquals(60f, next.weightKg)
    }

    // ------------------------------------------------------------------ linear

    @Test
    fun `linear adds weight after a session at the target`() {
        val next = nextPrescription(session(1, 100f, 5, 5, 5), config(ProgressionPolicy.LINEAR, low = 5, high = 5))
        assertEquals(102.5f, next.weightKg)
        assertFalse(next.isDeload)
    }

    @Test
    fun `linear repeats the weight after a miss and deloads after the stall threshold`() {
        val cfg = config(ProgressionPolicy.LINEAR, low = 5, high = 5, stall = 3)
        val oneMiss = session(1, 100f, 5, 5, 4)
        assertEquals(100f, nextPrescription(oneMiss, cfg).weightKg)

        val threeMisses = session(1, 100f, 5, 4) + session(2, 100f, 5, 3) + session(3, 100f, 4, 4)
        val next = nextPrescription(threeMisses, cfg)
        assertTrue(next.isDeload)
        assertEquals(90f, next.weightKg) // −10 %, on the 2.5 kg grid
    }

    // ------------------------------------------------------------------ GreySkull

    @Test
    fun `greyskull reads the last set as the AMRAP`() {
        val cfg = config(ProgressionPolicy.GREYSKULL_LP, low = 5, high = 5)
        assertEquals(105f, nextPrescription(session(1, 100f, 5, 5, 10), cfg).weightKg) // doubled: double jump
        assertEquals(102.5f, nextPrescription(session(1, 100f, 5, 5, 6), cfg).weightKg)
        val missed = nextPrescription(session(1, 100f, 5, 5, 3), cfg)
        assertTrue(missed.isDeload)
        assertEquals(90f, missed.weightKg)
    }

    // ------------------------------------------------------------------ bodyweight and time

    @Test
    fun `bodyweight adds reps, then a set at the ceiling`() {
        val cfg = config(ProgressionPolicy.DOUBLE_PROGRESSION, low = 8, high = 12, sets = 3)
        val climbing = nextPrescription(session(1, null, 9, 9, 10), cfg)
        assertEquals(10, climbing.reps)
        assertNull(climbing.weightKg)

        val ceiling = nextPrescription(session(1, null, 12, 12, 12), cfg)
        assertEquals(4, ceiling.sets)
        assertEquals(8, ceiling.reps)
    }

    @Test
    fun `timed holds grow five seconds up to the ceiling`() {
        fun hold(vararg secs: Int) = secs.mapIndexed { i, s ->
            WorkoutSet(id = nextId++, sessionId = 1, setNumber = i + 1, durationSecs = s, isCompleted = true, completedAt = 1_000L + i)
        }
        val cfg = config(ProgressionPolicy.TIME, low = 30, high = 60)
        assertEquals(45, nextPrescription(hold(40, 45), cfg).durationSecs)
        assertEquals(60, nextPrescription(hold(58, 60), cfg).durationSecs)
    }
}
