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

/** The date-range queries every stats screen and the dashboard total up. */
@RunWith(AndroidJUnit4::class)
class WorkoutRangeQueryTest {

    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        HeavyDutyDatabase::class.java,
    ).build()
    private val dao = database.workoutDao()

    @After
    fun close() = database.close()

    @Test
    fun aWorkoutPastMidnightKeepsAllItsSetsInThePeriodItStarted() = runTest {
        val exercise = database.exerciseDao().insertExercise(ExerciseEntity(name = "Sentadilla", muscleGroup = "QUADRICEPS", exerciseType = "STRENGTH"))
        val midnight = 10 * 86_400_000L
        val late = dao.insertSession(WorkoutSessionEntity(sessionName = "Pierna", startTime = midnight - 20 * 60_000, status = "COMPLETED"))
        dao.insertSet(WorkoutSetEntity(sessionId = late, exerciseId = exercise, setNumber = 1, reps = 5, weight = 100f, isCompleted = true, completedAt = midnight - 60_000))
        dao.insertSet(WorkoutSetEntity(sessionId = late, exerciseId = exercise, setNumber = 2, reps = 5, weight = 100f, isCompleted = true, completedAt = midnight + 60_000))
        // Not counted: unfinished set, unfinished workout.
        dao.insertSet(WorkoutSetEntity(sessionId = late, exerciseId = exercise, setNumber = 3, reps = 5, weight = 100f, isCompleted = false))
        val running = dao.insertSession(WorkoutSessionEntity(sessionName = "Pierna", startTime = midnight - 10_000, status = "IN_PROGRESS"))
        dao.insertSet(WorkoutSetEntity(sessionId = running, exerciseId = exercise, setNumber = 1, reps = 5, weight = 100f, isCompleted = true, completedAt = midnight - 5_000))

        val dayBefore = 0L until midnight
        val dayAfter = midnight until 2 * midnight

        assertEquals(2, dao.getSessionsByDateRange(dayBefore.first, dayBefore.last).first().size)
        assertEquals(listOf(1, 2), dao.getCompletedSetsByDateRange(dayBefore.first, dayBefore.last).first().map { it.setNumber }.sorted())
        assertEquals(0, dao.getSessionsByDateRange(dayAfter.first, dayAfter.last).first().size)
        assertEquals(0, dao.getCompletedSetsByDateRange(dayAfter.first, dayAfter.last).first().size)
    }
}
