package com.example.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import com.example.domain.model.HapticStrength

enum class TactileEvent {
    NUMBER,
    OPERATOR,
    EQUALS,
    BACKSPACE,
    CLEAR_ALL,
    MODE_CHANGE
}

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun trigger(
        event: TactileEvent,
        strength: HapticStrength,
        soundEnabled: Boolean
    ) {
        if (soundEnabled) {
            runCatching {
                val fx = when (event) {
                    TactileEvent.BACKSPACE -> SoundEffectConstants.NAVIGATION_LEFT
                    TactileEvent.EQUALS -> SoundEffectConstants.NAVIGATION_DOWN
                    else -> SoundEffectConstants.CLICK
                }
                audioManager?.playSoundEffect(fx, 0.35f)
            }
        }

        if (strength == HapticStrength.OFF) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val strengthScale = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.LIGHT -> 0.55f
            HapticStrength.MEDIUM -> 1.0f
            HapticStrength.STRONG -> 1.45f
        }

        runCatching {
            val hasAmp = vib.hasAmplitudeControl()
            when (event) {
                TactileEvent.BACKSPACE -> {
                    val dur = (7L * strengthScale).toLong().coerceIn(4L, 18L)
                    val amp = if (hasAmp) (75 * strengthScale).toInt().coerceIn(25, 255)
                    else VibrationEffect.DEFAULT_AMPLITUDE
                    vib.vibrate(VibrationEffect.createOneShot(dur, amp))
                }

                TactileEvent.NUMBER -> {
                    val dur = (11L * strengthScale).toLong().coerceIn(6L, 26L)
                    val amp = if (hasAmp) (105 * strengthScale).toInt().coerceIn(35, 255)
                    else VibrationEffect.DEFAULT_AMPLITUDE
                    vib.vibrate(VibrationEffect.createOneShot(dur, amp))
                }

                TactileEvent.OPERATOR -> {
                    val dur = (18L * strengthScale).toLong().coerceIn(10L, 38L)
                    val amp = if (hasAmp) (155 * strengthScale).toInt().coerceIn(50, 255)
                    else VibrationEffect.DEFAULT_AMPLITUDE
                    vib.vibrate(VibrationEffect.createOneShot(dur, amp))
                }

                TactileEvent.MODE_CHANGE -> {
                    if (hasAmp) {
                        val timings = longArrayOf(0L, (10L * strengthScale).toLong().coerceIn(6L, 22L), 24L, (14L * strengthScale).toLong().coerceIn(8L, 28L))
                        val amps = intArrayOf(0, (110 * strengthScale).toInt().coerceIn(40, 220), 0, (165 * strengthScale).toInt().coerceIn(60, 255))
                        vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                    } else {
                        val dur = (18L * strengthScale).toLong().coerceIn(10L, 35L)
                        vib.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }

                TactileEvent.CLEAR_ALL -> {
                    val dur = (30L * strengthScale).toLong().coerceIn(16L, 58L)
                    val amp = if (hasAmp) (210 * strengthScale).toInt().coerceIn(80, 255)
                    else VibrationEffect.DEFAULT_AMPLITUDE
                    vib.vibrate(VibrationEffect.createOneShot(dur, amp))
                }

                TactileEvent.EQUALS -> {
                    if (hasAmp) {
                        val d1 = (16L * strengthScale).toLong().coerceIn(10L, 30L)
                        val d2 = (22L * strengthScale).toLong().coerceIn(14L, 42L)
                        val a1 = (185 * strengthScale).toInt().coerceIn(70, 255)
                        val a2 = (240 * strengthScale).toInt().coerceIn(95, 255)
                        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, d1, 20L, d2), intArrayOf(0, a1, 0, a2), -1))
                    } else {
                        val dur = (28L * strengthScale).toLong().coerceIn(15L, 55L)
                        vib.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            }
        }
    }
}
