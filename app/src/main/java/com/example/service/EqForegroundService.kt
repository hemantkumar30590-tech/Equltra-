package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.EqDatabase
import com.example.data.EqPresetEntity
import com.example.data.PresetManager
import com.example.dsp.GlobalAudioEffectManager

/**
 * Foreground Service that keeps the Equltra 10-band DynamicsProcessing & Global AudioSession
 * alive while the user watches YouTube, YouTube Music, Spotify, or uses other apps.
 * Uses the shared [GlobalAudioEffectManager] singleton so it never conflicts with the UI engine.
 */
class EqForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val db = EqDatabase.getInstance(applicationContext)
        val presetManager = PresetManager(applicationContext, db.eqPresetDao())
        val globalAudioManager = GlobalAudioEffectManager.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                presetManager.setBackgroundServiceEnabled(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED).setPackage(packageName))
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_BYPASS -> {
                val newEnabled = !presetManager.isDspEnabled()
                val activeProfile = presetManager.loadSavedActiveProfile()
                presetManager.persistActiveProfileState(
                    enabled = newEnabled,
                    activePresetName = activeProfile.name,
                    bandGainsDb = activeProfile.getBandGains(),
                    bassBoostStrength = activeProfile.bassBoostStrength,
                    reverbLevel = activeProfile.reverbLevel,
                    reverbRoom = activeProfile.reverbRoom,
                    spatialLevel = activeProfile.spatialLevel,
                    preampDb = activeProfile.preampDb
                )
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED).setPackage(packageName))
            }

            ACTION_NEXT_PRESET -> {
                val builtIns = EqPresetEntity.BUILT_IN_PRESETS
                val currentName = presetManager.loadSavedActiveProfile().name
                val currentIdx = builtIns.indexOfFirst { it.name.equals(currentName, ignoreCase = true) }
                val nextPreset = builtIns[(currentIdx + 1) % builtIns.size]
                presetManager.persistActiveProfileState(
                    enabled = true,
                    activePresetName = nextPreset.name,
                    bandGainsDb = nextPreset.getBandGains(),
                    bassBoostStrength = nextPreset.bassBoostStrength,
                    reverbLevel = nextPreset.reverbLevel,
                    reverbRoom = nextPreset.reverbRoom,
                    spatialLevel = nextPreset.spatialLevel,
                    preampDb = nextPreset.preampDb
                )
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED).setPackage(packageName))
            }

            ACTION_REHOOK_YOUTUBE -> {
                globalAudioManager.forceRehookYouTubeAndSystemAudio()
            }
        }

        val isEnabled = presetManager.isDspEnabled()
        val activeProfile = presetManager.loadSavedActiveProfile()

        globalAudioManager.updateParameters(
            enabled = isEnabled,
            bandsDb = activeProfile.getBandGains(),
            bass = activeProfile.bassBoostStrength,
            reverb = activeProfile.reverbLevel,
            reverbRoom = activeProfile.reverbRoom,
            spatial = activeProfile.spatialLevel,
            preamp = activeProfile.preampDb,
            limiter = true
        )

        val notification = buildNotification(isEnabled, activeProfile)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        return START_STICKY
    }

    private fun buildNotification(enabled: Boolean, profile: EqPresetEntity): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, EqForegroundService::class.java).apply {
            action = ACTION_TOGGLE_BYPASS
        }
        val togglePending = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPresetIntent = Intent(this, EqForegroundService::class.java).apply {
            action = ACTION_NEXT_PRESET
        }
        val nextPresetPending = PendingIntent.getService(
            this,
            2,
            nextPresetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rehookIntent = Intent(this, EqForegroundService::class.java).apply {
            action = ACTION_REHOOK_YOUTUBE
        }
        val rehookPending = PendingIntent.getService(
            this,
            3,
            rehookIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (enabled) {
            "Equltra Background DSP ACTIVE"
        } else {
            "Equltra • DSP Bypassed"
        }

        val statusBody =
            "31-Band DynamicsProcessing • Bass ${profile.bassBoostStrength}% • Spatial ${profile.spatialLevel}%"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(statusTitle)
            .setContentText(statusBody)
            .setSubText("YouTube & Global Audio Equalizer")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPending)
            .addAction(
                0,
                if (enabled) "Bypass EQ" else "Enable EQ",
                togglePending
            )
            .addAction(
                0,
                "Next Preset",
                nextPresetPending
            )
            .addAction(
                0,
                "Re-Hook YouTube",
                rehookPending
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Equltra Background DSP Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Equltra 31-band equalizer active on YouTube and other apps"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "equltra_bg_channel"
        const val NOTIFICATION_ID = 4010
        const val ACTION_UPDATE_STATE = "com.example.equltra.ACTION_UPDATE_STATE"
        const val ACTION_TOGGLE_BYPASS = "com.example.equltra.ACTION_TOGGLE_BYPASS"
        const val ACTION_NEXT_PRESET = "com.example.equltra.ACTION_NEXT_PRESET"
        const val ACTION_REHOOK_YOUTUBE = "com.example.equltra.ACTION_REHOOK_YOUTUBE"
        const val ACTION_STOP_SERVICE = "com.example.equltra.ACTION_STOP_SERVICE"
        const val ACTION_SERVICE_STATE_CHANGED = "com.example.equltra.ACTION_SERVICE_STATE_CHANGED"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, EqForegroundService::class.java).apply {
                action = ACTION_UPDATE_STATE
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EqForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            runCatching {
                context.stopService(intent)
            }
        }
    }
}
