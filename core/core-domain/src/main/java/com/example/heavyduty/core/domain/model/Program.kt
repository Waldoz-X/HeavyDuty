package com.example.heavyduty.core.domain.model

data class Program(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val totalWeeks: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val weeks: List<ProgramWeek> = emptyList(),
)
