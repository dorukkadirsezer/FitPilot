package com.doruk.fitprogress.core.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerAlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            // Use STREAM_ALARM or STREAM_NOTIFICATION depending on preference.
            // STREAM_MUSIC is often best so it follows media volume (doesn't blast through alarms).
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60) // 60% volume
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun ensureToneGenerator() {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var lastWarningTime = 0L
    private var lastFinishedTime = 0L

    fun playWarningAlert(useSound: Boolean, useVibration: Boolean, style: com.doruk.fitprogress.core.domain.model.TimerSoundStyle = com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP) {
        val now = System.currentTimeMillis()
        if (now - lastWarningTime < 500) return // Debounce
        lastWarningTime = now

        try {
            if (useSound) {
                ensureToneGenerator()
                when (style) {
                    com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    com.doruk.fitprogress.core.domain.model.TimerSoundStyle.CLEAR_TONE -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
            }
            if (useVibration) {
                vibrate(longArrayOf(0, 100))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If it failed, try resetting it for next time
            release()
        }
    }

    fun playFinishedAlert(useSound: Boolean, useVibration: Boolean, style: com.doruk.fitprogress.core.domain.model.TimerSoundStyle = com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP) {
        val now = System.currentTimeMillis()
        if (now - lastFinishedTime < 500) return // Debounce
        lastFinishedTime = now
        
        try {
            if (useSound) {
                ensureToneGenerator()
                when (style) {
                    com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 500)
                    com.doruk.fitprogress.core.domain.model.TimerSoundStyle.CLEAR_TONE -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 500)
                }
            }
            if (useVibration) {
                vibrate(longArrayOf(0, 200, 100, 200))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If it failed, try resetting it for next time
            release()
        }
    }

    private fun vibrate(pattern: LongArray) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
