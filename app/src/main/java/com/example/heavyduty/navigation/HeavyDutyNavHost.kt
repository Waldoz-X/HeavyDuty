package com.example.heavyduty.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.heavyduty.feature.calendar.navigation.calendarScreen
import com.example.heavyduty.feature.dashboard.navigation.dashboardScreen
import com.example.heavyduty.feature.exercises.navigation.exerciseDetailScreen
import com.example.heavyduty.feature.exercises.navigation.exerciseListScreen
import com.example.heavyduty.feature.history.navigation.historyScreen
import com.example.heavyduty.feature.history.navigation.progressScreen
import com.example.heavyduty.feature.profile.navigation.profileScreen
import com.example.heavyduty.feature.routines.navigation.routineEditorScreen
import com.example.heavyduty.feature.routines.navigation.routineListScreen
import com.example.heavyduty.feature.stats.navigation.statsGraph
import com.example.heavyduty.feature.timer.navigation.timerScreen
import com.example.heavyduty.feature.workout.navigation.activeWorkoutScreen

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

private const val TRANSITION_DURATION_MS = 220

/**
 * Switching between bottom-nav tabs (a sibling swap, not a hierarchical push) reads oddly with
 * a horizontal slide — it isn't "forward" or "back" navigation, so those tabs only crossfade.
 * Every other transition (list → detail, hub → sub-screen) slides, matching push/pop intent.
 */
private fun isTabSwitch(fromRoute: String?, toRoute: String?): Boolean =
    fromRoute in topLevelRoutes && toRoute in topLevelRoutes

@Composable
fun HeavyDutyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.DASHBOARD.route,
        modifier = modifier,
        enterTransition = {
            if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                fadeIn(tween(TRANSITION_DURATION_MS))
            } else {
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { it / 3 } + fadeIn(tween(TRANSITION_DURATION_MS))
            }
        },
        exitTransition = {
            if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                fadeOut(tween(TRANSITION_DURATION_MS))
            } else {
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { -it / 3 } + fadeOut(tween(TRANSITION_DURATION_MS))
            }
        },
        popEnterTransition = {
            if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                fadeIn(tween(TRANSITION_DURATION_MS))
            } else {
                slideInHorizontally(tween(TRANSITION_DURATION_MS)) { -it / 3 } + fadeIn(tween(TRANSITION_DURATION_MS))
            }
        },
        popExitTransition = {
            if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                fadeOut(tween(TRANSITION_DURATION_MS))
            } else {
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { it / 3 } + fadeOut(tween(TRANSITION_DURATION_MS))
            }
        },
    ) {
        // Dashboard
        dashboardScreen(
            onNavigateToActiveWorkout = {
                navController.navigate(TopLevelDestination.WORKOUT.route)
            },
            onNavigateToRoutines = {
                navController.navigate("routines_graph")
            },
            onStartRoutine = { routineId ->
                navController.navigate("active_workout/$routineId")
            },
        )

        // Workout
        activeWorkoutScreen(
            onNavigateToRoutines = {
                navController.navigate("routines_graph")
            },
            onWorkoutCompleted = {
                navController.navigate(TopLevelDestination.DASHBOARD.route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            }
        )

        // Exercises
        exerciseListScreen(
            onExerciseClick = { exerciseId ->
                navController.navigate("exercise_detail/$exerciseId")
            },
        )
        exerciseDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onViewProgress = { exerciseId ->
                navController.navigate("progress/$exerciseId")
            },
        )

        // Routines / Programs
        routineListScreen(
            onRoutineClick = { routineId ->
                navController.navigate("routine_editor/$routineId")
            },
            onProgramClick = { programId ->
                navController.navigate("program_detail/$programId")
            },
            onNavigateBack = { navController.popBackStack() },
        )
        routineEditorScreen(
            onNavigateBack = { navController.popBackStack() },
        )

        // Timer
        timerScreen()

        // History
        historyScreen(
            onSessionClick = { /* future detail */ },
            onExerciseProgressClick = { exerciseId ->
                navController.navigate("progress/$exerciseId")
            },
        )
        progressScreen(
            onNavigateBack = { navController.popBackStack() },
        )

        // Calendar
        calendarScreen(
            onRoutineClick = { routineId ->
                navController.navigate("routine_editor/$routineId")
            },
        )

        // Profile
        profileScreen(
            onNavigateToStats = { navController.navigate("stats_graph") },
        )

        // Statistics
        statsGraph(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSeries = { navController.navigate("stats/series") },
            onNavigateToDistribucionRadar = { navController.navigate("stats/distribucion_radar") },
            onNavigateToDistribucionCuerpo = { navController.navigate("stats/distribucion_cuerpo") },
            onNavigateToPrincipales = { navController.navigate("stats/principales") },
            onNavigateToClasificacion = { navController.navigate("stats/clasificacion") },
            onNavigateToInformeMensual = { navController.navigate("stats/informe_mensual") },
            onNavigateToMapaActividad = { navController.navigate("stats/mapa_actividad") },
        )
    }
}
