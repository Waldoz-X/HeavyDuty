package com.example.heavyduty.feature.exercises.presentation.detail

import com.example.heavyduty.core.common.R
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.heavyduty.core.ui.hdSharedBounds
import com.example.heavyduty.feature.exercises.presentation.exerciseImageKey
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.ui.equipmentLabel
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdSkeletonLine
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.feature.exercises.presentation.accentColor

private val HeaderHeight = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onViewProgress: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ExerciseDetailUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        when {
            uiState.isLoading -> {
                ExerciseDetailLoading(
                    exerciseId = uiState.exerciseId,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                )
            }
            uiState.exercise != null -> {
                ExerciseDetailContent(
                    exercise = uiState.exercise!!,
                    onNavigateBack = onNavigateBack,
                    onViewProgress = onViewProgress,
                    onWeightUnitChange = { viewModel.onEvent(ExerciseDetailUiEvent.OnWeightUnitChange(it)) },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.exercise_load_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * The header's frame while the exercise loads, in the same place and with the same shared key
 * as the loaded image: the list's thumbnail grows into it from the first frame instead of the
 * transition finding nothing to match and falling back to a plain slide.
 */
@Composable
private fun ExerciseDetailLoading(exerciseId: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeaderHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(208.dp)
                    .hdSharedBounds(exerciseImageKey(exerciseId), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
    modifier: Modifier = Modifier,
) {
    val accent = exercise.muscleGroup.accentColor()
    val scrollState = rememberScrollState()
    val heroImage = exercise.gifUrl.ifBlank { exercise.imageUrl }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .clipToBounds(),
            ) {
                if (heroImage.isNotBlank()) {
                    // The source media is a small 180x180 thumbnail — stretching it edge-to-edge
                    // across a full-width, 320dp-tall header would upscale it ~2-3x and look
                    // visibly pixelated. Instead: a heavily blurred copy fills the backdrop (blur
                    // hides the low resolution) while a sharp copy sits centered at a size close
                    // to its native resolution, framed like an album cover.
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
                            .blur(36.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 12.dp)
                            .size(208.dp)
                            .hdSharedBounds(exerciseImageKey(exercise.id), RoundedCornerShape(20.dp))
                            .graphicsLayer {
                                translationY = scrollState.value * 0.5f
                            }
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                            contentDescription = exercise.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(alpha = 0.4f), accent.copy(alpha = 0.1f)),
                                ),
                            ),
                    )
                }

                // Scrim so the title stays legible over any image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                                ),
                            ),
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    if (exercise.equipment.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = equipmentLabel(exercise.equipment),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    AccentChip(text = stringResource(exercise.muscleGroup.nameResId), color = accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    AccentChip(
                        text = stringResource(exercise.exerciseType.nameResId),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = stringResource(R.string.exercise_detail_weight_unit), accent = accent)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HdChip(
                        text = "kg",
                        isSelected = exercise.weightUnit == WeightUnit.KG,
                        onClick = { onWeightUnitChange(WeightUnit.KG) },
                    )
                    HdChip(
                        text = "lb",
                        isSelected = exercise.weightUnit == WeightUnit.LB,
                        onClick = { onWeightUnitChange(WeightUnit.LB) },
                    )
                }

                val secondaryMuscles = exercise.muscleWeights.keys.filter { it != exercise.muscleGroup }
                if (secondaryMuscles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val secondaryNames = secondaryMuscles.map { stringResource(it.nameResId) }.joinToString(", ")
                    Text(
                        text = stringResource(R.string.exercise_detail_also_works, secondaryNames),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HdTonalButton(
                    text = stringResource(R.string.exercise_detail_view_progress),
                    onClick = { onViewProgress(exercise.id) },
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (exercise.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = stringResource(R.string.exercise_detail_summary), accent = accent)
                    Spacer(modifier = Modifier.height(8.dp))
                    HdCard {
                        Text(
                            text = exercise.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val steps = exercise.instructions
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (steps.isNotEmpty()) {
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AccentChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
