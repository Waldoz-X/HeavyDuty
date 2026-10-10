package com.example.heavyduty

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.navigation.AppSections
import com.example.heavyduty.core.ui.LocalSharedTransitionScope
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.navigation.DrawerDestination
import com.example.heavyduty.navigation.HeavyDutyNavHost
import com.example.heavyduty.navigation.TopLevelDestination
import com.example.heavyduty.navigation.navigateToTopLevel
import com.example.heavyduty.ui.ActiveWorkoutBar
import com.example.heavyduty.ui.ActiveWorkoutRailButton
import com.example.heavyduty.ui.ForgottenWorkoutDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

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
            val forgottenWorkout by mainViewModel.forgottenWorkout.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
                HeavyDutyTheme {
                    HeavyDutyMainScreen(
                        activeWorkout = activeWorkout,
                        pendingSection = pendingSection,
                        onSectionOpen = mainViewModel::onSectionOpen
                    )
                    // Asked over whatever screen is open: a forgotten workout distorts every stat.
                    forgottenWorkout?.let { workout ->
                        ForgottenWorkoutDialog(
                            workout = workout,
                            onFinish = mainViewModel::finishForgottenWorkout,
                            onDiscard = mainViewModel::discardForgottenWorkout,
                            onKeepTraining = mainViewModel::keepTraining
                        )
                    }
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
    "stats/peso_corporal"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun HeavyDutyMainScreen(activeWorkout: ActiveWorkoutSummary?, pendingSection: String?, onSectionOpen: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentOnSectionOpen by rememberUpdatedState(onSectionOpen)
    LaunchedEffect(pendingSection) {
        val route = when (pendingSection) {
            AppSections.SECTION_WORKOUT -> TopLevelDestination.WORKOUT.route
            AppSections.SECTION_TIMER -> TopLevelDestination.TIMER.route
            else -> return@LaunchedEffect
        }
        navController.navigateToTopLevel(route)
        currentOnSectionOpen()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "HeavyDuty",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
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
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        val currentRouteHasOwnTopBar = currentDestination?.route?.let { it in ROUTES_WITH_OWN_TOP_BAR } ?: false
        // A phone on its side: the top bar, workout bar and bottom navigation together left a
        // strip of content, so navigation moves to a rail at the side instead.
        val compactHeight = isCompactHeight()
        val isSelected: (TopLevelDestination) -> Boolean = { destination ->
            currentDestination?.route?.let { currentRoute ->
                destination.associatedRoutes.any { associated -> currentRoute.startsWith(associated) }
            } == true
        }
        val navigate: (TopLevelDestination) -> Unit = { navController.navigateToTopLevel(it.route) }
        val openWorkout = { navController.navigateToTopLevel(TopLevelDestination.WORKOUT.route) }
        val openMenu: () -> Unit = { coroutineScope.launch { drawerState.open() } }
        // The workout screen shows the session itself; everywhere else a shortcut leads back.
        val onWorkoutScreen = currentDestination?.route?.startsWith(TopLevelDestination.WORKOUT.route) == true
        val showWorkoutShortcut = activeWorkout != null && !onWorkoutScreen
        // Keeps drawing the last workout while the shortcut animates out after finishing it.
        var shownWorkout by remember { mutableStateOf(activeWorkout) }
        if (activeWorkout != null) shownWorkout = activeWorkout

        // When the pushed screen owns its own TopAppBar, it already consumes the status bar
        // inset itself — without this, Scaffold reserves that inset a second time here (nothing
        // consumed it, since the outer topBar renders nothing), leaving a blank gap above the
        // pushed screen's own header.
        val routeInsets = if (currentRouteHasOwnTopBar) {
            ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
        } else {
            ScaffoldDefaults.contentWindowInsets
        }
        // Beside the rail the start side (the camera cutout) is the rail's. It's dropped here
        // outright rather than left to Scaffold's consumed-insets tracking, which forgets the
        // exclusion whenever these insets change on navigation.
        val contentInsets = if (compactHeight) routeInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End) else routeInsets

        Row(modifier = Modifier.fillMaxSize()) {
            if (compactHeight) {
                HeavyDutyNavigationRail(
                    isSelected = isSelected,
                    onNavigate = navigate,
                    onOpenMenu = openMenu,
                    workout = shownWorkout,
                    showWorkout = showWorkoutShortcut,
                    onOpenWorkout = openWorkout
                )
            }
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    // The rail already sits clear of the cutout on its side; screens mustn't pad for it again.
                    .then(
                        if (compactHeight) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                            )
                        } else {
                            Modifier
                        }
                    ),
                topBar = {
                    // In landscape the menu lives in the rail, and the bar's height is worth more as content.
                    if (!currentRouteHasOwnTopBar && !compactHeight) {
                        TopAppBar(
                            title = { Text(text = "HeavyDuty") },
                            navigationIcon = {
                                IconButton(onClick = openMenu) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.nav_open_menu),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                },
                contentWindowInsets = contentInsets,
                bottomBar = {
                    if (!compactHeight) {
                        Column {
                            AnimatedVisibility(
                                visible = showWorkoutShortcut,
                                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                            ) {
                                shownWorkout?.let { workout -> ActiveWorkoutBar(workout = workout, onClick = openWorkout) }
                            }
                            HeavyDutyNavigationBar(isSelected = isSelected, onNavigate = navigate)
                        }
                    }
                }
            ) { innerPadding ->
                // Lets screens share elements across navigation (see core.ui.hdSharedBounds).
                // Consumed as well as applied, so screens with their own Scaffold don't pad for the
                // same insets again (in landscape that left a cutout-wide gap beside the rail).
                SharedTransitionLayout(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        HeavyDutyNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

/** Below this height (a phone in landscape) navigation moves from the bottom to a side rail. */
private val COMPACT_HEIGHT = 480.dp

@Composable
private fun isCompactHeight(): Boolean {
    val height = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { height.toDp() } < COMPACT_HEIGHT
}

@Composable
private fun HeavyDutyNavigationBar(isSelected: (TopLevelDestination) -> Boolean, onNavigate: (TopLevelDestination) -> Unit) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = isSelected(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { DestinationIcon(destination, selected) },
                label = { NavBarLabel(stringResource(destination.labelRes)) }
            )
        }
    }
}

/**
 * Landscape navigation: the menu on top, the sections centred (only the selected one labelled,
 * so all four fit the short height), and the workout-in-progress shortcut at the bottom.
 */
@Composable
private fun HeavyDutyNavigationRail(
    isSelected: (TopLevelDestination) -> Boolean,
    onNavigate: (TopLevelDestination) -> Unit,
    onOpenMenu: () -> Unit,
    workout: ActiveWorkoutSummary?,
    showWorkout: Boolean,
    onOpenWorkout: () -> Unit
) {
    NavigationRail(
        header = {
            IconButton(onClick = onOpenMenu) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(R.string.nav_open_menu))
            }
        },
        // Clear of the camera cutout too, which sits on this side when the phone is turned left.
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TopLevelDestination.entries.forEach { destination ->
            val selected = isSelected(destination)
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { DestinationIcon(destination, selected) },
                label = { NavBarLabel(stringResource(destination.labelRes)) },
                alwaysShowLabel = false
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = showWorkout,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            workout?.let { ActiveWorkoutRailButton(workout = it, onClick = onOpenWorkout, modifier = Modifier.padding(bottom = 8.dp)) }
        }
    }
}

@Composable
private fun DestinationIcon(destination: TopLevelDestination, selected: Boolean) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = stringResource(destination.labelRes)
    )
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
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
