package com.example.heavyduty.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.calendar.presentation.CalendarScreen

/**
 * Registers the calendar composable in the NavHost.
 */
fun NavGraphBuilder.calendarScreen(onRoutineClick: (Long) -> Unit) {
    composable(route = "calendar_graph") {
        CalendarScreen(onRoutineClick = onRoutineClick)
    }
}
