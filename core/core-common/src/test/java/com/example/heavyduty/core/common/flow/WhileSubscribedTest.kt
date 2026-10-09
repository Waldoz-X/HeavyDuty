package com.example.heavyduty.core.common.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhileSubscribedTest {

    @Test
    fun `work runs only while the screen watches and catches up when it comes back`() = runTest {
        val database = MutableStateFlow(1) // stands in for a Room flow
        val state = MutableStateFlow("loading")
        var loads = 0
        val ui = state.whileSubscribed(backgroundScope) {
            loads++
            launch { database.collect { value -> state.value = "data $value" } }
        }
        runCurrent()
        assertEquals("nothing runs before the screen shows", 0, loads)

        val screen = backgroundScope.launch { ui.collect { } }
        runCurrent()
        assertEquals(1, loads)
        assertEquals("data 1", ui.value)

        screen.cancel()
        advanceTimeBy(STOP_TIMEOUT_MILLIS + 1)
        database.value = 2 // a set logged while the screen is in the back stack
        runCurrent()
        assertEquals("not recomputed while away", "data 1", state.value)

        backgroundScope.launch { ui.collect { } }
        runCurrent()
        assertEquals(2, loads)
        assertEquals("data 2", ui.value)
    }

    @Test
    fun `a quick rotation doesn't restart the work`() = runTest {
        val state = MutableStateFlow(0)
        var loads = 0
        val ui = state.whileSubscribed(backgroundScope) { loads++ }

        val first = backgroundScope.launch { ui.collect { } }
        runCurrent()
        first.cancel()
        advanceTimeBy(1_000)
        backgroundScope.launch { ui.collect { } }
        runCurrent()

        assertEquals(1, loads)
    }
}
