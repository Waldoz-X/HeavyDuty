package com.example.heavyduty.core.domain.model

/**
 * Domain model representing a routine scheduled on a specific day of the week.
 */
data class CalendarEntry(
    val id: Long = 0,
    val routineId: Long = 0,
    val routine: Routine? = null,
    val dayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday
    val recurrence: Recurrence = Recurrence.WEEKLY,
    val isActive: Boolean = true
)
