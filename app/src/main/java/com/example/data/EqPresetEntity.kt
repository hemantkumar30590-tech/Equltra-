package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Custom",
    val description: String = "User saved studio curve",
    val bandsCsv: String, // 10 comma-separated dB values (-15f..+15f)
    val bassBoostStrength: Int = 0, // 0..100
    val reverbLevel: Int = 0, // 0..100
    val reverbRoom: String = "Small Room",
    val spatialLevel: Int = 0, // 0..100
    val preampDb: Float = 0f,
    val isBuiltIn: Boolean = false
) {
    fun getBandGains(): List<Float> {
        val parsed = bandsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        return if (parsed.size == 10) parsed else List(10) { 0f }
    }

    companion object {
        val FREQUENCY_LABELS = listOf(
            "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
            "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
        )

        val FREQUENCY_HZ = listOf(
            31f, 62f, 125f, 250f, 500f,
            1000f, 2000f, 4000f, 8000f, 16000f
        )

        val BUILT_IN_PRESETS = listOf(
            EqPresetEntity(
                id = -1,
                name = "Flat",
                category = "Reference",
                description = "Transparent 0.0 dB studio reference response",
                bandsCsv = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                bassBoostStrength = 0,
                reverbLevel = 0,
                reverbRoom = "None",
                spatialLevel = 0,
                preampDb = 0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -2,
                name = "Bass Boost",
                category = "Low-End Punch",
                description = "Deep sub-bass shelf +8.5 dB with clean low-mid transient punch",
                bandsCsv = "8.5,7.5,5.0,1.5,-0.5,0.0,1.0,2.0,2.5,1.5",
                bassBoostStrength = 78,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 35,
                preampDb = -2.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -3,
                name = "Warm",
                category = "Analog Tube",
                description = "Lush upper-bass warmth with smooth silk-dome treble roll-off",
                bandsCsv = "3.0,4.5,4.0,2.5,1.0,0.0,-1.0,-2.0,-2.5,-3.5",
                bassBoostStrength = 42,
                reverbLevel = 22,
                reverbRoom = "Medium Room",
                spatialLevel = 28,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -4,
                name = "Theater",
                category = "Cinema Surround",
                description = "Wide soundstage with sub-rumble extension and crisp air",
                bandsCsv = "6.5,5.5,3.0,-0.5,-1.5,2.0,4.0,5.0,4.5,3.5",
                bassBoostStrength = 64,
                reverbLevel = 55,
                reverbRoom = "Large Hall",
                spatialLevel = 82,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -5,
                name = "Dialogue Boost",
                category = "Voice & Podcast",
                description = "High-pass sub cut with focused 1kHz–4kHz vocal intelligibility",
                bandsCsv = "-4.5,-3.0,-1.0,2.5,5.5,7.5,6.5,3.5,0.5,-2.0",
                bassBoostStrength = 0,
                reverbLevel = 5,
                reverbRoom = "None",
                spatialLevel = 18,
                preampDb = 2.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -6,
                name = "Action Mode",
                category = "Explosive V-Curve",
                description = "High-impact low-end slam and hyper-detailed transient presence",
                bandsCsv = "7.5,6.5,4.0,0.5,-2.0,1.5,4.5,6.5,7.0,5.5",
                bassBoostStrength = 85,
                reverbLevel = 38,
                reverbRoom = "Medium Room",
                spatialLevel = 75,
                preampDb = -1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -7,
                name = "Soft",
                category = "Fatigue-Free",
                description = "Gentle midrange cushion with tamed sibilance and sub-bass",
                bandsCsv = "-2.0,-1.0,0.5,1.5,2.0,1.0,-0.5,-2.5,-3.5,-4.5",
                bassBoostStrength = 15,
                reverbLevel = 25,
                reverbRoom = "Small Room",
                spatialLevel = 30,
                preampDb = 0.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -8,
                name = "Late Night",
                category = "Night Comfort",
                description = "Attenuates heavy sub-bass wall-shake while keeping whispers clear",
                bandsCsv = "-7.0,-5.0,-2.0,2.0,4.0,4.5,3.0,-0.5,-2.5,-4.0",
                bassBoostStrength = 0,
                reverbLevel = 15,
                reverbRoom = "Small Room",
                spatialLevel = 45,
                preampDb = 3.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -9,
                name = "Vocal Clarity",
                category = "Studio Presence",
                description = "Articulate lead vocals, acoustic guitars, and harmonic air",
                bandsCsv = "-3.5,-2.5,-1.0,3.0,6.0,7.0,5.5,3.5,2.0,1.0",
                bassBoostStrength = 12,
                reverbLevel = 28,
                reverbRoom = "Plate",
                spatialLevel = 48,
                preampDb = 1.5f,
                isBuiltIn = true
            )
        )
    }
}
