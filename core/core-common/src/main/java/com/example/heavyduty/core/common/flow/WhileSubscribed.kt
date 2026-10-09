package com.example.heavyduty.core.common.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn

/** How long work keeps running after the screen stops watching — enough to ride out a rotation. */
const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * This state as seen by a screen, with [load] running only while the screen is collecting it.
 *
 * A ViewModel whose `init` collects database flows keeps recomputing for as long as it lives —
 * including while its screen sits in the back stack and the user logs sets elsewhere. Here
 * [load] starts when the screen starts collecting and is cancelled [STOP_TIMEOUT_MILLIS] after
 * it stops; coming back starts it again, and since Room flows emit their current result right
 * away, the screen catches up at once. Meanwhile the last state stays on screen, and events
 * can keep updating it as usual.
 *
 * [load] launches its collectors in the scope it receives, which is the one that gets cancelled.
 */
fun <T> MutableStateFlow<T>.whileSubscribed(scope: CoroutineScope, load: CoroutineScope.() -> Unit): StateFlow<T> {
    val state = this
    return channelFlow {
        load()
        state.collect { send(it) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), state.value)
}
