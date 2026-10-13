package com.example.heavyduty.feature.timer.engine

import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiEffect
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import com.example.heavyduty.feature.timer.service.TimerNotifications
import com.example.heavyduty.feature.timer.service.TimerService
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [device] is a relaxed mock: starting/stopping [TimerService] and vibrating on completion
 * aren't what this test is about — it verifies the countdown state machine itself, driven
 * purely by virtual time via [StandardTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private val device: TimerDevice = mockk(relaxed = true)
    private val notifications: TimerNotifications = mockk(relaxed = true)

    @Test
    fun `counts down every second while running`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.REST))
        engine.onEvent(TimerUiEvent.OnDurationChange(10))

        engine.onEvent(TimerUiEvent.OnStartPause)
        advanceTimeBy(3.seconds)
        runCurrent()

        assertEquals(7, engine.state.value.remainingSeconds)
        assertTrue(engine.state.value.isRunning)
    }

    @Test
    fun `pausing stops the countdown without resetting remaining time`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.REST))
        engine.onEvent(TimerUiEvent.OnDurationChange(10))
        engine.onEvent(TimerUiEvent.OnStartPause)
        advanceTimeBy(3.seconds)
        runCurrent()

        engine.onEvent(TimerUiEvent.OnStartPause)
        val remainingAtPause = engine.state.value.remainingSeconds
        advanceTimeBy(5.seconds)
        runCurrent()

        assertEquals(remainingAtPause, engine.state.value.remainingSeconds)
        assertTrue(engine.state.value.isPaused)
        assertFalse(engine.state.value.isRunning)
    }

    @Test
    fun `reset returns to the configured duration`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.REST))
        engine.onEvent(TimerUiEvent.OnDurationChange(30))
        engine.onEvent(TimerUiEvent.OnStartPause)
        advanceTimeBy(10.seconds)
        runCurrent()

        engine.onEvent(TimerUiEvent.OnReset)

        val state = engine.state.value
        assertEquals(30, state.remainingSeconds)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun `finishing the only round emits TimerFinished and stops running`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        val effects = mutableListOf<TimerUiEffect>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            engine.effects.toList(effects)
        }

        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.REST))
        engine.onEvent(TimerUiEvent.OnDurationChange(2))
        engine.onEvent(TimerUiEvent.OnStartPause)
        advanceUntilIdle()

        assertEquals(listOf(TimerUiEffect.TimerFinished), effects)
        assertFalse(engine.state.value.isRunning)
        assertEquals(0, engine.state.value.remainingSeconds)
    }

    @Test
    fun `advances to the next round instead of finishing when more rounds remain`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        val effects = mutableListOf<TimerUiEffect>()
        backgroundScope.launch { engine.effects.toList(effects) }

        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.TABATA))
        engine.onEvent(TimerUiEvent.OnDurationChange(2))
        engine.onEvent(TimerUiEvent.OnRoundsChange(2))
        engine.onEvent(TimerUiEvent.OnStartPause)
        advanceTimeBy(2.seconds)
        runCurrent()

        assertTrue(effects.isEmpty())
        assertEquals(2, engine.state.value.currentRound)
        assertTrue(engine.state.value.isRunning)
    }

    @Test
    fun `a rest that runs out alerts the lifter, a skipped one doesn't`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)

        engine.startRest(2)
        advanceUntilIdle()
        verify(exactly = 1) { notifications.notifyRestFinished() }

        engine.startRest(30)
        runCurrent()
        engine.skipRest()
        advanceUntilIdle()
        verify(exactly = 1) { notifications.notifyRestFinished() }
        verify(exactly = 2) { notifications.cancelRestFinished() }
    }

    @Test
    fun `a skipped workout rest gives the timer tab its own setup back`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.TABATA))

        engine.startRest(60)
        engine.adjustRest(15)
        runCurrent()
        engine.skipRest()

        val state = engine.state.value
        assertEquals(TimerMode.TABATA, state.mode)
        assertEquals(20, state.totalSeconds)
        assertEquals(20, state.remainingSeconds)
        assertEquals(8, state.rounds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `a workout rest that runs out also gives the setup back`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.AMRAP))

        engine.startRest(2)
        engine.startRest(3) // back-to-back rests keep the setup from before the first one
        advanceUntilIdle()

        assertEquals(TimerMode.AMRAP, engine.state.value.mode)
        assertEquals(600, engine.state.value.totalSeconds)
        assertFalse(engine.state.value.isRunning)
    }

    @Test
    fun `picking a mode during a workout rest keeps that choice`() = runTest {
        val engine = TimerEngine(StandardTestDispatcher(testScheduler), device, notifications)
        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.TABATA))
        engine.startRest(60)

        engine.onEvent(TimerUiEvent.OnModeSelect(TimerMode.EMOM))
        engine.onEvent(TimerUiEvent.OnReset)

        assertEquals(TimerMode.EMOM, engine.state.value.mode)
        assertEquals(60, engine.state.value.totalSeconds)
    }
}
