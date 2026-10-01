package com.example.heavyduty

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.LocalWeightUnit
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.navigation.DrawerDestination
import com.example.heavyduty.navigation.HeavyDutyNavHost
import com.example.heavyduty.navigation.TopLevelDestination
import com.example.heavyduty.navigation.navigateToTopLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    /**
     * Applies the app language (see HeavyDutyApp) to this activity's resources:
     * - Android 12 and older have no per-app language setting, so Spanish is applied directly.
     * - On Android 13+ the system does it, except on the very first launch: HeavyDutyApp sets
     *   Spanish moments before this activity is created and the system hasn't applied it yet,
     *   so the first screen came up in the phone's language until the app was reopened.
     */
    override fun attachBaseContext(newBase: Context) {
        val appLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            newBase.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)
        } else {
            Locale.forLanguageTag(HeavyDutyApp.SPANISH).also { Locale.setDefault(it) }
        }
        if (appLocale == null || newBase.resources.configuration.locales[0] == appLocale) {
            super.attachBaseContext(newBase)
            return
        }
        val config = Configuration(newBase.resources.configuration).apply { setLocale(appLocale) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val weightUnit by mainViewModel.preferredWeightUnit.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
                HeavyDutyTheme {
                    HeavyDutyMainScreen()
                }
            }
        }
    }
}

// Routes whose own screen already renders a Scaffold + TopAppBar with a back arrow.
// Showing the outer "HeavyDuty" bar above those doubles up the header, so it's hidden
// there — the pushed screen's own back arrow is the only way back, which always leads
// to a route outside this set where the outer bar reappears.
private val ROUTES_WITH_OWN_TOP_BAR = setOf(
    "routine_editor/{routineId}",
    "routine_editor",
    "exercise_picker",
    "progress/{exerciseId}",
    "stats_graph",
    "stats/series",
    "stats/distribucion_radar",
    "stats/distribucion_cuerpo",
    "stats/principales",
    "stats/clasificacion",
    "stats/informe_mensual",
    "stats/mapa_actividad",
    "stats/peso_corporal",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeavyDutyMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

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
                            navController.navigateToTopLevel(destination.route)
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
                        val selected = currentDestination?.route?.let { currentRoute ->
                            destination.associatedRoutes.any { associated ->
                                currentRoute.startsWith(associated)
                            }
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination.route) },
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
