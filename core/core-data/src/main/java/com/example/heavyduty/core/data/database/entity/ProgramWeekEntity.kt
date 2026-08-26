package com.example.heavyduty.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_weeks",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["programId"])],
)
data class ProgramWeekEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: Long,
    val weekNumber: Int,
    val notes: String = "",
)
