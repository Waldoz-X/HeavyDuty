package com.example.heavyduty.feature.timer.engine

import android.content.Context
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiEffect
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import com.example.heavyduty.feature.timer.service.TimerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [context] is a relaxed mock: TimerEngine only uses it to start/stop [TimerService] and to
 * vibrate on completion, neither of which this test cares about — it verifies the countdown
 * state machine itself, driven purely by virtual time via [StandardTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private val context: Context = mockk(relaxed = true)

    @Test
    fun `counts down every second while running`() = runTest {
        val engine = TimerEngine(context, StandardTestDispatcher(testScheduler))
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
        val engine = TimerEngine(context, StandardTestDispatcher(testScheduler))
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
        val engine = TimerEngine(context, StandardTestDispatcher(testScheduler))
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
        val engine = TimerEngine(context, StandardTestDispatcher(testScheduler))
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
        val engine = TimerEngine(context, StandardTestDispatcher(testScheduler))
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
}
