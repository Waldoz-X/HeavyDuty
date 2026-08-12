package com.example.heavyduty.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.dashboard.presentation.DashboardScreen

const val dashboardRoute = "dashboard"

fun NavController.navigateToDashboard(navOptions: NavOptions? = null) {
    this.navigate(dashboardRoute, navOptions)
}

fun NavGraphBuilder.dashboardScreen(
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToRoutines: () -> Unit
) {
    composable(route = dashboardRoute) {
        DashboardScreen(
            onNavigateToActiveWorkout = onNavigateToActiveWorkout,
            onNavigateToRoutines = onNavigateToRoutines
        )
    }
}
