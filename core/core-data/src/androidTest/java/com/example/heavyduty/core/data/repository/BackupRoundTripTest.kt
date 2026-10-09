package com.example.heavyduty.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.heavyduty.core.data.database.HeavyDutyDatabase
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A backup exported from one install must restore completely on another — including one whose
 * exercise catalog was translated differently (a later app version), since catalog exercises
 * are matched by their stable English name.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var source: HeavyDutyDatabase
    private lateinit var target: HeavyDutyDatabase

    @Before
    fun createDatabases() {
        source = Room.inMemoryDatabaseBuilder(context, HeavyDutyDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, HeavyDutyDatabase::class.java).build()
    }

    @After
    fun closeDatabases() {
        source.close()
        target.close()
    }

    private fun HeavyDutyDatabase.backups() = BackupRepositoryImpl(this, exerciseDao(), routineDao(), workoutDao(), bodyWeightDao())

    @Test
    fun exportThenImport_restoresEverythingOnAnotherInstall() = runTest {
        val bench = source.exerciseDao().insertExercise(
            ExerciseEntity(name = "Press de banca con barra", nameEn = "barbell bench press", muscleGroup = "CHEST", exerciseType = "STRENGTH"),
        )
        val custom = source.exerciseDao().insertExercise(
            ExerciseEntity(name = "Mi press raro", muscleGroup = "CHEST", exerciseType = "STRENGTH", isCustom = true),
        )
        val routineId = source.routineDao().insertRoutine(RoutineEntity(name = "Torso"))
        source.routineDao().insertRoutineExercise(
            RoutineExerciseEntity(routineId = routineId, exerciseId = bench, orderIndex = 0, targetSets = 4, targetReps = 6, targetWeight = 61.23f, weightUnit = "LB"),
        )
        source.routineDao().insertRoutineExercise(
            RoutineExerciseEntity(routineId = routineId, exerciseId = custom, orderIndex = 1, supersetGroupId = "A"),
        )
        val sessionId = source.workoutDao().insertSession(
            WorkoutSessionEntity(sessionName = "Torso", startTime = 1_000, endTime = 5_000, totalDurationSecs = 4, status = "COMPLETED"),
        )
        source.workoutDao().insertSet(
            WorkoutSetEntity(sessionId = sessionId, exerciseId = bench, setNumber = 1, reps = 10, weight = 40f, isCompleted = true, completedAt = 2_000, isWarmup = true),
        )
        source.workoutDao().insertSet(
            WorkoutSetEntity(
                sessionId = sessionId, exerciseId = bench, setNumber = 2, reps = 6, weight = 61.23f, isCompleted = true,
                completedAt = 3_000, effortType = "RIR", effortValue = 2f, weightUnit = "LB",
            ),
        )
        source.bodyWeightDao().insertEntry(BodyWeightEntity(weightKg = 82.5f, loggedAt = 1_000))

        // The new install's catalog calls the same exercise something else in Spanish.
        target.exerciseDao().insertExercise(
            ExerciseEntity(name = "Press banca con barra", nameEn = "barbell bench press", muscleGroup = "CHEST", exerciseType = "STRENGTH"),
        )

        val result = target.backups().importSnapshot(source.backups().exportSnapshot())

        assertEquals(1, result.customExercisesImported)
        assertEquals(1, result.routinesImported)
        assertEquals(1, result.sessionsImported)
        assertEquals(1, result.bodyWeightEntriesImported)
        assertEquals(0, result.skippedReferences)

        val routine = target.routineDao().getAllRoutines().first().single()
        val entries = routine.routineExercises.sortedBy { it.routineExercise.orderIndex }
        assertEquals(listOf("Press banca con barra", "Mi press raro"), entries.map { it.exercise.name })
        assertEquals("LB", entries[0].routineExercise.weightUnit)
        assertEquals(61.23f, entries[0].routineExercise.targetWeight, 0.001f)
        assertEquals("A", entries[1].routineExercise.supersetGroupId)

        val sets = target.workoutDao().getAllLoggedSets().first().sortedBy { it.setNumber }
        assertEquals(2, sets.size)
        assertTrue(sets[0].isWarmup)
        assertEquals("RIR", sets[1].effortType)
        assertEquals("LB", sets[1].weightUnit)
        assertEquals(82.5f, target.bodyWeightDao().getAllEntries().first().single().weightKg)
    }

    @Test
    fun backupTakenMidWorkout_restoresThatWorkoutAsFinished() = runTest {
        val bench = source.exerciseDao().insertExercise(
            ExerciseEntity(name = "Press de banca con barra", nameEn = "barbell bench press", muscleGroup = "CHEST", exerciseType = "STRENGTH"),
        )
        val sessionId = source.workoutDao().insertSession(WorkoutSessionEntity(sessionName = "Torso", startTime = 1_000, status = "IN_PROGRESS"))
        source.workoutDao().insertSet(
            WorkoutSetEntity(sessionId = sessionId, exerciseId = bench, setNumber = 1, reps = 8, weight = 60f, isCompleted = true, completedAt = 61_000),
        )
        target.exerciseDao().insertExercise(
            ExerciseEntity(name = "Press de banca con barra", nameEn = "barbell bench press", muscleGroup = "CHEST", exerciseType = "STRENGTH"),
        )

        target.backups().importSnapshot(source.backups().exportSnapshot())

        // An imported "running" workout would take over the workout screen on the new install.
        assertNull(target.workoutDao().getActiveSession().first())
        val session = target.workoutDao().getAllSessions().first().single()
        assertEquals("COMPLETED", session.status)
        assertEquals(61_000L, session.endTime)
        assertEquals(60, session.totalDurationSecs)
        assertEquals(1, target.workoutDao().getAllLoggedSets().first().size)
    }
}
