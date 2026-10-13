package com.example.heavyduty.core.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.repository.TrainingStatsRepositoryImpl
import com.example.heavyduty.core.domain.model.bestWeightsBefore
import com.example.heavyduty.core.domain.model.exerciseSetSummaries
import com.example.heavyduty.core.domain.model.lastHardSetAtByExercise
import com.example.heavyduty.core.domain.model.latestCompletedSession
import com.example.heavyduty.core.domain.model.trainingTotals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The database answers the stats' summary questions with aggregate SQL; the core-domain
 * functions over plain lists define the right answer. Both must agree on data full of the
 * cases that are easy to get wrong.
 */
@RunWith(AndroidJUnit4::class)
class TrainingAggregatesTest {

    private lateinit var db: HeavyDutyDatabase
    private lateinit var repository: TrainingStatsRepositoryImpl

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, HeavyDutyDatabase::class.java).build()
        repository = TrainingStatsRepositoryImpl(db.workoutDao())
        val dao = db.workoutDao()
        val bench = db.exerciseDao().insertExercise(ExerciseEntity(name = "Banca", muscleGroup = "CHEST", exerciseType = "STRENGTH"))
        val squat = db.exerciseDao().insertExercise(
            ExerciseEntity(name = "Sentadilla", muscleGroup = "QUADRICEPS", exerciseType = "STRENGTH")
        )
        val plank = db.exerciseDao().insertExercise(ExerciseEntity(name = "Plancha", muscleGroup = "ABS", exerciseType = "STRENGTH"))
        val curl = db.exerciseDao().insertExercise(ExerciseEntity(name = "Curl", muscleGroup = "BICEPS", exerciseType = "STRENGTH"))

        suspend fun workout(start: Long, status: String = "COMPLETED", vararg sets: WorkoutSetEntity) {
            val id = dao.insertSession(
                WorkoutSessionEntity(
                    startTime = start,
                    endTime = if (status ==
                        "COMPLETED"
                    ) {
                        start + 3_600_000
                    } else {
                        null
                    },
                    totalDurationSecs = 3600,
                    status = status
                )
            )
            sets.forEachIndexed { i, set -> dao.insertSet(set.copy(sessionId = id, setNumber = i + 1)) }
        }
        fun set(exercise: Long, reps: Int?, weight: Float?, at: Long, warmup: Boolean = false, done: Boolean = true) = WorkoutSetEntity(
            sessionId = 0,
            exerciseId = exercise,
            setNumber = 1,
            reps = reps,
            weight = weight,
            isCompleted = done,
            completedAt = if (done) at else null,
            isWarmup = warmup
        )

        workout(
            1_000_000,
            sets = arrayOf(
                set(bench, 8, 100f, 1_000_100), // 126.7: bench's best
                set(bench, 1, 120f, 1_000_200), // a true single counts as itself
                set(bench, 1, 160f, 1_000_300, warmup = true), // warm-ups never count
                set(squat, 15, 100f, 1_000_400), // over 12 reps: no estimate
                set(plank, null, null, 1_000_500), // a timed set: counted, no weight
                set(curl, 10, 20f, 1_000_600) // 26.7
            )
        )
        workout(
            2_000_000,
            sets = arrayOf(
                set(squat, 5, 80f, 2_000_100), // squat's only estimate: 93.3
                set(bench, 10, 70f, 2_000_200), // 70 / 126.7 = 55 %: not hard
                set(bench, 3, 95f, 2_000_300), // 75 %: hard, and the latest hard bench set
                set(curl, 10, 10f, 2_000_400), // 37 %: not hard
                set(bench, 12, 0f, 2_000_500) // no weight
            )
        )
        // None of these count: an unfinished and a cancelled workout, and an unchecked set.
        workout(3_000_000, "IN_PROGRESS", set(bench, 5, 200f, 3_000_100))
        workout(4_000_000, "CANCELLED", set(squat, 5, 300f, 4_000_100))
        workout(5_000_000, sets = arrayOf(set(curl, 5, 50f, 0, done = false)))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun sessions() = db.workoutDao().getAllSessions().first().map { it.toDomain() }
    private suspend fun completedSets() = db.workoutDao().getAllCompletedSets().first().map { it.toDomain() }

    @Test
    fun exerciseSummariesMatchTheReference() = runTest {
        val expected = exerciseSetSummaries(completedSets()).sortedBy { it.exerciseId }
        val actual = repository.getExerciseSetSummaries().first().sortedBy { it.exerciseId }

        assertEquals(expected.map { it.exerciseId }, actual.map { it.exerciseId })
        expected.zip(actual).forEach { (want, got) ->
            assertEquals("sets of ${want.exerciseId}", want.workingSets, got.workingSets)
            if (want.bestOneRepMaxKg == null) {
                assertEquals(null, got.bestOneRepMaxKg)
            } else {
                assertEquals(want.bestOneRepMaxKg!!, got.bestOneRepMaxKg!!, 0.01f)
            }
            assertEquals(want.bestSetWeightKg, got.bestSetWeightKg)
            assertEquals(want.bestSetReps, got.bestSetReps)
            assertEquals(want.bestSetAt, got.bestSetAt)
        }
        assertNotNull("the data must exercise a real estimate", actual.first().bestOneRepMaxKg)
    }

    @Test
    fun lastHardSetsMatchTheReference() = runTest {
        val expected = lastHardSetAtByExercise(completedSets())

        assertEquals(expected, repository.getLastHardSetTimes().first())
        assertEquals("bench's latest hard set is the 95 kg triple", 2_000_300L, expected.values.max())
    }

    @Test
    fun totalsMatchTheReference() = runTest {
        val expected = trainingTotals(sessions(), completedSets())
        val actual = repository.getAllTimeTotals().first()

        assertEquals(expected.workouts, actual.workouts)
        assertEquals(expected.durationSecs, actual.durationSecs)
        assertEquals(expected.sets, actual.sets)
        assertEquals(expected.volumeKg, actual.volumeKg, 0.01f)
    }

    @Test
    fun bestWeightsBeforeMatchTheReference() = runTest {
        for (time in listOf(0L, 1_500_000L, 2_500_000L, 10_000_000L)) {
            assertEquals(
                "before $time",
                bestWeightsBefore(sessions(), completedSets(), time),
                repository.getBestWeightsBefore(time).first()
            )
        }
    }

    @Test
    fun latestCompletedSessionMatchesTheReference() = runTest {
        assertEquals(latestCompletedSession(sessions())?.id, repository.getLatestCompletedSession().first()?.id)
    }
}
