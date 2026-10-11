package com.example.heavyduty.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.history.presentation.detail.WorkoutDetailScreen
import com.example.heavyduty.feature.history.presentation.list.HistoryListScreen
import com.example.heavyduty.feature.history.presentation.progress.ProgressScreen

/**
 * Registers the history list composable in the NavHost.
 */
fun NavGraphBuilder.historyScreen(
    onSessionClick: (Long) -> Unit,
    onExerciseProgressClick: (Long) -> Unit,
    onStartWorkout: () -> Unit = {}
) {
    composable(route = "history_graph") {
        HistoryListScreen(
            onSessionClick = onSessionClick,
            onExerciseProgressClick = onExerciseProgressClick,
            onStartWorkout = onStartWorkout
        )
    }
}

/** Registers a finished workout's detail, where it can be corrected or deleted. */
fun NavGraphBuilder.workoutDetailScreen(onNavigateBack: () -> Unit) {
    composable(
        route = "workout_detail/{sessionId}",
        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
    ) {
        WorkoutDetailScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Registers the exercise progress composable in the NavHost.
 */
fun NavGraphBuilder.progressScreen(onNavigateBack: () -> Unit) {
    composable(
        route = "progress/{exerciseId}",
        arguments = listOf(
            navArgument("exerciseId") { type = NavType.LongType }
        )
    ) {
        ProgressScreen(onNavigateBack = onNavigateBack)
    }
}
