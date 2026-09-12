package com.example.heavyduty.core.ui

import androidx.annotation.StringRes
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.WorkoutType

@Suppress("unused")
val MuscleGroup.nameResId: Int
    @StringRes get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.FOREARMS -> R.string.muscle_forearms
        MuscleGroup.QUADRICEPS -> R.string.muscle_quadriceps
        MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
        MuscleGroup.ABS -> R.string.muscle_abs
        MuscleGroup.OBLIQUES -> R.string.muscle_obliques
        MuscleGroup.TRAPS -> R.string.muscle_traps
        MuscleGroup.LATS -> R.string.muscle_lats
        MuscleGroup.FULL_BODY -> R.string.muscle_full_body
        MuscleGroup.CARDIO -> R.string.muscle_cardio
    }

@Suppress("unused")
val ExerciseType.nameResId: Int
    @StringRes get() = when (this) {
        ExerciseType.STRENGTH -> R.string.exercise_type_strength
        ExerciseType.CARDIO -> R.string.exercise_type_cardio
        ExerciseType.FUNCTIONAL -> R.string.exercise_type_functional
    }

@Suppress("unused")
val WorkoutType.nameResId: Int
    @StringRes get() = when (this) {
        WorkoutType.GYM -> R.string.workout_type_gym
        WorkoutType.CROSSFIT -> R.string.workout_type_crossfit
        WorkoutType.CARDIO -> R.string.workout_type_cardio
        WorkoutType.MIXED -> R.string.workout_type_mixed
    }

@Suppress("unused")
val ProgressionPolicy.nameResId: Int
    @StringRes get() = when (this) {
        ProgressionPolicy.OFF -> R.string.progression_off
        ProgressionPolicy.LINEAR -> R.string.progression_linear
        ProgressionPolicy.GREYSKULL_LP -> R.string.progression_greyskull
        ProgressionPolicy.DOUBLE_PROGRESSION -> R.string.progression_double
        ProgressionPolicy.TIME -> R.string.progression_time
    }
