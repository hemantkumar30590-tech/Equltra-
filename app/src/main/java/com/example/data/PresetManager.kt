package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * PresetManager coordinates storing and loading custom 10-band equalizer sound profiles,
 * 15 Oct 2026 Device-Specific EQ presets, imported AutoEQ / .json / .equltra profiles,
 * and active DSP state.
 */
class PresetManager(
    context: Context,
    private val dao: EqPresetDao
) {
    private val activeStateBox: SharedPreferences =
        context.applicationContext.getSharedPreferences(ACTIVE_STATE_BOX_NAME, Context.MODE_PRIVATE)

    val devicePresets: List<EqPresetEntity> = EqPresetEntity.DEVICE_PRESETS

    val allSoundProfiles: Flow<List<EqPresetEntity>> =
        dao.getAllCustomPresets().combine(flowOf(EqPresetEntity.BUILT_IN_PRESETS)) { custom, builtIn ->
            builtIn + custom
        }

    val importedProfiles: Flow<List<EqPresetEntity>> =
        dao.getAllCustomPresets().map { list ->
            list.filter { it.category.equals("Imported", ignoreCase = true) }
        }

    suspend fun saveCustomSoundProfile(
        name: String,
        description: String,
        bandGainsDb: List<Float>,
        bassBoostStrength: Int,
        reverbLevel: Int,
        reverbRoom: String,
        spatialLevel: Int,
        preampDb: Float,
        category: String = "Custom"
    ): EqPresetEntity {
        val cleanName = name.trim().ifEmpty { "My Sound Profile" }
        val cleanDesc = description.trim().ifEmpty { "User saved 10-band studio profile" }
        val clampedBands = List(10) { idx ->
            bandGainsDb.getOrElse(idx) { 0f }.coerceIn(-15f, 15f)
        }
        val preset = EqPresetEntity(
            name = cleanName,
            category = category,
            description = cleanDesc,
            bandsCsv = clampedBands.joinToString(",") { String.format(Locale.US, "%.1f", it) },
            bassBoostStrength = bassBoostStrength.coerceIn(0, 100),
            reverbLevel = reverbLevel.coerceIn(0, 100),
            reverbRoom = reverbRoom,
            spatialLevel = spatialLevel.coerceIn(0, 100),
            preampDb = preampDb.coerceIn(-12f, 12f),
            isBuiltIn = false
        )
        dao.insertPreset(preset)
        persistActiveProfileState(
            enabled = isDspEnabled(),
            activePresetName = cleanName,
            bandGainsDb = clampedBands,
            bassBoostStrength = preset.bassBoostStrength,
            reverbLevel = preset.reverbLevel,
            reverbRoom = preset.reverbRoom,
            spatialLevel = preset.spatialLevel,
            preampDb = preset.preampDb
        )
        return preset
    }

    suspend fun saveImportedSoundProfile(imported: EqPresetEntity): EqPresetEntity {
        val tagged = imported.copy(category = "Imported", isBuiltIn = false)
        dao.insertPreset(tagged)
        persistActiveProfileState(
            enabled = isDspEnabled(),
            activePresetName = tagged.name,
            bandGainsDb = tagged.getBandGains(),
            bassBoostStrength = tagged.bassBoostStrength,
            reverbLevel = tagged.reverbLevel,
            reverbRoom = tagged.reverbRoom,
            spatialLevel = tagged.spatialLevel,
            preampDb = tagged.preampDb
        )
        return tagged
    }

    suspend fun deleteCustomSoundProfile(preset: EqPresetEntity) {
        if (!preset.isBuiltIn) {
            dao.deletePresetById(preset.id)
        }
    }

    fun persistActiveProfileState(
        enabled: Boolean,
        activePresetName: String,
        bandGainsDb: List<Float>,
        bassBoostStrength: Int,
        reverbLevel: Int,
        reverbRoom: String,
        spatialLevel: Int,
        preampDb: Float
    ) {
        val bandsCsv = List(10) { idx ->
            bandGainsDb.getOrElse(idx) { 0f }.coerceIn(-15f, 15f)
        }.joinToString(",") { String.format(Locale.US, "%.1f", it) }

        activeStateBox.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_ACTIVE_PRESET, activePresetName)
            .putString(KEY_BANDS_CSV, bandsCsv)
            .putInt(KEY_BASS_BOOST, bassBoostStrength.coerceIn(0, 100))
            .putInt(KEY_REVERB_LEVEL, reverbLevel.coerceIn(0, 100))
            .putString(KEY_REVERB_ROOM, reverbRoom)
            .putInt(KEY_SPATIAL_LEVEL, spatialLevel.coerceIn(0, 100))
            .putFloat(KEY_PREAMP_DB, preampDb.coerceIn(-12f, 12f))
            .apply()
    }

    fun isDspEnabled(): Boolean = activeStateBox.getBoolean(KEY_ENABLED, true)

    fun isBackgroundServiceEnabled(): Boolean =
        activeStateBox.getBoolean(KEY_BG_SERVICE_ENABLED, true)

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        activeStateBox.edit().putBoolean(KEY_BG_SERVICE_ENABLED, enabled).apply()
    }

    fun isAiProUnlocked(): Boolean =
        activeStateBox.getBoolean(KEY_AI_PRO_UNLOCKED, false)

    fun setAiProUnlocked(unlocked: Boolean) {
        activeStateBox.edit().putBoolean(KEY_AI_PRO_UNLOCKED, unlocked).apply()
    }

    fun verifyOwnerProAccess(input: String): Boolean {
        val normalized = input.trim().lowercase(Locale.US)
        return normalized in OWNER_VALID_KEYS
    }

    fun loadSavedActiveProfile(): EqPresetEntity {
        val defaultPreset = EqPresetEntity.BUILT_IN_PRESETS[1] // Bass Boost
        val presetName = activeStateBox.getString(KEY_ACTIVE_PRESET, defaultPreset.name) ?: defaultPreset.name
        val bandsCsv = activeStateBox.getString(KEY_BANDS_CSV, defaultPreset.bandsCsv) ?: defaultPreset.bandsCsv
        val bassBoost = activeStateBox.getInt(KEY_BASS_BOOST, defaultPreset.bassBoostStrength)
        val reverbLevel = activeStateBox.getInt(KEY_REVERB_LEVEL, defaultPreset.reverbLevel)
        val reverbRoom = activeStateBox.getString(KEY_REVERB_ROOM, defaultPreset.reverbRoom) ?: defaultPreset.reverbRoom
        val spatialLevel = activeStateBox.getInt(KEY_SPATIAL_LEVEL, defaultPreset.spatialLevel)
        val preampDb = activeStateBox.getFloat(KEY_PREAMP_DB, defaultPreset.preampDb)

        return EqPresetEntity(
            id = 0,
            name = presetName,
            category = "Active",
            description = "Restored active sound profile",
            bandsCsv = bandsCsv,
            bassBoostStrength = bassBoost,
            reverbLevel = reverbLevel,
            reverbRoom = reverbRoom,
            spatialLevel = spatialLevel,
            preampDb = preampDb,
            isBuiltIn = (EqPresetEntity.BUILT_IN_PRESETS + EqPresetEntity.DEVICE_PRESETS)
                .any { it.name.equals(presetName, ignoreCase = true) }
        )
    }

    companion object {
        const val ACTIVE_STATE_BOX_NAME = "equltra_settings_box"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BG_SERVICE_ENABLED = "bg_service_enabled"
        private const val KEY_AI_PRO_UNLOCKED = "ai_pro_unlocked"
        private const val KEY_ACTIVE_PRESET = "preset"
        private const val KEY_BANDS_CSV = "bands_csv"
        private const val KEY_BASS_BOOST = "bass_boost"
        private const val KEY_REVERB_LEVEL = "reverb_level"
        private const val KEY_REVERB_ROOM = "reverb_room"
        private const val KEY_SPATIAL_LEVEL = "spatial_level"
        private const val KEY_PREAMP_DB = "preamp_db"

        private val OWNER_VALID_KEYS = setOf(
            "hemantkumar30590@gmail.com",
            "hemantkumar30590-tech",
            "hemantkumar30590",
            "hemant30590",
            "30590"
        )
    }
}
