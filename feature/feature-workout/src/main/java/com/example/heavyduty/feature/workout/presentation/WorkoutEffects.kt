package com.example.heavyduty.feature.workout.presentation

import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The workout screen's one-off effects. A Channel, not a SharedFlow: an effect sent while the
 * screen isn't collecting (rotation, background) waits for it instead of being dropped, and is
 * delivered exactly once.
 */
internal class WorkoutEffects(private val scope: CoroutineScope) {
    private val channel = Channel<ActiveWorkoutUiEffect>(Channel.BUFFERED)
    val flow: Flow<ActiveWorkoutUiEffect> = channel.receiveAsFlow()

    suspend fun send(effect: ActiveWorkoutUiEffect) = channel.send(effect)

    fun post(effect: ActiveWorkoutUiEffect) {
        scope.launch { channel.send(effect) }
    }

    fun showMessage(@StringRes messageResId: Int) = post(ActiveWorkoutUiEffect.ShowMessage(messageResId))
}
