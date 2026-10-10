package com.example.heavyduty.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.profile.presentation.ProfileScreen

/**
 * Registers the profile composable in the NavHost.
 */
fun NavGraphBuilder.profileScreen(onNavigateToStats: () -> Unit) {
    composable(route = "profile_graph") {
        ProfileScreen(onNavigateToStats = onNavigateToStats)
    }
}
