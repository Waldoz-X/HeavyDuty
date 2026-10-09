package com.example.heavyduty.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.heavyduty.core.data.database.HeavyDutyDatabase
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.domain.model.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A damaged, foreign or hand-edited backup must be refused as a whole — never half-imported,
 * and never written in a shape that later crashes the screens that read it back.
 */
@RunWith(AndroidJUnit4::class)
class BackupImportRobustnessTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: HeavyDutyDatabase
    private lateinit var backups: BackupRepositoryImpl

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(context, HeavyDutyDatabase::class.java).build()
        backups = BackupRepositoryImpl(db, db.exerciseDao(), db.routineDao(), db.workoutDao(), db.bodyWeightDao(), Clock.System)
        // What the user already has; a refused import must leave exactly this.
        db.exerciseDao().insertExercise(
            ExerciseEntity(name = "Press de banca con barra", nameEn = "barbell bench press", muscleGroup = "CHEST", exerciseType = "STRENGTH"),
        )
        db.routineDao().insertRoutine(RoutineEntity(name = "Mi rutina"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun contents() = listOf(
        db.exerciseDao().getAllExercises().first().size,
        db.routineDao().getAllRoutines().first().size,
        db.workoutDao().getAllSessions().first().size,
        db.workoutDao().getAllLoggedSets().first().size,
        db.bodyWeightDao().getAllEntries().first().size,
    )

    /** Everything the app's screens read, through the same mappers they use. */
    private suspend fun readEverythingLikeTheApp() {
        db.exerciseDao().getAllExercises().first().forEach { it.toDomain() }
        db.routineDao().getAllRoutines().first().forEach { it.toDomain() }
        db.workoutDao().getAllSessions().first().forEach { it.toDomain() }
        db.workoutDao().getAllLoggedSets().first().forEach { it.toDomain() }
    }

    private suspend fun assertRefused(json: String) {
        val before = contents()
        try {
            backups.importSnapshot(json)
            fail("Imported a bad backup: ${json.take(80)}")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertEquals("database changed by a refused import", before, contents())
        readEverythingLikeTheApp()
    }

    private val validSession = """
        {"sessionName":"Torso","startTime":1000,"endTime":5000,"totalDurationSecs":4,"status":"COMPLETED","notes":"","type":"GYM",
         "sets":[{"exerciseName":"barbell bench press","setNumber":1,"reps":8,"weight":80.0,"isCompleted":true,"completedAt":2000,"notes":"","isWarmup":false}]}
    """.trimIndent()

    private fun backup(
        sessions: String = validSession,
        customExercises: String = "",
        routines: String = """{"name":"Torso","exercises":[{"exerciseName":"barbell bench press","orderIndex":0}]}""",
        version: Int = 1,
    ) = """{"version":$version,"exportedAt":1,"customExercises":[$customExercises],"routines":[$routines],
           "sessions":[$sessions],"bodyWeightEntries":[{"weightKg":82.5,"loggedAt":1000}]}"""

    @Test
    fun aGoodBackupStillImports() = runTest {
        val result = backups.importSnapshot(backup())

        assertEquals(1, result.routinesImported)
        assertEquals(1, result.sessionsImported)
        assertEquals(listOf(1, 2, 1, 1, 1), contents())
        readEverythingLikeTheApp()
    }

    @Test
    fun aCutOffFileIsRefused() = runTest {
        val full = backup()
        assertRefused(full.take(full.length / 2))
    }

    @Test
    fun emptyOrForeignFilesAreRefused() = runTest {
        assertRefused("")
        assertRefused("null")
        assertRefused("hola, esto no es un respaldo")
        assertRefused("[1, 2, 3]")
        // Valid JSON from some other app: it must not "import 0 things" and call it a success.
        assertRefused("""{"contacts":[{"name":"Ana"}]}""")
    }

    @Test
    fun aBackupFromANewerAppVersionIsRefused() = runTest {
        assertRefused(backup(version = 2))
    }

    @Test
    fun oneBadWorkoutRefusesTheWholeFile() = runTest {
        // The first workout is fine; the second has a status this app doesn't know. Nothing at
        // all may be written — not the routines, not the first workout.
        assertRefused(backup(sessions = validSession + "," + validSession.replace("\"COMPLETED\"", "\"PAUSED\"")))
    }

    @Test
    fun unknownValuesThatWouldCrashTheAppAreRefused() = runTest {
        assertRefused(backup(customExercises = """{"name":"Mío","muscleGroup":"WINGS","exerciseType":"STRENGTH"}"""))
        assertRefused(backup(customExercises = """{"name":"Mío","exerciseType":"STRENGTH"}"""))
        assertRefused(backup(routines = """{"name":"Torso","type":"YOGA","exercises":[]}"""))
        assertRefused(backup(routines = """{"name":"Torso","exercises":[{"exerciseName":"barbell bench press","progressionPolicy":"MAGIC"}]}"""))
        assertRefused(backup(sessions = validSession.replace("\"GYM\"", "\"SPACE\"")))
    }

    @Test
    fun missingRequiredDataIsRefused() = runTest {
        assertRefused(backup(routines = """{"exercises":[]}""")) // routine without a name
        assertRefused(backup(sessions = """{"sessionName":"Sin fecha","status":"COMPLETED","sets":[]}"""))
        assertRefused(backup(sessions = validSession.replace("\"exerciseName\":\"barbell bench press\",", "")))
        assertRefused(backup().replace(""""weightKg":82.5""", """"weightKg":-3"""))
    }

    @Test
    fun anOlderBackupWithoutNewerOptionalFieldsImports() = runTest {
        // Written before notes, units, muscle weights or effort existed.
        val old = """{"version":1,"customExercises":[{"name":"Mi press raro","muscleGroup":"CHEST","exerciseType":"STRENGTH"}],
            "routines":[{"name":"Torso","exercises":[{"exerciseName":"Mi press raro"}]}],
            "sessions":[{"startTime":1000,"sets":[{"exerciseName":"Mi press raro","reps":8,"weight":60.0,"completedAt":2000}]}]}"""

        val result = backups.importSnapshot(old)

        assertEquals(1, result.customExercisesImported)
        assertEquals(1, result.sessionsImported)
        assertEquals(0, result.skippedReferences)
        readEverythingLikeTheApp()
        val set = db.workoutDao().getAllLoggedSets().first().single()
        assertEquals("", set.notes)
        assertEquals(60f, set.weight)
    }
}
