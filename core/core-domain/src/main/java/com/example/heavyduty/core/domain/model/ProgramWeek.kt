package com.example.heavyduty.core.domain.model

data class ProgramWeek(
    val id: Long = 0,
    val programId: Long = 0,
    val weekNumber: Int = 1,
    val notes: String = "",
    val days: List<Routine> = emptyList(),
)
