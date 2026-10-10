package com.example.heavyduty.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Switches to a top-level section (bottom bar tab or drawer item) the standard way: the current
 * section's stack is saved, the target's saved stack is restored, and repeated taps never stack
 * copies. Every entry point into a section goes through here, so "back" from a section always
 * lands on the dashboard instead of wherever the section happened to be pushed from.
 */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens the workout tab to start [routineId]. Unlike [navigateToTopLevel] this doesn't restore
 * the tab's saved entry, which would ignore the new argument; the workout screen itself reads
 * the running session from the database, so a fresh entry never loses a workout in progress.
 */
fun NavHostController.navigateToWorkout(routineId: Long) {
    navigate(workoutRoute(routineId)) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
    }
}

/**
 * Pushes [route] only from a settled screen. While a transition runs the new entry isn't
 * RESUMED yet, so a second tap on the outgoing screen can't push a duplicate.
 */
fun NavHostController.navigateIfResumed(route: String) {
    if (isCurrentEntryResumed()) navigate(route)
}

/**
 * Pops one screen, ignoring the extra taps of a double tap on a back arrow — which would
 * otherwise also pop the screen underneath — and never popping the last entry, which would
 * leave a blank screen.
 */
fun NavHostController.popBackStackIfResumed() {
    if (isCurrentEntryResumed() && previousBackStackEntry != null) popBackStack()
}

private fun NavHostController.isCurrentEntryResumed(): Boolean = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

fun workoutRoute(routineId: Long): String = "${TopLevelDestination.WORKOUT.route}?routineId=$routineId"
