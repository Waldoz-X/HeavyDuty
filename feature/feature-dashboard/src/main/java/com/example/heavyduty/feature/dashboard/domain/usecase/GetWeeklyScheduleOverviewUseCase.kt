package com.example.heavyduty.feature.dashboard.domain.usecase

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
import java.time.LocalDate
import java.time.ZoneId
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
) {
    operator fun invoke(weekOffset: Int = 0): Flow<List<DayPlanUi>> =
        combine(
            calendarRepository.getWeekSchedule(),
            routineRepository.getAllRoutines(),
            workoutRepository.getAllSessions(),
        ) { schedule, routines, sessions ->
            val routineById = routines.associateBy { it.id }
            val monday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
            val today = LocalDate.now()

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
                    date.isBefore(today) -> DayStatus.MISSED
                    date.isEqual(today) -> DayStatus.TODAY_PENDING
                    else -> DayStatus.UPCOMING
                }

                DayPlanUi(
                    date = date,
                    dayLetter = DAY_LETTERS.getValue(day),
                    dayNumber = date.dayOfMonth,
                    routines = plannedRoutines,
                    isToday = date.isEqual(today),
                    status = status,
                )
            }
        }

    private companion object {
        val DAY_LETTERS = mapOf(1 to "Lu", 2 to "Ma", 3 to "Mi", 4 to "Ju", 5 to "Vi", 6 to "Sá", 7 to "Do")
    }
}
