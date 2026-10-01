package com.example.heavyduty.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Core destinations shown in the bottom navigation bar. Less frequent destinations
 * (Rutinas, Historial, Calendario, Perfil) live in [DrawerDestination] instead.
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val route: String,
    val associatedRoutes: List<String>,
) {
    DASHBOARD(
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = "Inicio",
        route = "dashboard",
        associatedRoutes = listOf("dashboard"),
    ),
    WORKOUT(
        selectedIcon = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow,
        label = "Entrenar",
        route = "workout_graph",
        associatedRoutes = listOf("workout_graph"),
    ),
    EXERCISES(
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
        label = "Ejercicios",
        route = "exercises_graph",
        associatedRoutes = listOf("exercises_graph", "exercise_detail"),
    ),
    TIMER(
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        label = "Temporizador",
        route = "timer_graph",
        associatedRoutes = listOf("timer_graph"),
    ),
}
