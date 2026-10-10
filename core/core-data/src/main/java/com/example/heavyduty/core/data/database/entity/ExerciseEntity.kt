package com.example.heavyduty.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** The catalog's original English name (search matches it too); empty for custom exercises. */
    @ColumnInfo(defaultValue = "")
    val nameEn: String = "",
    val description: String = "",
    val instructions: String = "",
    /** The catalog's English instructions (one step per line); empty for custom exercises. */
    @ColumnInfo(defaultValue = "")
    val instructionsEn: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    val muscleGroup: String,
    val exerciseType: String,
    val equipment: String = "",
    val isCustom: Boolean = false,
    /** "KG" or "LB" — see Exercise.weightUnit. Weights are still always stored in kg. */
    val weightUnit: String = "KG",
    /** Gson-encoded {"MUSCLE_NAME": weight} map — see Exercise.muscleWeights. Empty = unset. */
    val muscleWeightsJson: String = ""
)
