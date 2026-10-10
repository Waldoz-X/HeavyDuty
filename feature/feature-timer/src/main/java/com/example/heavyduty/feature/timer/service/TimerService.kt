package com.example.heavyduty.feature.timer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.heavyduty.feature.timer.engine.TimerEngine
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the process alive with foreground priority while a timer is running/paused and mirrors
 * [TimerEngine]'s state into an ongoing notification (see [TimerNotifications]). Holds no
 * timing logic itself — see [TimerEngine] for why.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerEngine: TimerEngine

    @Inject
    lateinit var notifications: TimerNotifications

    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> timerEngine.onEvent(TimerUiEvent.OnStartPause)
            ACTION_STOP -> timerEngine.onEvent(TimerUiEvent.OnReset)
            ACTION_ADD_REST -> timerEngine.adjustRest(REST_STEP_SECONDS)
            ACTION_SKIP_REST -> timerEngine.skipRest()
        }

        ServiceCompat.startForeground(
            this,
            TimerNotifications.ONGOING_NOTIFICATION_ID,
            notifications.ongoing(TimerNotifications.OngoingContent.of(timerEngine.state.value, System.currentTimeMillis())),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
        observeState()
        return START_NOT_STICKY
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun observeState() {
        if (serviceScope != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        serviceScope = scope
        scope.launch {
            timerEngine.state
                .map { state ->
                    val active = state.isRunning || state.isPaused
                    if (active) TimerNotifications.OngoingContent.of(state, System.currentTimeMillis()) else null
                }
                // The chronometer ticks by itself, so a plain second passing changes nothing here.
                .distinctUntilChanged { old, new -> old != null && new != null && old.showsSameAs(new) }
                .collect { content ->
                    if (content == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        NotificationManagerCompat.from(this@TimerService)
                            .notify(TimerNotifications.ONGOING_NOTIFICATION_ID, notifications.ongoing(content))
                    }
                }
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    internal companion object {
        const val ACTION_TOGGLE = "com.example.heavyduty.feature.timer.TOGGLE"
        const val ACTION_STOP = "com.example.heavyduty.feature.timer.STOP"
        const val ACTION_ADD_REST = "com.example.heavyduty.feature.timer.ADD_REST"
        const val ACTION_SKIP_REST = "com.example.heavyduty.feature.timer.SKIP_REST"
        const val REST_STEP_SECONDS = 15
    }
}
