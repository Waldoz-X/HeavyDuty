package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetEntryTest {

    private val planned = RoutineExercise(exerciseId = 1, targetSets = 3, targetReps = 8, targetWeight = 60f)
    private val state = ActiveWorkoutUiState(routineExercises = listOf(planned))

    @Test
    fun `what was typed is logged`() {
        val entry = state.copy(repsInput = "10", weightInput = "62.5", effortInput = "2").setEntry()

        assertEquals(SetEntry(reps = 10, durationSecs = null, weightKg = 62.5f, effort = Effort.Rir(2)), entry)
    }

    @Test
    fun `empty inputs fall back to the plan shown in them`() {
        assertEquals(SetEntry(reps = 8, durationSecs = null, weightKg = 60f, effort = null), state.setEntry())
    }

    @Test
    fun `a weight typed in pounds is stored in kg`() {
        val inPounds = ActiveWorkoutUiState(routineExercises = listOf(planned.copy(weightUnit = WeightUnit.LB)), weightInput = "100")

        assertEquals(45.36f, inPounds.setEntry()!!.weightKg!!, 0.01f)
    }

    @Test
    fun `effort is read on the chosen scale`() {
        val entry = state.copy(effortInput = "8.5", preferredEffortScale = "RPE").setEntry()

        assertEquals(Effort.Rpe(8.5f), entry!!.effort)
    }

    @Test
    fun `a hold logs its time, not reps, and needs one`() {
        val hold = state.copy(isTimedInput = true, repsInput = "10")

        assertNull(hold.setEntry())
        assertEquals(
            SetEntry(reps = null, durationSecs = 45, weightKg = 60f, effort = null),
            hold.copy(workTimerElapsedSecs = 45).setEntry()
        )
    }

    @Test
    fun `without reps typed nor planned there's nothing to log`() {
        val free = ActiveWorkoutUiState(routineExercises = listOf(RoutineExercise(exerciseId = 1, targetSets = 0, targetReps = 0)))

        assertNull(free.setEntry())
    }

    @Test
    fun `a record has to beat every earlier set by estimated 1RM`() {
        val earlier = listOf(WorkoutSet(weight = 100f, reps = 5, isCompleted = true))

        // 100 × 5 → 116.7; 105 × 5 → 122.5 beats it, 100 × 5 again doesn't.
        assertEquals(122.5f, newRecordEstimate(WorkoutSet(weight = 105f, reps = 5), earlier)!!, 0.01f)
        assertNull(newRecordEstimate(WorkoutSet(weight = 100f, reps = 5), earlier))
    }

    @Test
    fun `no record for a warm-up, nor the first time an exercise is done`() {
        val earlier = listOf(WorkoutSet(weight = 100f, reps = 5, isCompleted = true))

        assertNull(newRecordEstimate(WorkoutSet(weight = 140f, reps = 5, isWarmup = true), earlier))
        assertNull(newRecordEstimate(WorkoutSet(weight = 140f, reps = 5), emptyList()))
    }
}
