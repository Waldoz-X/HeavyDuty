package com.example.heavyduty.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.heavyduty.core.common.R

/**
 * Secondary destinations reachable from the hamburger menu drawer.
 */
enum class DrawerDestination(
    val icon: ImageVector,
    @param:StringRes val labelRes: Int,
    val route: String,
) {
    ROUTINES(
        icon = Icons.Default.ListAlt,
        labelRes = R.string.nav_routines,
        route = "routines_graph",
    ),
    HISTORY(
        icon = Icons.Default.History,
        labelRes = R.string.nav_history,
        route = "history_graph",
    ),
    CALENDAR(
        icon = Icons.Default.CalendarMonth,
        labelRes = R.string.nav_calendar,
        route = "calendar_graph",
    ),
    PROFILE(
        icon = Icons.Default.AccountCircle,
        labelRes = R.string.nav_profile,
        route = "profile_graph",
    ),
}
