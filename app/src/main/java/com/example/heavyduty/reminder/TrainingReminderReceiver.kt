package com.example.heavyduty.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.pendingRoutinesOn
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The reminder's alarm went off: if today's calendar has a routine not trained yet (and no
 * workout is under way), say so; then set tomorrow's alarm either way.
 */
@AndroidEntryPoint
class TrainingReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var preferences: UserPreferencesRepository

    @Inject lateinit var calendarRepository: CalendarRepository

    @Inject lateinit var routineRepository: RoutineRepository

    @Inject lateinit var workoutRepository: WorkoutRepository

    @Inject lateinit var scheduler: TrainingReminderScheduler

    @Inject lateinit var notifications: TrainingReminderNotifications

    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = preferences.trainingReminder.first()
                if (settings.enabled && workoutRepository.getActiveSession().first() == null) {
                    val today = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
                    val due = pendingRoutinesOn(
                        date = today,
                        schedule = calendarRepository.getWeekSchedule().first(),
                        routines = routineRepository.getAllRoutines().first(),
                        sessions = workoutRepository.getAllSessions().first()
                    )
                    if (due.isNotEmpty()) notifications.show(due)
                }
                scheduler.schedule(settings)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms don't survive a reboot, and a time or time-zone change moves "18:00": set it again. */
@AndroidEntryPoint
class RescheduleTrainingReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var preferences: UserPreferencesRepository

    @Inject lateinit var scheduler: TrainingReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                scheduler.schedule(preferences.trainingReminder.first())
            } finally {
                pending.finish()
            }
        }
    }
}
