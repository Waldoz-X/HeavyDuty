package com.example.heavyduty.feature.exercises.presentation.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdSearchBar

@Composable
fun ExerciseListScreen(
    onExerciseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ExerciseListUiEffect.NavigateToDetail -> onExerciseClick(effect.exerciseId)
                is ExerciseListUiEffect.ShowError -> { /* Could show snackbar */ }
            }
        }
    }

    ExerciseListContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
private fun ExerciseListContent(
    uiState: ExerciseListUiState,
    onEvent: (ExerciseListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Exercises",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HdSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { onEvent(ExerciseListUiEvent.OnSearchQueryChange(it)) },
            placeholder = "Search exercises...",
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Muscle group filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(MuscleGroup.entries.toList()) { group ->
                HdChip(
                    text = group.displayName,
                    isSelected = uiState.selectedFilter == group,
                    onClick = { onEvent(ExerciseListUiEvent.OnFilterSelect(group)) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> {
                HdLoadingIndicator(
                    message = "Loading exercises...",
                    modifier = Modifier.weight(1f)
                )
            }
            uiState.exercises.isEmpty() -> {
                HdEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = "No exercises found",
                    description = if (uiState.searchQuery.isNotBlank()) {
                        "Try a different search term"
                    } else {
                        "Exercises will appear here"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(
                            items = uiState.exercises,
                            key = { it.id },
                        ) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onEvent(ExerciseListUiEvent.OnExerciseClick(exercise.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (exercise.imageUrl.isNotBlank()) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp)
                ) {
                    AsyncImage(
                        model = exercise.imageUrl,
                        contentDescription = exercise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    HdChip(
                        text = exercise.muscleGroup.displayName,
                        isSelected = true,
                        onClick = {}
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exercise.equipment,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
