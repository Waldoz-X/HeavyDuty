package com.example.heavyduty.feature.timer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.timer.presentation.TimerScreen

/**
 * Registers the timer composable in the NavHost.
 */
fun NavGraphBuilder.timerScreen() {
    composable(route = "timer_graph") {
        TimerScreen()
    }
}
