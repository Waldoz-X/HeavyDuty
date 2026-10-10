package com.example.heavyduty.feature.dashboard.domain.model

import java.time.LocalDate

data class DayPlanUi(
    val date: LocalDate,
    val dayLetter: String,
    val dayNumber: Int,
    /** Every routine scheduled for this day — a day can have more than one (e.g. AM + PM). */
    val routines: List<PlannedRoutineUi>,
    val isToday: Boolean,
    val status: DayStatus
)

data class PlannedRoutineUi(val routineId: Long, val routineName: String, val isCompleted: Boolean)

enum class DayStatus { COMPLETED, MISSED, REST, UPCOMING, TODAY_PENDING }
