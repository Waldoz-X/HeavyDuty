package com.example.heavyduty.feature.exercises.presentation.detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.displayInstructions
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdSkeletonLine
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.equipmentLabel
import com.example.heavyduty.core.ui.hdSharedBounds
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.feature.exercises.presentation.accentColor
import com.example.heavyduty.feature.exercises.presentation.exerciseImageKey

private val HeaderHeight = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onViewProgress: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ExerciseDetailUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        // The header runs under the status bar, full-bleed; only what's on it keeps clear of it.
        val statusBar = innerPadding.calculateTopPadding()
        when {
            uiState.isLoading -> {
                ExerciseDetailLoading(exerciseId = uiState.exerciseId, statusBar = statusBar)
            }

            uiState.exercise != null -> {
                ExerciseDetailContent(
                    exercise = uiState.exercise!!,
                    onNavigateBack = onNavigateBack,
                    onViewProgress = onViewProgress,
                    onWeightUnitChange = { viewModel.onEvent(ExerciseDetailUiEvent.OnWeightUnitChange(it)) },
                    statusBar = statusBar
                )
            }

            // Couldn't be read — or no longer exists.
            else -> HdErrorState(modifier = Modifier.padding(innerPadding))
        }
    }
}

/**
 * The header's frame while the exercise loads, in the same place and with the same shared key
 * as the loaded image: the list's thumbnail grows into it from the first frame instead of the
 * transition finding nothing to match and falling back to a plain slide.
 */
@Composable
private fun ExerciseDetailLoading(exerciseId: Long, statusBar: Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeaderHeight + statusBar),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp + statusBar)
                    .size(208.dp)
                    .hdSharedBounds(exerciseImageKey(exerciseId), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HdSkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), height = 28.dp)
            HdSkeletonLine(modifier = Modifier.fillMaxWidth(0.35f), height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            repeat(3) { HdSkeletonLine(modifier = Modifier.fillMaxWidth(), height = 14.dp) }
        }
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    onNavigateBack: () -> Unit,
    onViewProgress: (Long) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit,
    statusBar: Dp,
    modifier: Modifier = Modifier
) {
    val accent = exercise.muscleGroup.accentColor()
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            ExerciseHeader(
                exercise = exercise,
                accent = accent,
                scrollState = scrollState,
                statusBar = statusBar,
                onNavigateBack = onNavigateBack
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                ExerciseBasics(exercise = exercise, accent = accent, onWeightUnitChange = onWeightUnitChange)
                Spacer(modifier = Modifier.height(16.dp))
                HdTonalButton(
                    text = stringResource(R.string.exercise_detail_view_progress),
                    onClick = { onViewProgress(exercise.id) },
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    modifier = Modifier.fillMaxWidth()
                )
                SummarySection(exercise = exercise, accent = accent)
                InstructionsSection(instructions = exercise.displayInstructions, accent = accent)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/** The full-bleed header: the exercise's image (or its muscle's colour), a scrim, back, and the title. */
@Composable
private fun ExerciseHeader(
    exercise: Exercise,
    accent: Color,
    scrollState: ScrollState,
    statusBar: Dp,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heroImage = exercise.gifUrl.ifBlank { exercise.imageUrl }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight + statusBar)
            .clipToBounds()
    ) {
        if (heroImage.isNotBlank()) {
            HeroImage(heroImage = heroImage, exercise = exercise, scrollState = scrollState, statusBar = statusBar)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.4f), accent.copy(alpha = 0.1f))))
            )
        }

        // Scrim so the title stays legible over any image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            // Darker at the very top: the status bar's light icons sit on it.
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        HeaderBackButton(onClick = onNavigateBack, modifier = Modifier.padding(top = statusBar))
        HeaderTitle(exercise = exercise, modifier = Modifier.align(Alignment.BottomStart))
    }
}

/** Back, on a dark disc so it reads over any image. */
@Composable
private fun HeaderBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun HeaderTitle(exercise: Exercise, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(text = exercise.displayName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        if (exercise.equipment.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = equipmentLabel(exercise.equipment),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * The source media is a small 180x180 thumbnail — stretching it edge-to-edge across a
 * full-width, 320dp-tall header would upscale it ~2-3x and look visibly pixelated. Instead: a
 * heavily blurred copy fills the backdrop (blur hides the low resolution) while a sharp copy sits
 * centered at a size close to its native resolution, framed like an album cover.
 */
@Composable
private fun HeroImage(heroImage: String, exercise: Exercise, scrollState: ScrollState, statusBar: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = heroImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = scrollState.value * 0.5f
                    val scale = 1f + (scrollState.value / HeaderHeight.toPx()).coerceIn(0f, 1f) * 0.3f
                    scaleX = scale * 1.15f
                    scaleY = scale * 1.15f
                }
                .blur(36.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 12.dp + statusBar)
                .size(208.dp)
                .hdSharedBounds(exerciseImageKey(exercise.id), RoundedCornerShape(20.dp))
                .graphicsLayer { translationY = scrollState.value * 0.5f }
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            // While the demo loads, show the list's still image (already in memory)
            // so the shared transition doesn't grow an empty box.
            val context = LocalContext.current
            val heroRequest = remember(heroImage, exercise.imageUrl) {
                ImageRequest.Builder(context)
                    .data(heroImage)
                    .placeholderMemoryCacheKey(exercise.imageUrl.takeIf { it.isNotBlank() })
                    .build()
            }
            AsyncImage(
                model = heroRequest,
                contentDescription = exercise.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Muscle and type, the weight unit it's logged in, and the other muscles it works. */
@Composable
private fun ExerciseBasics(exercise: Exercise, accent: Color, onWeightUnitChange: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row {
            AccentChip(text = stringResource(exercise.muscleGroup.nameResId), color = accent)
            Spacer(modifier = Modifier.width(8.dp))
            AccentChip(text = stringResource(exercise.exerciseType.nameResId), color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(title = stringResource(R.string.exercise_detail_weight_unit), accent = accent)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HdChip(text = "kg", isSelected = exercise.weightUnit == WeightUnit.KG, onClick = { onWeightUnitChange(WeightUnit.KG) })
            HdChip(text = "lb", isSelected = exercise.weightUnit == WeightUnit.LB, onClick = { onWeightUnitChange(WeightUnit.LB) })
        }
        val secondaryMuscles = exercise.muscleWeights.keys.filter { it != exercise.muscleGroup }
        if (secondaryMuscles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val secondaryNames = secondaryMuscles.map { stringResource(it.nameResId) }.joinToString(", ")
            Text(
                text = stringResource(R.string.exercise_detail_also_works, secondaryNames),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummarySection(exercise: Exercise, accent: Color, modifier: Modifier = Modifier) {
    // The catalog's stored summary is Spanish (built at seed time from its more
    // detailed target muscle); in other languages it's rebuilt from the same parts.
    val summary = if (!exercise.isCustom && LocalConfiguration.current.locales[0].language != "es") {
        stringResource(
            R.string.exercise_detail_catalog_summary,
            equipmentLabel(exercise.equipment),
            stringResource(exercise.muscleGroup.nameResId).lowercase()
        )
    } else {
        exercise.description
    }
    if (summary.isBlank()) return
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(title = stringResource(R.string.exercise_detail_summary), accent = accent)
        Spacer(modifier = Modifier.height(8.dp))
        HdCard {
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** One numbered step per line of the instructions. */
@Composable
private fun InstructionsSection(instructions: String, accent: Color, modifier: Modifier = Modifier) {
    val steps = instructions.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (steps.isEmpty()) return
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(title = stringResource(R.string.exercise_detail_instructions), accent = accent)
        Spacer(modifier = Modifier.height(8.dp))
        HdCard {
            steps.forEachIndexed { index, step ->
                if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                InstructionStep(number = index + 1, text = step, accent = accent)
            }
        }
    }
}

@Composable
private fun AccentChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun InstructionStep(number: Int, text: String, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
