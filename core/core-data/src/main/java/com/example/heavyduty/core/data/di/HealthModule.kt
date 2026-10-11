package com.example.heavyduty.core.data.di

import com.example.heavyduty.core.data.health.DataStoreHealthConnectSettings
import com.example.heavyduty.core.data.health.HealthConnectClientSource
import com.example.heavyduty.core.data.health.HealthConnectSettings
import com.example.heavyduty.core.data.health.HealthConnectSource
import com.example.heavyduty.core.data.health.HealthSyncImpl
import com.example.heavyduty.core.domain.repository.HealthSync
import com.example.heavyduty.core.domain.repository.WorkoutHealthMirror
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
internal abstract class HealthModule {

    @Binds
    abstract fun bindHealthSync(impl: HealthSyncImpl): HealthSync

    @Binds
    abstract fun bindWorkoutHealthMirror(impl: HealthSyncImpl): WorkoutHealthMirror

    @Binds
    abstract fun bindHealthConnectSource(impl: HealthConnectClientSource): HealthConnectSource

    @Binds
    abstract fun bindHealthConnectSettings(impl: DataStoreHealthConnectSettings): HealthConnectSettings
}
