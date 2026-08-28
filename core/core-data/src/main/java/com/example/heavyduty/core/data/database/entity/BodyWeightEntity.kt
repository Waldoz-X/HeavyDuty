package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight_entries")
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Float,
    val loggedAt: Long,
)
