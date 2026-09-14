package com.example.heavyduty.feature.workout.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.workout.presentation.ActiveWorkoutScreen

/**
 * Registers the active workout composable in the NavHost.
 */
fun NavGraphBuilder.activeWorkoutScreen(
    onNavigateToRoutines: () -> Unit,
    onWorkoutCompleted: () -> Unit,
) {
    composable(
        route = "workout_graph",
        arguments = emptyList(),
    ) {
        ActiveWorkoutScreen(
            onNavigateToRoutines = onNavigateToRoutines,
            onWorkoutCompleted = onWorkoutCompleted,
        )
    }
    composable(
        route = "active_workout/{routineId}",
        arguments = listOf(
            navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        ActiveWorkoutScreen(
            onNavigateToRoutines = onNavigateToRoutines,
            onWorkoutCompleted = onWorkoutCompleted,
        )
    }
}
