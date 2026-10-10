package com.example.heavyduty.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.dashboard.presentation.DashboardScreen

const val DASHBOARD_ROUTE = "dashboard"

fun NavController.navigateToDashboard(navOptions: NavOptions? = null) {
    this.navigate(DASHBOARD_ROUTE, navOptions)
}

fun NavGraphBuilder.dashboardScreen(
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onStartRoutine: (Long) -> Unit
) {
    composable(route = DASHBOARD_ROUTE) {
        DashboardScreen(
            onNavigateToActiveWorkout = onNavigateToActiveWorkout,
            onNavigateToRoutines = onNavigateToRoutines,
            onStartRoutine = onStartRoutine
        )
    }
}
