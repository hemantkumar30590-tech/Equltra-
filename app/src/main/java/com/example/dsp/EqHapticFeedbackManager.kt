package com.example.dsp

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.data.EqPresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class EqHapticPattern(val displayLabel: String) {
    PRESET_SELECT_SUB_BASS("Tactile Sub-Bass Thump + Lock"),
    PRESET_SELECT_SPATIAL("Tactile Spatial 3D Ripple"),
    PRESET_SELECT_STUDIO("Tactile Studio Detent Click"),
    PRESET_TOGGLE_ON("Tactile Preset Toggle ON"),
    PRESET_TOGGLE_OFF("Tactile Preset Toggle OFF"),
    AI_PRESET_LOCK("Tactile AI Auto-EQ Harmonic Lock"),
    FADER_CENTER_DETENT("0.0dB Center Fader Detent"),
    CONTROL_TICK("Hardware Control Tick")
}

/**
 * Provides hardware-accelerated tactile haptic feedback when selecting or toggling
 * equalizer presets, toggling master DSP power, or snapping 10-band studio faders.
 *
 * Uses Android's VibratorManager / Vibrator APIs with custom acoustic-profiled
 * VibrationEffect waveforms alongside View HapticFeedbackConstants fallback.
 */
class EqHapticFeedbackManager(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _lastHapticEventLabel = MutableStateFlow("TACTILE HAPTICS READY")
    val lastHapticEventLabel: StateFlow<String> = _lastHapticEventLabel.asStateFlow()

    private val _hapticTriggerCount = MutableStateFlow(0)
    val hapticTriggerCount: StateFlow<Int> = _hapticTriggerCount.asStateFlow()

    private var lastFaderTickTimeMs: Long = 0L

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        _lastHapticEventLabel.value = if (enabled) "TACTILE HAPTICS ON" else "TACTILE HAPTICS MUTED"
        if (enabled) {
            triggerPattern(EqHapticPattern.PRESET_TOGGLE_ON, "Haptics Enabled")
        }
    }

    /**
     * Triggers an acoustic-tailored tactile haptic pulse when a user selects an EQ preset.
     * - Bass-heavy presets produce a warm dual-pulse sub-bass thump + crisp detent lock.
     * - Spatial-heavy presets produce a 3-step expanding spatial ripple.
     * - Reference/Flat/Vocal presets produce a surgical studio console detent click.
     */
    fun onPresetSelected(preset: EqPresetEntity, view: View? = null) {
        val gains = preset.getBandGains()
        val lowBandAvg = if (gains.size >= 3) (gains[0] + gains[1] + gains[2]) / 3f else 0f
        val pattern = when {
            preset.bassBoostStrength >= 72 || lowBandAvg >= 3.5f ->
                EqHapticPattern.PRESET_SELECT_SUB_BASS
            preset.spatialLevel >= 72 ->
                EqHapticPattern.PRESET_SELECT_SPATIAL
            else ->
                EqHapticPattern.PRESET_SELECT_STUDIO
        }

        performViewHaptic(view, isConfirm = true)
        triggerPattern(pattern, preset.name)
    }

    /**
     * Triggers a distinct ascending or descending tactile switch snap when a user
     * toggles an active equalizer preset or master equalizer state ON / OFF.
     */
    fun onPresetToggled(enabled: Boolean, presetName: String, view: View? = null) {
        val pattern = if (enabled) {
            EqHapticPattern.PRESET_TOGGLE_ON
        } else {
            EqHapticPattern.PRESET_TOGGLE_OFF
        }
        performViewHaptic(view, isConfirm = enabled)
        triggerPattern(pattern, "$presetName (${if (enabled) "ON" else "BYPASS"})")
    }

    /**
     * Triggers a 3-stage harmonic lock pulse when an AI Auto-EQ preset finishes generating.
     */
    fun onAiPresetApplied(presetName: String, view: View? = null) {
        performViewHaptic(view, isConfirm = true)
        triggerPattern(EqHapticPattern.AI_PRESET_LOCK, presetName)
    }

    /**
     * Triggers a crisp studio detent click when a fader crosses 0.0 dB or a reverb room chip is tapped.
     */
    fun onControlTick(label: String = "Studio Detent", isZeroDbSnap: Boolean = false, view: View? = null) {
        val now = System.currentTimeMillis()
        if (now - lastFaderTickTimeMs < 45L) return
        lastFaderTickTimeMs = now

        val pattern = if (isZeroDbSnap) {
            EqHapticPattern.FADER_CENTER_DETENT
        } else {
            EqHapticPattern.CONTROL_TICK
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        triggerPattern(pattern, label)
    }

    private fun performViewHaptic(view: View?, isConfirm: Boolean) {
        if (!_isHapticsEnabled.value || view == null) return
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (isConfirm) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(constant)
        }
    }

    private fun triggerPattern(pattern: EqHapticPattern, contextLabel: String) {
        _hapticTriggerCount.value += 1
        _lastHapticEventLabel.value = "${pattern.displayLabel}: $contextLabel"

        if (!_isHapticsEnabled.value) return
        val vib = vibrator ?: return

        runCatching {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = buildVibrationEffect(pattern)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()
                    vib.vibrate(effect, attrs)
                } else {
                    vib.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(20L)
            }
        }
    }

    private fun buildVibrationEffect(pattern: EqHapticPattern): VibrationEffect {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw IllegalStateException("Requires API 26+")
        }
        return when (pattern) {
            EqHapticPattern.PRESET_SELECT_SUB_BASS -> {
                // Deep Sub-Bass Double Thump + Crisp Lock Detent
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 26L, 32L, 16L),
                    intArrayOf(0, 235, 0, 160),
                    -1
                )
            }

            EqHapticPattern.PRESET_SELECT_SPATIAL -> {
                // Expanding 3-Pulse Spatial 3D Ripple
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 12L, 22L, 15L, 22L, 18L),
                    intArrayOf(0, 120, 0, 175, 0, 225),
                    -1
                )
            }

            EqHapticPattern.PRESET_SELECT_STUDIO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createOneShot(16L, 180)
                }
            }

            EqHapticPattern.PRESET_TOGGLE_ON -> {
                // Ascending Dual-Pulse Mechanical Switch ON
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 14L, 24L, 22L),
                    intArrayOf(0, 135, 0, 245),
                    -1
                )
            }

            EqHapticPattern.PRESET_TOGGLE_OFF -> {
                // Descending Soft Release Switch OFF
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 20L, 24L, 11L),
                    intArrayOf(0, 185, 0, 95),
                    -1
                )
            }

            EqHapticPattern.AI_PRESET_LOCK -> {
                // 3-Stage Harmonic Lock for AI Auto-EQ
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 14L, 24L, 18L, 26L, 26L),
                    intArrayOf(0, 130, 0, 190, 0, 255),
                    -1
                )
            }

            EqHapticPattern.FADER_CENTER_DETENT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                } else {
                    VibrationEffect.createOneShot(10L, 140)
                }
            }

            EqHapticPattern.CONTROL_TICK -> {
                VibrationEffect.createOneShot(8L, 110)
            }
        }
    }

    companion object {
        fun didCrossZeroDb(oldDb: Float, newDb: Float): Boolean {
            if (oldDb == newDb) return false
            return (oldDb != 0f && abs(newDb) < 0.01f) || (oldDb < 0f && newDb > 0f) || (oldDb > 0f && newDb < 0f)
        }
    }
}
