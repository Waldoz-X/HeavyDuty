package com.example.heavyduty.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.nextReminderAt
import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One inexact alarm for the next reminder; [TrainingReminderReceiver] sets the following one
 * each time it fires. Inexact on purpose: a reminder a few minutes late is fine, and it needs
 * neither the exact-alarm permission nor waking the phone at an exact second.
 */
@Singleton
class AlarmTrainingReminderScheduler @Inject constructor(@param:ApplicationContext private val context: Context, private val clock: Clock) :
    TrainingReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(settings: ReminderSettings) {
        val alarm = alarmIntent()
        alarmManager.cancel(alarm)
        if (!settings.enabled) return
        val at = nextReminderAt(clock.nowMillis(), settings.hour, settings.minute)
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, alarm)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TrainingReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        /** How late the system may deliver it, to batch it with other alarms. */
        val WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10)
    }
}
