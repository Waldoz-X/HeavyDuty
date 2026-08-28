package com.example.heavyduty.feature.dashboard.domain.model

import java.time.LocalDate

data class DayPlanUi(
    val date: LocalDate,
    val dayLetter: String,
    val dayNumber: Int,
    val routineId: Long?,
    val routineName: String?,
    val isToday: Boolean,
    val status: DayStatus,
)

enum class DayStatus { COMPLETED, MISSED, REST, UPCOMING, TODAY_PENDING }
