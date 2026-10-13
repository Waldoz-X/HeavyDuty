package com.example.heavyduty.feature.timer.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.heavyduty.feature.timer.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * What [TimerEngine] asks of the phone: [TimerService] in the foreground while a timer runs,
 * so the process survives in the background, and a buzz when a round or the timer ends.
 * Each is best effort: a timer keeps counting even if the system refuses one of them.
 */
class TimerDevice @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun startService() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }
    }

    fun stopService() {
        runCatching {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_MILLIS)
            }
        }
    }
}

private const val VIBRATION_MILLIS = 300L
