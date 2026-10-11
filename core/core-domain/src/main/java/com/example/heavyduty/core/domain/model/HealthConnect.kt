package com.example.heavyduty.core.domain.model

/** Whether this phone can use Health Connect at all. */
enum class HealthConnectStatus {
    /** Too old an Android, or not offered on this device. */
    UNSUPPORTED,

    /** Supported, but the Health Connect app has to be installed or updated first. */
    NEEDS_INSTALL,
    AVAILABLE
}

/** What one sync did: workouts sent to Health Connect and weigh-ins brought from it. */
data class HealthSyncResult(val workoutsExported: Int = 0, val weightsImported: Int = 0)
