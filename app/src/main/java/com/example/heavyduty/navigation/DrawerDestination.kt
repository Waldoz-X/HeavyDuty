package com.example.heavyduty.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Secondary destinations reachable from the hamburger menu drawer.
 */
enum class DrawerDestination(
    val icon: ImageVector,
    val label: String,
    val route: String,
) {
    ROUTINES(
        icon = Icons.Default.ListAlt,
        label = "Rutinas",
        route = "routines_graph",
    ),
    HISTORY(
        icon = Icons.Default.History,
        label = "Historial",
        route = "history_graph",
    ),
    CALENDAR(
        icon = Icons.Default.CalendarMonth,
        label = "Calendario",
        route = "calendar_graph",
    ),
    PROFILE(
        icon = Icons.Default.AccountCircle,
        label = "Perfil",
        route = "profile_graph",
    ),
}
