package com.example.heavyduty.feature.timer.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.engine.TimerEngine
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import com.example.heavyduty.feature.timer.presentation.TimerUiState
import com.example.heavyduty.feature.timer.presentation.nameResId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the process alive with foreground priority while a timer is running/paused and mirrors
 * [TimerEngine]'s state into an ongoing notification. Holds no timing logic itself — see
 * [TimerEngine] for why.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerEngine: TimerEngine

    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> timerEngine.onEvent(TimerUiEvent.OnStartPause)
            ACTION_STOP -> timerEngine.onEvent(TimerUiEvent.OnReset)
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(timerEngine.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
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
            timerEngine.state.collect { state ->
                if (!state.isRunning && !state.isPaused) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    NotificationManagerCompat.from(this@TimerService)
                        .notify(NOTIFICATION_ID, buildNotification(state))
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

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Temporizador de entrenamiento")
            .setDescription("Muestra el temporizador de descanso/AMRAP/EMOM/Tabata en curso")
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun buildNotification(state: TimerUiState): android.app.Notification {
        val contentText = when (state.mode) {
            TimerMode.STOPWATCH -> formatTime(state.elapsedSeconds)
            else -> if (state.rounds > 1) {
                "${formatTime(state.remainingSeconds)} · Round ${state.currentRound}/${state.rounds}"
            } else {
                formatTime(state.remainingSeconds)
            }
        }

        val toggleAction = NotificationCompat.Action(
            if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (state.isRunning) "Pausar" else "Reanudar",
            servicePendingIntent(ACTION_TOGGLE),
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Detener",
            servicePendingIntent(ACTION_STOP),
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(state.mode.nameResId))
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(reopenAppPendingIntent())
            .addAction(toggleAction)
            .addAction(stopAction)
            .build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getService(
                this,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }

    private fun reopenAppPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.example.heavyduty.feature.timer.TOGGLE"
        const val ACTION_STOP = "com.example.heavyduty.feature.timer.STOP"
    }
}
