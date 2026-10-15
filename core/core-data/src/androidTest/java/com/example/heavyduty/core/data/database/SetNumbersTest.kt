package com.example.heavyduty.core.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** An exercise's sets in a workout stay numbered 1, 2, 3… through deletes and undos, in SQL. */
@RunWith(AndroidJUnit4::class)
class SetNumbersTest {

    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        HeavyDutyDatabase::class.java
    ).build()
    private val dao = database.workoutDao()

    @After
    fun close() = database.close()

    private suspend fun exercise(name: String) =
        database.exerciseDao().insertExercise(ExerciseEntity(name = name, muscleGroup = "CHEST", exerciseType = "STRENGTH"))

    private fun set(session: Long, exercise: Long, number: Int, reps: Int) = WorkoutSetEntity(
        sessionId = session,
        exerciseId = exercise,
        setNumber = number,
        reps = reps,
        weight = 60f,
        isCompleted = true,
        completedAt = number * 1_000L
    )

    /** reps by set number, for one exercise in one workout. */
    private suspend fun numbers(session: Long, exercise: Long) = dao.getAllLoggedSets().first()
        .filter { it.sessionId == session && it.exerciseId == exercise }
        .sortedBy { it.setNumber }
        .map { it.setNumber to it.reps }

    @Test
    fun deletingAMiddleSetClosesTheGapAndPuttingItBackReopensIt() = runTest {
        val bench = exercise("Banca")
        val row = exercise("Remo")
        val today = dao.insertSession(WorkoutSessionEntity(sessionName = "Torso", startTime = 1_000, status = "IN_PROGRESS"))
        val lastWeek = dao.insertSession(WorkoutSessionEntity(sessionName = "Torso", startTime = 0, status = "COMPLETED"))
        val ids = listOf(10, 9, 8).mapIndexed { i, reps -> dao.insertSetKeepingNumbers(set(today, bench, i + 1, reps)) }
        listOf(12, 11).forEachIndexed { i, reps -> dao.insertSetKeepingNumbers(set(today, row, i + 1, reps)) }
        listOf(7, 6, 5).forEachIndexed { i, reps -> dao.insertSetKeepingNumbers(set(lastWeek, bench, i + 1, reps)) }
        val second = dao.getAllLoggedSets().first().single { it.id == ids[1] }

        dao.deleteSetKeepingNumbers(second)
        assertEquals(listOf(1 to 10, 2 to 8), numbers(today, bench))

        dao.insertSetKeepingNumbers(second)
        assertEquals(listOf(1 to 10, 2 to 9, 3 to 8), numbers(today, bench))

        // Other exercises and other workouts are never renumbered.
        assertEquals(listOf(1 to 12, 2 to 11), numbers(today, row))
        assertEquals(listOf(1 to 7, 2 to 6, 3 to 5), numbers(lastWeek, bench))
    }
}
