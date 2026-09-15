package com.example.heavyduty.feature.exercises.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.exercises.presentation.detail.ExerciseDetailScreen
import com.example.heavyduty.feature.exercises.presentation.list.ExerciseListScreen

/**
 * Registers the exercise list composable in the NavHost.
 */
fun NavGraphBuilder.exerciseListScreen(
    onExerciseClick: (Long) -> Unit,
) {
    composable(route = "exercises_graph") {
        ExerciseListScreen(onExerciseClick = onExerciseClick)
    }
}

/**
 * Registers the exercise picker composable — the same exercise browser (search, filters,
 * images) as [exerciseListScreen], but in multi-select mode so a caller (e.g. the routine
 * editor) can send exercises into itself instead of navigating to exercise detail.
 */
fun NavGraphBuilder.exercisePickerScreen(
    onExercisesSelected: (List<Long>) -> Unit,
    onCancel: () -> Unit,
) {
    composable(route = "exercise_picker") {
        ExerciseListScreen(
            onExerciseClick = {},
            selectionMode = true,
            onConfirmSelection = onExercisesSelected,
            onCancelSelection = onCancel,
        )
    }
}

/**
 * Registers the exercise detail composable in the NavHost.
 */
fun NavGraphBuilder.exerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onViewProgress: (Long) -> Unit,
) {
    composable(
        route = "exercise_detail/{exerciseId}",
        arguments = listOf(
            navArgument("exerciseId") { type = NavType.LongType },
        ),
    ) {
        ExerciseDetailScreen(onNavigateBack = onNavigateBack, onViewProgress = onViewProgress)
    }
}
