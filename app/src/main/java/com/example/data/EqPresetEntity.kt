package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale
import kotlin.math.roundToInt

@Entity(tableName = "custom_eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Custom", // "Studio", "Device", "Imported", "Custom"
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

    val bassSummaryDb: Float
        get() {
            val g = getBandGains()
            return ((g[0] + g[1] + g[2]) / 3f * 10f).roundToInt() / 10f
        }

    val midSummaryDb: Float
        get() {
            val g = getBandGains()
            return ((g[3] + g[4] + g[5] + g[6]) / 4f * 10f).roundToInt() / 10f
        }

    val trebleSummaryDb: Float
        get() {
            val g = getBandGains()
            return ((g[7] + g[8] + g[9]) / 3f * 10f).roundToInt() / 10f
        }

    fun toExportJson(): String {
        val bandsFormatted = getBandGains().joinToString(", ") {
            String.format(Locale.US, "%.1f", it)
        }
        return """
            {
              "name": "${name.replace("\"", "\\\"")}",
              "description": "${description.replace("\"", "\\\"")}",
              "bands": [$bandsFormatted],
              "bass": $bassBoostStrength,
              "spatial": $spatialLevel,
              "reverb": $reverbLevel,
              "preamp": ${String.format(Locale.US, "%.1f", preampDb)}
            }
        """.trimIndent()
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

        /**
         * 15 Latest Oct 2026 Device-Specific EQ Presets (AutoEQ & RTINGS Tuned)
         */
        val DEVICE_PRESETS = listOf(
            EqPresetEntity(
                id = -101,
                name = "Apple AirPods Pro 3",
                category = "Device",
                description = "Warm with slight bass boost, vocal clarity (H3 Adaptive EQ)",
                bandsCsv = "3.5,3.0,2.0,0.5,1.0,2.5,3.5,2.0,1.5,2.5",
                bassBoostStrength = 48,
                reverbLevel = 14,
                reverbRoom = "Small Room",
                spatialLevel = 68,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -102,
                name = "Apple AirPods 4 ANC",
                category = "Device",
                description = "Balanced flat with airy highs & open-fit sub compensation",
                bandsCsv = "4.0,2.5,0.5,0.0,0.0,0.5,1.5,3.0,4.5,5.0",
                bassBoostStrength = 38,
                reverbLevel = 10,
                reverbRoom = "Small Room",
                spatialLevel = 58,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -103,
                name = "Sony WF-1000XM5",
                category = "Device",
                description = "Harman tuned, deep sub-bass 55Hz +3dB & upper-mid clarity",
                bandsCsv = "3.5,3.0,1.0,-1.5,-1.0,1.5,3.5,4.0,2.5,3.0",
                bassBoostStrength = 55,
                reverbLevel = 15,
                reverbRoom = "Small Room",
                spatialLevel = 45,
                preampDb = -1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -104,
                name = "Sony WF-1000XM6",
                category = "Device",
                description = "Improved XM5 curve, less mid-bass bloat & tighter transients",
                bandsCsv = "3.0,2.0,-1.5,-2.5,-0.5,1.5,3.0,3.5,3.0,2.5",
                bassBoostStrength = 44,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 52,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -105,
                name = "Sony WH-1000XM6",
                category = "Device",
                description = "Studio reference with LDAC optimization & de-mudded 250Hz",
                bandsCsv = "2.0,1.5,-2.0,-3.0,-0.5,1.0,2.5,3.5,4.0,3.5",
                bassBoostStrength = 40,
                reverbLevel = 22,
                reverbRoom = "Medium Room",
                spatialLevel = 62,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -106,
                name = "Samsung Galaxy Buds 3 Pro",
                category = "Device",
                description = "Dual driver planar + dynamic curve, boosted presence 2–4kHz",
                bandsCsv = "2.5,2.0,0.5,-0.5,0.5,2.0,4.5,4.0,2.5,2.0",
                bassBoostStrength = 50,
                reverbLevel = 16,
                reverbRoom = "Small Room",
                spatialLevel = 60,
                preampDb = -1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -107,
                name = "Samsung Galaxy Buds 4 Pro",
                category = "Device",
                description = "Next-gen acoustic powerhouse curve with ultra-wide treble extension",
                bandsCsv = "4.0,3.5,1.5,0.0,1.0,2.5,4.0,4.5,3.5,4.0",
                bassBoostStrength = 62,
                reverbLevel = 18,
                reverbRoom = "Medium Room",
                spatialLevel = 72,
                preampDb = -2.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -108,
                name = "Bose QuietComfort Ultra 2nd Gen",
                category = "Device",
                description = "Immersive audio spatial curve with controlled upper-bass",
                bandsCsv = "3.0,2.5,-1.0,-1.5,0.5,2.0,3.0,3.5,4.0,3.5",
                bassBoostStrength = 52,
                reverbLevel = 32,
                reverbRoom = "Large Hall",
                spatialLevel = 88,
                preampDb = -1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -109,
                name = "Sennheiser Momentum 4",
                category = "Device",
                description = "Audiophile aptX Lossless flat curve with sub-bass taming",
                bandsCsv = "-1.5,-1.0,-0.5,0.5,1.0,1.5,2.0,1.5,2.0,2.5",
                bassBoostStrength = 25,
                reverbLevel = 14,
                reverbRoom = "Small Room",
                spatialLevel = 42,
                preampDb = 0.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -110,
                name = "Google Pixel Buds Pro 2",
                category = "Device",
                description = "Google Tensor A1 signature clear voice & crisp midrange curve",
                bandsCsv = "2.5,2.0,0.5,1.5,3.0,4.0,3.5,2.0,1.5,2.0",
                bassBoostStrength = 36,
                reverbLevel = 10,
                reverbRoom = "Small Room",
                spatialLevel = 54,
                preampDb = -0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -111,
                name = "JBL Tour Pro 3",
                category = "Device",
                description = "Smart case hybrid dual-driver curve with extra sub-bass slam",
                bandsCsv = "6.5,5.5,3.0,0.0,-0.5,1.0,2.5,3.5,4.0,3.0",
                bassBoostStrength = 76,
                reverbLevel = 20,
                reverbRoom = "Medium Room",
                spatialLevel = 65,
                preampDb = -2.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -112,
                name = "OnePlus Buds Pro 3",
                category = "Device",
                description = "Dynaudio co-tuned bass heavy punchy V-shaped curve",
                bandsCsv = "7.0,6.0,3.5,-0.5,-1.0,1.5,3.5,4.5,4.0,3.5",
                bassBoostStrength = 80,
                reverbLevel = 18,
                reverbRoom = "Small Room",
                spatialLevel = 58,
                preampDb = -2.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -113,
                name = "Nothing Ear (a)",
                category = "Device",
                description = "Style tuned transparent curve with crisp highs & tight kick",
                bandsCsv = "3.5,3.0,1.0,0.0,0.5,2.0,3.5,4.5,5.0,4.0",
                bassBoostStrength = 54,
                reverbLevel = 14,
                reverbRoom = "Plate",
                spatialLevel = 50,
                preampDb = -1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -114,
                name = "Boat Airdopes / Noise / Realme Buds",
                category = "Device",
                description = "Indian budget buds corrective flat curve (tames boomy 125Hz)",
                bandsCsv = "-2.0,-3.5,-4.5,-1.5,1.5,3.5,4.5,3.5,2.5,2.0",
                bassBoostStrength = 22,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 46,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -115,
                name = "Over-ear Headphones Generic",
                category = "Device",
                description = "Harman 2018 over-ear target with natural soundstage air",
                bandsCsv = "4.0,3.5,1.5,-0.5,0.0,1.5,3.0,3.5,2.5,3.0",
                bassBoostStrength = 48,
                reverbLevel = 26,
                reverbRoom = "Medium Room",
                spatialLevel = 70,
                preampDb = -1.0f,
                isBuiltIn = true
            )
        )
    }
}
