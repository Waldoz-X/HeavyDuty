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

/** The workout detail's route pattern, for whoever matches against it (the app's top bar). */
const val WORKOUT_DETAIL_ROUTE = "workout_detail/{sessionId}?finished={finished}"

/** The detail of [sessionId]; [finished] when arriving straight from finishing it, to say so. */
fun workoutDetailRoute(sessionId: Long, finished: Boolean = false): String = "workout_detail/$sessionId?finished=$finished"

/** Registers a finished workout's detail, where it can be corrected or deleted — and the summary shown on finishing. */
fun NavGraphBuilder.workoutDetailScreen(onNavigateBack: () -> Unit) {
    composable(
        route = WORKOUT_DETAIL_ROUTE,
        arguments = listOf(
            navArgument("sessionId") { type = NavType.LongType },
            navArgument("finished") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { entry ->
        WorkoutDetailScreen(onNavigateBack = onNavigateBack, justFinished = entry.arguments?.getBoolean("finished") == true)
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
