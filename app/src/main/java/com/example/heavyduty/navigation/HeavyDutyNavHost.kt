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
import com.example.heavyduty.feature.exercises.navigation.exercisePickerScreen
import com.example.heavyduty.feature.history.navigation.historyScreen
import com.example.heavyduty.feature.history.navigation.progressScreen
import com.example.heavyduty.feature.profile.navigation.profileScreen
import com.example.heavyduty.feature.routines.presentation.editor.SELECTED_EXERCISE_IDS_KEY
import com.example.heavyduty.feature.routines.navigation.routineEditorScreen
import com.example.heavyduty.feature.routines.navigation.routineListScreen
import com.example.heavyduty.feature.stats.navigation.statsGraph
import com.example.heavyduty.feature.timer.navigation.timerScreen
import com.example.heavyduty.feature.workout.navigation.activeWorkoutScreen

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

private const val TRANSITION_DURATION_MS = 220

/** Route without its optional arguments: "workout_graph?routineId={routineId}" → "workout_graph". */
private fun String?.baseRoute(): String? = this?.substringBefore('?')

private fun isTabSwitch(fromRoute: String?, toRoute: String?): Boolean =
    fromRoute.baseRoute() in topLevelRoutes && toRoute.baseRoute() in topLevelRoutes

/**
 * Navigation rules, applied consistently:
 * - Entering a section (tab, drawer item, or a shortcut to one) uses [navigateToTopLevel], so
 *   each section keeps its own stack and back always returns to the dashboard.
 * - Pushing a detail screen uses [navigateIfResumed], and every back arrow uses
 *   [popBackStackIfResumed], so double taps can't push twice or skip the previous screen.
 */
@Composable
fun HeavyDutyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navigateBack: () -> Unit = { navController.popBackStackIfResumed() }

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
        dashboardScreen(
            onNavigateToActiveWorkout = {
                navController.navigateToTopLevel(TopLevelDestination.WORKOUT.route)
            },
            // The workout tab lists the routines, each with its own "Iniciar" button.
            onNavigateToRoutines = {
                navController.navigateToTopLevel(TopLevelDestination.WORKOUT.route)
            },
            onStartRoutine = { routineId ->
                navController.navigateToWorkout(routineId)
            },
        )

        activeWorkoutScreen(
            onNavigateToRoutines = {
                navController.navigateToTopLevel(DrawerDestination.ROUTINES.route)
            },
            onWorkoutCompleted = {
                navController.navigate(TopLevelDestination.DASHBOARD.route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            },
        )

        exerciseListScreen(
            onExerciseClick = { exerciseId ->
                navController.navigateIfResumed("exercise_detail/$exerciseId")
            },
        )
        exerciseDetailScreen(
            onNavigateBack = navigateBack,
            onViewProgress = { exerciseId ->
                navController.navigateIfResumed("progress/$exerciseId")
            },
        )
        exercisePickerScreen(
            onExercisesSelected = { exerciseIds ->
                // Hands the picked ids back to whichever screen pushed this route (the
                // routine editor) via its own SavedStateHandle — the standard nav-result
                // pattern, so no shared ViewModel is needed between the two features.
                navController.previousBackStackEntry?.savedStateHandle
                    ?.set(SELECTED_EXERCISE_IDS_KEY, exerciseIds.toLongArray())
                navController.popBackStackIfResumed()
            },
            onCancel = navigateBack,
        )

        routineListScreen(
            onRoutineClick = { routineId ->
                navController.navigateIfResumed("routine_editor/$routineId")
            },
            onCreateRoutine = {
                navController.navigateIfResumed("routine_editor")
            },
            onStartRoutine = { routineId ->
                navController.navigateToWorkout(routineId)
            },
        )
        routineEditorScreen(
            onNavigateBack = navigateBack,
            onOpenExercisePicker = { navController.navigateIfResumed("exercise_picker") },
        )

        timerScreen()

        historyScreen(
            onSessionClick = { },
            onExerciseProgressClick = { exerciseId ->
                navController.navigateIfResumed("progress/$exerciseId")
            },
        )
        progressScreen(
            onNavigateBack = navigateBack,
        )

        calendarScreen(
            onRoutineClick = { routineId ->
                navController.navigateIfResumed("routine_editor/$routineId")
            },
        )

        profileScreen(
            onNavigateToStats = { navController.navigateIfResumed("stats_graph") },
        )

        statsGraph(
            onNavigateBack = navigateBack,
            onNavigateToSeries = { navController.navigateIfResumed("stats/series") },
            onNavigateToDistribucionRadar = { navController.navigateIfResumed("stats/distribucion_radar") },
            onNavigateToDistribucionCuerpo = { navController.navigateIfResumed("stats/distribucion_cuerpo") },
            onNavigateToPrincipales = { navController.navigateIfResumed("stats/principales") },
            onNavigateToClasificacion = { navController.navigateIfResumed("stats/clasificacion") },
            onNavigateToInformeMensual = { navController.navigateIfResumed("stats/informe_mensual") },
            onNavigateToMapaActividad = { navController.navigateIfResumed("stats/mapa_actividad") },
            onNavigateToPesoCorporal = { navController.navigateIfResumed("stats/peso_corporal") },
        )
    }
}
