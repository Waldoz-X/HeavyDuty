package com.example.heavyduty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.navigation.DrawerDestination
import com.example.heavyduty.navigation.HeavyDutyNavHost
import com.example.heavyduty.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeavyDutyTheme {
                HeavyDutyMainScreen()
            }
        }
    }
}

// Routes whose own screen already renders a Scaffold + TopAppBar with a back arrow.
// Showing the outer "HeavyDuty" bar above those doubles up the header, so it's hidden
// there — the pushed screen's own back arrow is the only way back, which always leads
// to a route outside this set where the outer bar reappears.
private val ROUTES_WITH_OWN_TOP_BAR = setOf(
    "program_detail/{programId}",
    "routine_editor/{routineId}",
    "routine_editor",
    "progress/{exerciseId}",
    "stats_graph",
    "stats/series",
    "stats/distribucion_radar",
    "stats/distribucion_cuerpo",
    "stats/principales",
    "stats/clasificacion",
    "stats/informe_mensual",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeavyDutyMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "HeavyDuty",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
                DrawerDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                        label = { Text(text = destination.label) },
                        selected = selected,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateTo(destination.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        val currentRouteHasOwnTopBar = currentDestination?.route?.let { it in ROUTES_WITH_OWN_TOP_BAR } ?: false

        Scaffold(
            topBar = {
                if (!currentRouteHasOwnTopBar) {
                    TopAppBar(
                        title = { Text(text = "HeavyDuty") },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Abrir menú",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                    )
                }
            },
            // When the pushed screen owns its own TopAppBar, it already consumes the status
            // bar inset itself — without this, Scaffold reserves that inset a second time
            // here (nothing consumed it, since the outer topBar renders nothing), leaving a
            // blank gap above the pushed screen's own header.
            contentWindowInsets = if (currentRouteHasOwnTopBar) {
                ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
            } else {
                ScaffoldDefaults.contentWindowInsets
            },
            bottomBar = {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTo(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(text = destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            HeavyDutyNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
