package com.example.heavyduty.feature.profile.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.feature.profile.presentation.TrainingReminderCard

/**
 * The first-run welcome, shown once over the whole app: a name, the unit, the training
 * reminder, then either the first routine or straight in. "Omitir" keeps what was chosen so far.
 * The choices are saved by the time [onFinish] runs; the host then marks the welcome completed,
 * after arranging where to go next.
 */
@Composable
fun OnboardingScreen(
    onFinish: (createFirstRoutine: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.Finished -> currentOnFinish(effect.createFirstRoutine)
            }
        }
    }
    OnboardingContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
internal fun OnboardingContent(uiState: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val isLast = uiState.page == OnboardingPage.entries.last()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PageDots(current = uiState.page, modifier = Modifier.weight(1f))
            if (!isLast) {
                TextButton(onClick = { onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = false)) }, enabled = !uiState.isFinishing) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }
        AnimatedContent(
            targetState = uiState.page,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
            label = "onboardingPage",
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingPageContent(page = page, uiState = uiState, onEvent = onEvent)
            }
        }
        OnboardingButtons(uiState = uiState, onEvent = onEvent)
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, uiState: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    when (page) {
        OnboardingPage.WELCOME -> {
            PageHeader(Icons.Default.FitnessCenter, R.string.onboarding_welcome_title, R.string.onboarding_welcome_text)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onEvent(OnboardingUiEvent.OnNameChange(it)) },
                label = { Text(stringResource(R.string.onboarding_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OnboardingPage.UNIT -> {
            PageHeader(Icons.Default.MonitorWeight, R.string.onboarding_unit_title, R.string.onboarding_unit_text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                HdChip(
                    text = stringResource(R.string.profile_unit_kg),
                    isSelected = uiState.unit == WeightUnit.KG,
                    onClick = { onEvent(OnboardingUiEvent.OnUnitChange(WeightUnit.KG)) }
                )
                HdChip(
                    text = stringResource(R.string.profile_unit_lb),
                    isSelected = uiState.unit == WeightUnit.LB,
                    onClick = { onEvent(OnboardingUiEvent.OnUnitChange(WeightUnit.LB)) }
                )
            }
        }

        OnboardingPage.REMINDER -> {
            PageHeader(Icons.Default.NotificationsActive, R.string.onboarding_reminder_title, R.string.onboarding_reminder_text)
            TrainingReminderCard(
                reminder = uiState.reminder,
                onEnabledChange = { onEvent(OnboardingUiEvent.OnReminderEnabledChange(it)) },
                onTimeChange = { hour, minute -> onEvent(OnboardingUiEvent.OnReminderTimeChange(hour, minute)) }
            )
        }

        OnboardingPage.READY -> PageHeader(Icons.Default.CheckCircle, R.string.onboarding_ready_title, R.string.onboarding_ready_text)
    }
}

@Composable
private fun PageHeader(icon: ImageVector, title: Int, text: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Back / Next, or on the last page the two ways in. */
@Composable
private fun OnboardingButtons(uiState: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    if (uiState.page == OnboardingPage.entries.last()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HdButton(
                text = stringResource(R.string.onboarding_create_routine),
                onClick = { onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = true)) },
                enabled = !uiState.isFinishing,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = { onEvent(OnboardingUiEvent.OnFinish(createFirstRoutine = false)) },
                enabled = !uiState.isFinishing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.onboarding_start)) }
        }
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (uiState.page != OnboardingPage.entries.first()) {
            TextButton(onClick = { onEvent(OnboardingUiEvent.OnBack) }) { Text(stringResource(R.string.action_back)) }
        }
        Spacer(modifier = Modifier.weight(1f))
        HdButton(text = stringResource(R.string.onboarding_next), onClick = { onEvent(OnboardingUiEvent.OnNext) })
    }
}

@Composable
private fun PageDots(current: OnboardingPage, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OnboardingPage.entries.forEach { page ->
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (page == current) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (page ==
                            current
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
            )
        }
    }
}
