package com.example.heavyduty.feature.timer.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(modifier: Modifier = Modifier, viewModel: TimerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    TimerEffects(effects = viewModel.uiEffect, snackbarHostState = snackbarHostState)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        TimerContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onStartPause = {
                // The countdown keeps going in a notification; on Android 13+ that needs the permission.
                if (!uiState.isRunning && needsNotificationPermission(context)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.onEvent(TimerUiEvent.OnStartPause)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        )
    }
}

/** "Time's up" and other messages, each shown once. */
@Composable
private fun TimerEffects(effects: Flow<TimerUiEffect>, snackbarHostState: SnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                TimerUiEffect.TimerFinished -> coroutineScope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.timer_finished))
                }

                is TimerUiEffect.ShowMessage -> coroutineScope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }
}

@Composable
private fun TimerContent(uiState: TimerUiState, onEvent: (TimerUiEvent) -> Unit, onStartPause: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.timer_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TimerModeSelector(selected = uiState.mode, onEvent = onEvent)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(uiState.mode.descriptionResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        TimerDial(uiState = uiState)
        Spacer(modifier = Modifier.height(40.dp))
        if (uiState.mode != TimerMode.STOPWATCH && !uiState.isRunning) {
            DurationPresets(mode = uiState.mode, totalSeconds = uiState.totalSeconds, onEvent = onEvent)
            Spacer(modifier = Modifier.height(24.dp))
        }
        TimerControls(isRunning = uiState.isRunning, onReset = { onEvent(TimerUiEvent.OnReset) }, onStartPause = onStartPause)
    }
}

private fun needsNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

@Composable
private fun TimerModeSelector(selected: TimerMode, onEvent: (TimerUiEvent) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(TimerMode.entries.toList()) { mode ->
            HdChip(
                text = stringResource(mode.nameResId),
                isSelected = selected == mode,
                onClick = { onEvent(TimerUiEvent.OnModeSelect(mode)) }
            )
        }
    }
}

/** The time left (or elapsed, for the stopwatch) inside a ring that empties as it runs. */
@Composable
private fun TimerDial(uiState: TimerUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        DialRing(progress = dialProgress(uiState), modifier = Modifier.fillMaxSize())
        DialTime(uiState = uiState)
    }
}

/** How full the ring is: the share of the countdown left; none for the stopwatch. */
private fun dialProgress(uiState: TimerUiState): Float = when {
    uiState.mode == TimerMode.STOPWATCH -> 0f
    uiState.totalSeconds > 0 -> uiState.remainingSeconds.toFloat() / uiState.totalSeconds.toFloat()
    else -> 1f
}

@Composable
private fun DialRing(progress: Float, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        // Background track, then what's left of the countdown on top.
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = stroke
        )
        if (progress > 0f) {
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = arcSize,
                style = stroke
            )
        }
    }
}

/** The time — left, or elapsed for the stopwatch — and the round when there are several. */
@Composable
private fun DialTime(uiState: TimerUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val displaySeconds = if (uiState.mode == TimerMode.STOPWATCH) uiState.elapsedSeconds else uiState.remainingSeconds
        Text(
            text = formatTime(displaySeconds),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (uiState.rounds > 1) {
            Text(
                text = stringResource(R.string.timer_round, uiState.currentRound, uiState.rounds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Quick durations for the countdown modes. */
@Composable
private fun DurationPresets(mode: TimerMode, totalSeconds: Int, onEvent: (TimerUiEvent) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val presets = when (mode) {
            TimerMode.REST -> listOf(30, 60, 90, 120, 180)
            TimerMode.AMRAP -> listOf(300, 600, 900, 1200)
            TimerMode.EMOM -> listOf(60, 90, 120)
            TimerMode.TABATA -> listOf(20, 30, 40)
            else -> emptyList()
        }
        items(presets) { seconds ->
            HdChip(
                text = formatTime(seconds),
                isSelected = totalSeconds == seconds,
                onClick = { onEvent(TimerUiEvent.OnDurationChange(seconds)) }
            )
        }
    }
}

/** Reset, and the big play/pause button. */
@Composable
private fun TimerControls(isRunning: Boolean, onReset: () -> Unit, onStartPause: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        IconButton(
            onClick = onReset,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = stringResource(R.string.timer_reset),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play/Pause FAB
        FloatingActionButton(
            onClick = onStartPause,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (isRunning) R.string.timer_pause else R.string.timer_start),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
