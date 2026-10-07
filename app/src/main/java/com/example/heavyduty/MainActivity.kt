package com.example.heavyduty

import com.example.heavyduty.core.common.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.example.heavyduty.core.ui.LocalSharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.heavyduty.core.common.navigation.AppSections
import com.example.heavyduty.ui.ActiveWorkoutBar
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
        // A recreated activity (rotation…) already handled its launch intent.
        if (savedInstanceState == null) mainViewModel.openSection(intent.getStringExtra(AppSections.EXTRA_OPEN_SECTION))
        setContent {
            val weightUnit by mainViewModel.preferredWeightUnit.collectAsStateWithLifecycle()
            val activeWorkout by mainViewModel.activeWorkout.collectAsStateWithLifecycle()
            val pendingSection by mainViewModel.pendingSection.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
                HeavyDutyTheme {
                    HeavyDutyMainScreen(
                        activeWorkout = activeWorkout,
                        pendingSection = pendingSection,
                        onSectionOpened = mainViewModel::onSectionOpened,
                    )
                }
            }
        }
    }

    /** A notification tapped while the app is already open (see AppSections). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.openSection(intent.getStringExtra(AppSections.EXTRA_OPEN_SECTION))
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
    // The detail draws its own back button over a full-bleed header image.
    "exercise_detail/{exerciseId}",
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun HeavyDutyMainScreen(
    activeWorkout: ActiveWorkoutSummary?,
    pendingSection: String?,
    onSectionOpened: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pendingSection) {
        val route = when (pendingSection) {
            AppSections.SECTION_WORKOUT -> TopLevelDestination.WORKOUT.route
            AppSections.SECTION_TIMER -> TopLevelDestination.TIMER.route
            else -> return@LaunchedEffect
        }
        navController.navigateToTopLevel(route)
        onSectionOpened()
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
                        label = { Text(text = stringResource(destination.labelRes)) },
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
                                    contentDescription = stringResource(R.string.nav_open_menu),
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
                Column {
                    // The workout screen shows the session itself; everywhere else a bar leads back.
                    val onWorkoutScreen = currentDestination?.route?.startsWith(TopLevelDestination.WORKOUT.route) == true
                    // Keeps drawing the last workout while the bar animates out after finishing it.
                    var shownWorkout by remember { mutableStateOf(activeWorkout) }
                    if (activeWorkout != null) shownWorkout = activeWorkout
                    AnimatedVisibility(
                        visible = activeWorkout != null && !onWorkoutScreen,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                    ) {
                        shownWorkout?.let { workout ->
                            ActiveWorkoutBar(
                                workout = workout,
                                onClick = { navController.navigateToTopLevel(TopLevelDestination.WORKOUT.route) },
                            )
                        }
                    }
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
                                        contentDescription = stringResource(destination.labelRes),
                                    )
                                },
                                label = { NavBarLabel(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            // Lets screens share elements across navigation (see core.ui.hdSharedBounds).
            SharedTransitionLayout(modifier = Modifier.padding(innerPadding)) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    HeavyDutyNavHost(navController = navController)
                }
            }
        }
    }
}

/**
 * Bottom-bar label kept on one line: with a large system font, "Temporizador" used to wrap into
 * "Temporizado / r". It shrinks only as much as needed to fit, and only when it doesn't.
 */
@Composable
private fun NavBarLabel(text: String) {
    val style = MaterialTheme.typography.labelMedium
    BasicText(
        text = text,
        style = style.copy(color = LocalContentColor.current),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize),
        // A little room at the sides: the last tab sits at the screen edge, and a label exactly
        // as wide as its tab had its last letter clipped there.
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
