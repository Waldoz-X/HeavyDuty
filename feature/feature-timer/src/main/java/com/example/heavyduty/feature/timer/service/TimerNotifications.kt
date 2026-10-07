package com.example.heavyduty.feature.timer.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.heavyduty.core.common.R
import com.example.heavyduty.feature.timer.R as TimerR
import com.example.heavyduty.core.common.navigation.AppSections
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiState
import com.example.heavyduty.feature.timer.presentation.nameResId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Builds the timer's notifications: the ongoing one shown while a timer runs (see
 * [TimerService]) and the "rest's over" alert.
 *
 * The ongoing notification uses the system chronometer, which ticks on its own from a fixed
 * end (or start) time — so it's only re-posted when something actually changes (pause, +15 s,
 * next round), not once a second.
 */
@Singleton
class TimerNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(ONGOING_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.timer_channel_name))
                    .setDescription(context.getString(R.string.timer_channel_description))
                    .setShowBadge(false)
                    .build(),
                NotificationChannelCompat.Builder(REST_FINISHED_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.rest_finished_channel))
                    .setDescription(context.getString(R.string.rest_finished_channel_description))
                    .setVibrationEnabled(true)
                    .setShowBadge(false)
                    .build(),
            ),
        )
    }

    fun ongoing(content: OngoingContent): Notification {
        val isRest = content.mode == TimerMode.REST
        val title = context.getString(content.mode.nameResId) +
            if (content.rounds > 1) " · " + context.getString(R.string.timer_notification_round, content.round, content.rounds) else ""
        val text = when {
            !content.isRunning -> context.getString(R.string.rest_notification_paused, formatTime(content.frozenSeconds))
            isRest -> context.getString(R.string.rest_notification_text)
            else -> null
        }
        val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(TimerR.drawable.ic_stat_workout)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setContentIntent(openApp(if (isRest) AppSections.SECTION_WORKOUT else AppSections.SECTION_TIMER))

        if (content.isRunning) {
            builder.setShowWhen(true)
                .setWhen(content.anchorMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(content.mode != TimerMode.STOPWATCH)
        } else {
            builder.setShowWhen(false)
        }

        if (isRest) {
            builder.addAction(0, context.getString(R.string.rest_notification_add), serviceAction(TimerService.ACTION_ADD_REST))
            builder.addAction(0, context.getString(R.string.rest_notification_skip), serviceAction(TimerService.ACTION_SKIP_REST))
        } else {
            builder.addAction(
                if (content.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                context.getString(if (content.isRunning) R.string.timer_pause else R.string.timer_resume),
                serviceAction(TimerService.ACTION_TOGGLE),
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.timer_stop), serviceAction(TimerService.ACTION_STOP))
        }
        return builder.build()
    }

    /**
     * "Rest's over" — only while the app isn't on screen (pocket, other app, screen off); on
     * screen the workout's own banner already says so.
     */
    @SuppressLint("MissingPermission")
    fun notifyRestFinished() {
        if (isAppInForeground() || !manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, REST_FINISHED_CHANNEL_ID)
            .setSmallIcon(TimerR.drawable.ic_stat_workout)
            .setColor(ACCENT_COLOR)
            .setContentTitle(context.getString(R.string.rest_finished_title))
            .setContentText(context.getString(R.string.rest_finished_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(REST_FINISHED_TIMEOUT_MS)
            .setContentIntent(openApp(AppSections.SECTION_WORKOUT))
            .build()
        runCatching { manager.notify(REST_FINISHED_NOTIFICATION_ID, notification) }
    }

    /** A new rest makes the previous "rest's over" alert stale. */
    fun cancelRestFinished() {
        manager.cancel(REST_FINISHED_NOTIFICATION_ID)
    }

    private fun isAppInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    /** Brings the app's existing screen forward (SINGLE_TOP) and asks it to open [section]. */
    private fun openApp(section: String): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AppSections.EXTRA_OPEN_SECTION, section)
        return PendingIntent.getActivity(
            context,
            section.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun serviceAction(action: String): PendingIntent {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, action.hashCode(), intent, flags)
        } else {
            PendingIntent.getService(context, action.hashCode(), intent, flags)
        }
    }

    private fun formatTime(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    /**
     * What the ongoing notification shows. While running, [anchorMillis] is when the countdown
     * hits zero (or, for the stopwatch, when it started) and the system chronometer counts from
     * it; while paused, [frozenSeconds] is the time on display.
     */
    data class OngoingContent(
        val mode: TimerMode,
        val isRunning: Boolean,
        val round: Int,
        val rounds: Int,
        val anchorMillis: Long,
        val frozenSeconds: Int,
    ) {
        /**
         * Same notification as [other]: equal apart from anchor jitter. The engine ticks about
         * once a second, so the computed end time wobbles by some milliseconds between ticks.
         */
        fun showsSameAs(other: OngoingContent): Boolean =
            copy(anchorMillis = 0) == other.copy(anchorMillis = 0) &&
                abs(anchorMillis - other.anchorMillis) < ANCHOR_TOLERANCE_MS

        companion object {
            fun of(state: TimerUiState, nowMillis: Long): OngoingContent {
                val isStopwatch = state.mode == TimerMode.STOPWATCH
                val shownSeconds = if (isStopwatch) state.elapsedSeconds else state.remainingSeconds
                return OngoingContent(
                    mode = state.mode,
                    isRunning = state.isRunning,
                    round = state.currentRound,
                    rounds = state.rounds,
                    anchorMillis = when {
                        !state.isRunning -> 0L
                        isStopwatch -> nowMillis - shownSeconds * 1_000L
                        else -> nowMillis + shownSeconds * 1_000L
                    },
                    frozenSeconds = if (state.isRunning) 0 else shownSeconds,
                )
            }
        }
    }

    companion object {
        const val ONGOING_NOTIFICATION_ID = 1001
        private const val REST_FINISHED_NOTIFICATION_ID = 1002
        private const val ONGOING_CHANNEL_ID = "timer_channel"
        private const val REST_FINISHED_CHANNEL_ID = "rest_finished_channel"
        private const val REST_FINISHED_TIMEOUT_MS = 2 * 60 * 1_000L
        private const val ANCHOR_TOLERANCE_MS = 1_500L

        /** The app's primary cyan (core-ui PrimaryCyan), used to tint the icon and actions. */
        private const val ACCENT_COLOR = 0xFF00E5FF.toInt()
    }
}
