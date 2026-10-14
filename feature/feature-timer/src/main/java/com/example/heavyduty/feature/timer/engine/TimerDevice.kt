package com.example.heavyduty.feature.timer.engine

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
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

    /**
     * A buzz as an alarm, like a kitchen timer's: without a usage Android files it as touch
     * feedback, which follows the touch-vibration setting — turned down (as on the user's
     * phone) or off, the end of a rest went by unnoticed.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return@runCatching
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    vibrator.vibrate(oneShot(), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrateAsAlarmBefore13()

                else -> vibrator.vibrateAsAlarmBefore8()
            }
        }
    }
}

private const val VIBRATION_MILLIS = 300L

@RequiresApi(Build.VERSION_CODES.O)
private fun oneShot(): VibrationEffect = VibrationEffect.createOneShot(VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)

/** Before Android 13 a vibration's usage comes from audio attributes. */
private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

@RequiresApi(Build.VERSION_CODES.O)
@Suppress("DEPRECATION")
private fun Vibrator.vibrateAsAlarmBefore13() = vibrate(oneShot(), alarmAudioAttributes())

@Suppress("DEPRECATION")
private fun Vibrator.vibrateAsAlarmBefore8() = vibrate(VIBRATION_MILLIS, alarmAudioAttributes())
