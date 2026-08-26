package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val totalWeeks: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
