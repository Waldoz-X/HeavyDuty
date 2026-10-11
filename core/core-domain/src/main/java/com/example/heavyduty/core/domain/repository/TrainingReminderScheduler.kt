package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.ReminderSettings

/**
 * Keeps the system alarm for the daily training reminder in line with [ReminderSettings]:
 * (re)set to the next occurrence when enabled, cleared when not.
 */
fun interface TrainingReminderScheduler {
    fun schedule(settings: ReminderSettings)
}
