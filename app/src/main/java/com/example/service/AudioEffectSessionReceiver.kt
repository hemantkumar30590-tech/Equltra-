package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.example.dsp.GlobalAudioEffectManager

/**
 * Listens for system-wide [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION] and
 * [AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION] broadcasts from YouTube,
 * YouTube Music, Spotify, Chrome, and other media apps, immediately attaching
 * VoidEQ Equltra's 10-band DynamicsProcessing & Equalizer pipeline to their audio session.
 */
class AudioEffectSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        val manager = GlobalAudioEffectManager.getInstance(context)

        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                if (sessionId >= 0) {
                    manager.onExternalAudioSessionOpened(sessionId, packageName)
                }
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                if (sessionId > 0) {
                    manager.onExternalAudioSessionClosed(sessionId)
                }
            }
        }
    }
}
