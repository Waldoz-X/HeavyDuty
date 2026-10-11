package com.example.heavyduty.reminder

import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    abstract fun bindTrainingReminderScheduler(scheduler: AlarmTrainingReminderScheduler): TrainingReminderScheduler
}
