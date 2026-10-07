package com.example.heavyduty.core.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrading the app must keep the user's data. Builds a version-15 database (the last one
 * before kg/lb per exercise and Spanish names) with a little of everything, migrates it, and
 * checks both the raw schema and what the app's DAOs read back.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, HeavyDutyDatabase::class.java)

    @After
    fun deleteDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate15To16_keepsEveryRowAndMatchesTheV16Schema() {
        helper.createDatabase(TEST_DB, 15).apply {
            insertV15Data()
            close()
        }

        // Also validates the migrated tables against schemas/16.json.
        val db = helper.runMigrationsAndValidate(TEST_DB, 16, true, MIGRATION_15_16)

        fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, count("exercises"))
        assertEquals(1, count("routines"))
        assertEquals(1, count("routine_exercises"))
        assertEquals(1, count("workout_sessions"))
        assertEquals(2, count("workout_sets"))
        assertEquals(1, count("body_weight_entries"))
        assertEquals(1, count("calendar_entries"))

        // New columns start empty: no unit chosen yet, names not localized yet.
        db.query("SELECT nameEn FROM exercises WHERE id = 1").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
        db.query("SELECT weightUnit FROM routine_exercises").use { it.moveToFirst(); assertTrue(it.isNull(0)) }
        db.query("SELECT weightUnit FROM workout_sets").use { it.moveToFirst(); assertTrue(it.isNull(0)) }
        db.close()
    }

    @Test
    fun migratedDatabase_opensWithRoomAndGetsSpanishNames() = runTest {
        helper.createDatabase(TEST_DB, 15).apply {
            insertV15Data()
            close()
        }

        val database = Room.databaseBuilder(context, HeavyDutyDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            // What DatabaseModule does on every open.
            SeedData.localizeExerciseNames(context, database.exerciseDao())

            val exercises = database.exerciseDao().getAllExercises().first().associateBy { it.id }
            val catalog = exercises.getValue(1)
            assertEquals("Press de banca con barra", catalog.name)
            assertEquals("barbell bench press", catalog.nameEn)
            // Custom exercises are the user's own words: never renamed.
            assertEquals("Mi press raro", exercises.getValue(2).name)

            val routine = database.routineDao().getAllRoutines().first().single()
            assertEquals("Torso", routine.routine.name)
            assertEquals(80f, routine.routineExercises.single().routineExercise.targetWeight)

            val sets = database.workoutDao().getAllLoggedSets().first().sortedBy { it.setNumber }
            assertEquals(listOf(8, 6), sets.map { it.reps })
            assertNull(sets.first().weightUnit)
            assertEquals(1, database.bodyWeightDao().getAllEntries().first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate16To17_addsEmptyEnglishInstructionsAndKeepsRows() {
        helper.createDatabase(TEST_DB, 15).apply {
            insertV15Data()
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 16, true, MIGRATION_15_16).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 17, true, MIGRATION_16_17)

        db.query("SELECT COUNT(*) FROM exercises").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT instructionsEn FROM exercises WHERE id = 1").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
        db.close()
    }

    @Test
    fun englishInstructions_matchEachRowEvenWhenNamesRepeat() = runTest {
        val database = Room.inMemoryDatabaseBuilder(context, HeavyDutyDatabase::class.java).build()
        try {
            val dao = database.exerciseDao()
            fun row(spanish: String) = com.example.heavyduty.core.data.database.entity.ExerciseEntity(
                name = "Elevación de talones", nameEn = "smith reverse calf raises", instructions = spanish,
                muscleGroup = "CALVES", exerciseType = "STRENGTH",
            )
            val a = dao.insertExercise(row("Paso A"))
            val b = dao.insertExercise(row("Paso B"))
            val custom = dao.insertExercise(row("Mío").copy(nameEn = "", isCustom = true))

            dao.setEnglishInstructions(
                listOf(
                    Triple("smith reverse calf raises", "Paso A", "Step A"),
                    Triple("smith reverse calf raises", "Paso B", "Step B"),
                ),
            )

            val byId = dao.getAllExercises().first().associateBy { it.id }
            assertEquals("Step A", byId.getValue(a).instructionsEn)
            assertEquals("Step B", byId.getValue(b).instructionsEn)
            assertEquals("", byId.getValue(custom).instructionsEn)
            assertEquals(0, dao.countExercisesWithoutEnglishInstructions())
        } finally {
            database.close()
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertV15Data() {
        execSQL(
            """INSERT INTO exercises (id, name, description, instructions, imageUrl, gifUrl, muscleGroup,
               exerciseType, equipment, isCustom, weightUnit, muscleWeightsJson)
               VALUES (1, 'barbell bench press', '', '', '', '', 'CHEST', 'STRENGTH', 'barbell', 0, 'KG', '')""",
        )
        execSQL(
            """INSERT INTO exercises (id, name, description, instructions, imageUrl, gifUrl, muscleGroup,
               exerciseType, equipment, isCustom, weightUnit, muscleWeightsJson)
               VALUES (2, 'Mi press raro', '', '', '', '', 'CHEST', 'STRENGTH', '', 1, 'LB', '')""",
        )
        execSQL("INSERT INTO routines (id, name, description, type, createdAt, updatedAt) VALUES (1, 'Torso', '', 'GYM', 1, 1)")
        execSQL(
            """INSERT INTO routine_exercises (id, routineId, exerciseId, orderIndex, targetSets, targetReps,
               targetWeight, targetDurationSecs, restBetweenSetsSecs, notes, progressionPolicy, repRangeLow,
               repRangeHigh, stallThreshold, supersetGroupId)
               VALUES (1, 1, 1, 0, 3, 8, 80.0, 0, 90, '', 'LINEAR', 8, 10, 3, NULL)""",
        )
        execSQL(
            """INSERT INTO workout_sessions (id, routineId, sessionName, startTime, endTime, totalDurationSecs,
               status, notes, type) VALUES (1, 1, 'Torso', 1000, 4000, 3, 'COMPLETED', '', 'GYM')""",
        )
        execSQL(
            """INSERT INTO workout_sets (id, sessionId, exerciseId, setNumber, reps, weight, durationSecs, distance,
               isCompleted, completedAt, notes, isWarmup, effortType, effortValue)
               VALUES (1, 1, 1, 1, 8, 80.0, NULL, NULL, 1, 2000, '', 0, 'RIR', 2.0)""",
        )
        execSQL(
            """INSERT INTO workout_sets (id, sessionId, exerciseId, setNumber, reps, weight, durationSecs, distance,
               isCompleted, completedAt, notes, isWarmup, effortType, effortValue)
               VALUES (2, 1, 1, 2, 6, 85.0, NULL, NULL, 1, 3000, '', 0, NULL, NULL)""",
        )
        execSQL("INSERT INTO body_weight_entries (id, weightKg, loggedAt) VALUES (1, 82.5, 1000)")
        execSQL("INSERT INTO calendar_entries (id, routineId, dayOfWeek, recurrence, isActive) VALUES (1, 1, 1, 'WEEKLY', 1)")
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
