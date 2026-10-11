package com.example.heavyduty.feature.exercises.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.heavyduty.core.domain.model.MuscleGroup

/**
 * Maps each muscle group to one of the app's three accent colors so exercise
 * cards and detail sections read as a harmonious, color-coded system — taken from the active
 * theme, so they stay legible in light mode too.
 */
@Composable
@ReadOnlyComposable
fun MuscleGroup.accentColor(): Color = when (this) {
    MuscleGroup.CHEST,
    MuscleGroup.BACK,
    MuscleGroup.SHOULDERS,
    MuscleGroup.LATS,
    MuscleGroup.TRAPS,
    MuscleGroup.FULL_BODY
    -> MaterialTheme.colorScheme.primary

    MuscleGroup.BICEPS,
    MuscleGroup.TRICEPS,
    MuscleGroup.FOREARMS,
    MuscleGroup.ABS,
    MuscleGroup.OBLIQUES,
    MuscleGroup.CARDIO
    -> MaterialTheme.colorScheme.secondary

    MuscleGroup.QUADRICEPS,
    MuscleGroup.HAMSTRINGS,
    MuscleGroup.GLUTES,
    MuscleGroup.CALVES
    -> MaterialTheme.colorScheme.tertiary
}
