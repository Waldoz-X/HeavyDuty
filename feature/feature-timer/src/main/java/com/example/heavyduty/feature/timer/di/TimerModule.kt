package com.example.heavyduty.feature.timer.di

import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.feature.timer.engine.TimerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {
    @Binds
    abstract fun bindRestTimerController(timerEngine: TimerEngine): RestTimerController
}
