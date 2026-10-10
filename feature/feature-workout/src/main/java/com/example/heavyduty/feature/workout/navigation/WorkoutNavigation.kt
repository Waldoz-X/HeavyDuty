package com.example.heavyduty.feature.workout.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.workout.presentation.ActiveWorkoutScreen

/**
 * Registers the workout tab — one destination for every way into a workout. `routineId` is
 * optional ("workout_graph" or "workout_graph?routineId=7"): when given, that routine is
 * started on arrival unless a workout is already running. A single destination means a single
 * screen per workout; two routes used to let two copies of the same session drift apart.
 */
fun NavGraphBuilder.activeWorkoutScreen(onNavigateToRoutines: () -> Unit, onWorkoutComplete: () -> Unit) {
    composable(
        route = "workout_graph?routineId={routineId}",
        arguments = listOf(
            navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) {
        ActiveWorkoutScreen(
            onNavigateToRoutines = onNavigateToRoutines,
            onWorkoutComplete = onWorkoutComplete
        )
    }
}
