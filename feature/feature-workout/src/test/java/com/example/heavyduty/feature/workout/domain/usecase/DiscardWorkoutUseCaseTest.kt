package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscardWorkoutUseCaseTest {

    private val inProgress = WorkoutSession(id = 1, startTime = 1_000, status = WorkoutStatus.IN_PROGRESS)
    private val finished = WorkoutSession(id = 2, startTime = 500, status = WorkoutStatus.COMPLETED)
    private val repository = FakeWorkoutRepository(
        sessions = listOf(inProgress, finished),
        sets = listOf(
            WorkoutSet(id = 10, sessionId = 1, exerciseId = 1, reps = 8, weight = 60f, isCompleted = true),
            WorkoutSet(id = 20, sessionId = 2, exerciseId = 1, reps = 8, weight = 60f, isCompleted = true)
        )
    )
    private val discard = DiscardWorkoutUseCase(repository)

    @Test
    fun `a workout in progress goes, with its sets`() = runTest {
        discard(inProgress.id)

        assertEquals(listOf(2L), repository.sessions.value.map { it.id })
        assertEquals(listOf(20L), repository.sets.value.map { it.id })
    }

    @Test
    fun `a finished workout is history and is never discarded`() = runTest {
        discard(finished.id)

        assertEquals(listOf(1L, 2L), repository.sessions.value.map { it.id })
        assertEquals(2, repository.sets.value.size)
    }

    @Test
    fun `an unknown workout is ignored`() = runTest {
        discard(99)

        assertEquals(2, repository.sessions.value.size)
    }
}
