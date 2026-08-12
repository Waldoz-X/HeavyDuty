package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val imageUrl: String = "",
    val muscleGroup: String,
    val exerciseType: String,
    val equipment: String = "",
    val isCustom: Boolean = false,
)
