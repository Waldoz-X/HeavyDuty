package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.CalendarEntryEntity
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.Recurrence
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A stored value the app doesn't know (from a newer version, a bad import, a damaged
 * database) must read as a safe default — it used to throw, crashing every screen listing it.
 */
class EnumMappingTest {

    @Test
    fun `known values read back as themselves`() {
        val exercise = ExerciseEntity(name = "Banca", muscleGroup = "CHEST", exerciseType = "CARDIO", weightUnit = "LB").toDomain()
        assertEquals(MuscleGroup.CHEST, exercise.muscleGroup)
        assertEquals(ExerciseType.CARDIO, exercise.exerciseType)
        assertEquals(WeightUnit.LB, exercise.weightUnit)
        assertEquals(WorkoutStatus.IN_PROGRESS, WorkoutSessionEntity(status = "IN_PROGRESS").toDomain().status)
    }

    @Test
    fun `an exercise with unknown values still reads`() {
        val exercise = ExerciseEntity(name = "Raro", muscleGroup = "WINGS", exerciseType = "YOGA", weightUnit = "STONE").toDomain()

        assertEquals(MuscleGroup.FULL_BODY, exercise.muscleGroup)
        assertEquals(ExerciseType.STRENGTH, exercise.exerciseType)
        assertEquals(WeightUnit.KG, exercise.weightUnit)
        assertEquals(mapOf(MuscleGroup.FULL_BODY to 1f), exercise.muscleWeights)
    }

    @Test
    fun `an unknown workout status is neither counted as finished nor resumed as running`() {
        val session = WorkoutSessionEntity(status = "PAUSED", type = "SPACE").toDomain()

        assertEquals(WorkoutStatus.CANCELLED, session.status)
        assertEquals(WorkoutType.GYM, session.type)
    }

    @Test
    fun `routines and calendar entries with unknown values still read`() {
        assertEquals(WorkoutType.GYM, RoutineEntity(name = "Torso", type = "YOGA").toDomain().type)
        assertEquals(
            ProgressionPolicy.OFF, // never auto-change weights by a rule this version doesn't know
            RoutineExerciseEntity(routineId = 1, exerciseId = 1, orderIndex = 0, progressionPolicy = "MAGIC").toDomain().progressionPolicy
        )
        assertEquals(Recurrence.WEEKLY, CalendarEntryEntity(routineId = 1, dayOfWeek = 1, recurrence = "MONTHLY").toDomain().recurrence)
    }
}
