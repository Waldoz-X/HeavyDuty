package com.example.heavyduty.feature.routines.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.heavyduty.feature.routines.presentation.editor.RoutineEditorScreen
import com.example.heavyduty.feature.routines.presentation.list.RoutineListScreen

import com.example.heavyduty.feature.routines.presentation.programs.ProgramListScreen
import com.example.heavyduty.feature.routines.presentation.programs.ProgramDetailScreen

/**
 * Registers the program list composable in the NavHost.
 */
fun NavGraphBuilder.routineListScreen(
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    onProgramClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable(route = "routines_graph") {
        ProgramListScreen(
            onProgramClick = onProgramClick
        )
    }
    composable(
        route = "program_detail/{programId}",
        arguments = listOf(navArgument("programId") { type = NavType.LongType })
    ) {
        ProgramDetailScreen(
            onNavigateBack = onNavigateBack,
            onRoutineClick = onRoutineClick
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
