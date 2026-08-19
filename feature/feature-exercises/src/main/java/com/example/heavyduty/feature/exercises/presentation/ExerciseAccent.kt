package com.example.heavyduty.feature.exercises.presentation

import androidx.compose.ui.graphics.Color
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.theme.PrimaryCyan
import com.example.heavyduty.core.ui.theme.SecondaryMagenta
import com.example.heavyduty.core.ui.theme.TertiaryLime

/**
 * Maps each muscle group to one of the app's three accent colors so exercise
 * cards and detail sections read as a harmonious, color-coded system.
 */
fun MuscleGroup.accentColor(): Color = when (this) {
    MuscleGroup.CHEST,
    MuscleGroup.BACK,
    MuscleGroup.SHOULDERS,
    MuscleGroup.LATS,
    MuscleGroup.TRAPS,
    MuscleGroup.FULL_BODY,
    -> PrimaryCyan

    MuscleGroup.BICEPS,
    MuscleGroup.TRICEPS,
    MuscleGroup.FOREARMS,
    MuscleGroup.ABS,
    MuscleGroup.OBLIQUES,
    MuscleGroup.CARDIO,
    -> SecondaryMagenta

    MuscleGroup.QUADRICEPS,
    MuscleGroup.HAMSTRINGS,
    MuscleGroup.GLUTES,
    MuscleGroup.CALVES,
    -> TertiaryLime
}
