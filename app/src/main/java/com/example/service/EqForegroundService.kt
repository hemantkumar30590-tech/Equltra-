package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.EqDatabase
import com.example.data.EqPresetEntity
import com.example.data.PresetManager
import kotlin.math.max

/**
 * Foreground Service that keeps the VoidEQ Equltra 10-band DSP & Global AudioSession (Session 0)
 * alive while the user is in other apps (Spotify, YouTube Music, games, etc.) and provides
 * quick notification shade controls.
 */
class EqForegroundService : Service() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val db = EqDatabase.getInstance(applicationContext)
        val presetManager = PresetManager(applicationContext, db.eqPresetDao())

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                presetManager.setBackgroundServiceEnabled(false)
                releaseGlobalHardwareEffects()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED))
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
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED))
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
                sendBroadcast(Intent(ACTION_SERVICE_STATE_CHANGED))
            }
        }

        val isEnabled = presetManager.isDspEnabled()
        val activeProfile = presetManager.loadSavedActiveProfile()

        applyGlobalSessionZeroEffects(isEnabled, activeProfile)
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

    private fun applyGlobalSessionZeroEffects(enabled: Boolean, profile: EqPresetEntity) {
        runCatching {
            if (equalizer == null) {
                equalizer = Equalizer(100, 0)
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(100, 0)
            }
            if (virtualizer == null) {
                virtualizer = Virtualizer(100, 0)
            }
            if (presetReverb == null) {
                presetReverb = PresetReverb(100, 0)
            }
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0)
            }

            val bands = profile.getBandGains()
            equalizer?.let { eq ->
                eq.enabled = enabled
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                for (b in 0 until numBands) {
                    val mappedIdx = ((b.toFloat() / max(1, numBands - 1)) * 9f).toInt().coerceIn(0, 9)
                    val db = if (enabled) bands[mappedIdx] else 0f
                    val mB = (db * 100f).toInt().coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(b.toShort(), mB)
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = enabled && profile.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(
                        (if (enabled) profile.bassBoostStrength * 10 else 0).coerceIn(0, 1000).toShort()
                    )
                }
            }

            virtualizer?.let { virt ->
                virt.enabled = enabled && profile.spatialLevel > 0
                if (virt.strengthSupported) {
                    virt.setStrength(
                        (if (enabled) profile.spatialLevel * 10 else 0).coerceIn(0, 1000).toShort()
                    )
                }
            }

            loudnessEnhancer?.let { le ->
                le.enabled = enabled
                le.setTargetGain(if (enabled) (profile.preampDb * 100f).toInt() else 0)
            }
        }
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

        val stopIntent = Intent(this, EqForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPending = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (enabled) {
            "VoidEQ Equltra • ${profile.name} Active"
        } else {
            "VoidEQ Equltra • DSP Bypassed"
        }

        val statusBody =
            "Bass ${profile.bassBoostStrength}% • Spatial ${profile.spatialLevel}% • Reverb ${profile.reverbLevel}% (Global Audio)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(statusTitle)
            .setContentText(statusBody)
            .setSubText("Background Equalizer Active")
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
                "Stop Bg",
                stopPending
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoidEQ Background DSP Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps VoidEQ Equltra equalizer active while using other apps"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun releaseGlobalHardwareEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
    }

    override fun onDestroy() {
        releaseGlobalHardwareEffects()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "voideq_equltra_bg_channel"
        const val NOTIFICATION_ID = 4010
        const val ACTION_UPDATE_STATE = "com.example.voideq.ACTION_UPDATE_STATE"
        const val ACTION_TOGGLE_BYPASS = "com.example.voideq.ACTION_TOGGLE_BYPASS"
        const val ACTION_NEXT_PRESET = "com.example.voideq.ACTION_NEXT_PRESET"
        const val ACTION_STOP_SERVICE = "com.example.voideq.ACTION_STOP_SERVICE"
        const val ACTION_SERVICE_STATE_CHANGED = "com.example.voideq.ACTION_SERVICE_STATE_CHANGED"

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
