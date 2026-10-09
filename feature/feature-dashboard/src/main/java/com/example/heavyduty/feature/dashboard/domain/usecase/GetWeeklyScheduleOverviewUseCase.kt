package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.dashboard.domain.model.DayPlanUi
import com.example.heavyduty.feature.dashboard.domain.model.DayStatus
import com.example.heavyduty.feature.dashboard.domain.model.PlannedRoutineUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * The current (or offset) calendar week's day-by-day plan: which routines — a day can have
 * more than one [CalendarEntry][com.example.heavyduty.core.domain.model.CalendarEntry] (e.g.
 * an AM and a PM session) — are scheduled per that weekly template (Monday=1..Sunday=7), and
 * whether each one was completed.
 */
class GetWeeklyScheduleOverviewUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock,
) {
    operator fun invoke(weekOffset: Int = 0): Flow<List<DayPlanUi>> =
        combine(
            calendarRepository.getWeekSchedule(),
            routineRepository.getAllRoutines(),
            workoutRepository.getAllSessions(),
        ) { schedule, routines, sessions ->
            val routineById = routines.associateBy { it.id }
            // Read on every update, so the plan rolls over at midnight.
            val todayDate = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
            val monday = todayDate.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())

            (1..7).map { day ->
                val date = monday.plusDays((day - 1).toLong())

                val dayStartMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dayEndMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val completedSessionsToday = sessions.filter {
                    it.status == WorkoutStatus.COMPLETED && it.startTime in dayStartMillis until dayEndMillis
                }

                val plannedRoutines = schedule[day].orEmpty().mapNotNull { entry ->
                    val routine = routineById[entry.routineId] ?: return@mapNotNull null
                    PlannedRoutineUi(
                        routineId = routine.id,
                        routineName = routine.name,
                        isCompleted = completedSessionsToday.any { it.routineId == routine.id },
                    )
                }

                val status = when {
                    plannedRoutines.isEmpty() && completedSessionsToday.isEmpty() -> DayStatus.REST
                    plannedRoutines.isEmpty() -> DayStatus.COMPLETED // trained anyway on a rest day
                    plannedRoutines.all { it.isCompleted } -> DayStatus.COMPLETED
                    date.isBefore(todayDate) -> DayStatus.MISSED
                    date.isEqual(todayDate) -> DayStatus.TODAY_PENDING
                    else -> DayStatus.UPCOMING
                }

                DayPlanUi(
                    date = date,
                    dayLetter = dayLetter(date.dayOfWeek),
                    dayNumber = date.dayOfMonth,
                    routines = plannedRoutines,
                    isToday = date.isEqual(todayDate),
                    status = status,
                )
            }
        }

    /** Two-letter weekday in the device language ("Lu", "Ma"… / "Mo", "Tu"…). */
    private fun dayLetter(dayOfWeek: DayOfWeek): String {
        val locale = Locale.getDefault()
        return dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .take(2)
            .replaceFirstChar { it.titlecase(locale) }
    }
}
