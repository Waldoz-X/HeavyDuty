package com.example.heavyduty.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.navigation.AppSections
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.feature.timer.R as TimerR
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The "Hoy toca: Pierna" notification; tapping it opens the Train tab. */
@Singleton
class TrainingReminderNotifications @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    // Checked just before posting; without the permission the reminder simply isn't shown.
    @SuppressLint("MissingPermission")
    fun show(routines: List<Routine>) {
        if (!canPost()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(TimerR.drawable.ic_stat_workout)
            .setContentTitle(context.getString(R.string.reminder_title, routines.joinToString(", ") { it.name }))
            .setContentText(context.getString(R.string.reminder_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openTrainTab())
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun canPost(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openTrainTab(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AppSections.EXTRA_OPEN_SECTION, AppSections.SECTION_WORKOUT)
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private companion object {
        const val CHANNEL_ID = "training_reminder"
        const val NOTIFICATION_ID = 4_200
    }
}
