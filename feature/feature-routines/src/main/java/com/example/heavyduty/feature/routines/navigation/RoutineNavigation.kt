package com.example.heavyduty.feature.routines.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.routines.presentation.editor.RoutineEditorScreen
import com.example.heavyduty.feature.routines.presentation.list.RoutineListScreen

/**
 * Registers the routine list composable in the NavHost.
 */
fun NavGraphBuilder.routineListScreen(
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
) {
    composable(route = "routines_graph") {
        RoutineListScreen(
            onRoutineClick = onRoutineClick,
            onCreateRoutine = onCreateRoutine,
        )
    }
}

/**
 * Registers the routine editor composable in the NavHost.
 * Supports both create (no routineId) and edit (with routineId) modes.
 */
fun NavGraphBuilder.routineEditorScreen(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = "routine_editor/{routineId}",
        arguments = listOf(
            navArgument("routineId") {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        RoutineEditorScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "routine_editor") {
        RoutineEditorScreen(onNavigateBack = onNavigateBack)
    }
}
