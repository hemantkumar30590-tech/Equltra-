package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

@Entity(tableName = "custom_eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Studio",
    val description: String = "31-Band ISO 1/3-Octave Studio Graphic EQ Profile",
    val bandsCsv: String, // 31 comma-separated dB values (-15.0 to +15.0) for 20Hz..20kHz
    val bassBoostStrength: Int, // 0 to 100
    val reverbLevel: Int, // 0 to 100
    val reverbRoom: String, // None, Small Room, Medium Room, Large Hall, Plate
    val spatialLevel: Int = 40, // 0 to 100 (3D Virtualizer / Stereo Widener)
    val preampDb: Float = 0f, // -12.0 to +12.0 dB
    val isBuiltIn: Boolean = false
) {
    fun getBandGains(): List<Float> {
        val raw = bandsCsv.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .map { it.coerceIn(-15f, 15f) }
        return interpolateTo31Bands(raw)
    }

    val bassSummaryDb: Float
        get() {
            val g = getBandGains()
            // Sub-bass & Bass: 20Hz..125Hz (indices 0..8)
            return g.subList(0, 9).average().toFloat()
        }

    val midSummaryDb: Float
        get() {
            val g = getBandGains()
            // Low-Mids to Upper-Mids: 160Hz..2.5kHz (indices 9..20)
            return g.subList(9, 21).average().toFloat()
        }

    val trebleSummaryDb: Float
        get() {
            val g = getBandGains()
            // Presence, Brilliance & Air: 3.15kHz..20kHz (indices 21..30)
            return g.subList(21, 31).average().toFloat()
        }

    fun toExportJsonString(): String {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("category", category)
        obj.put("description", description)
        obj.put("bandCount", BAND_COUNT)
        val arr = JSONArray()
        getBandGains().forEach { arr.put((it * 10.0).roundToInt() / 10.0) }
        obj.put("bands", arr)
        val freqArr = JSONArray()
        FREQUENCY_HZ.forEach { freqArr.put(it) }
        obj.put("frequenciesHz", freqArr)
        obj.put("bass", bassBoostStrength)
        obj.put("spatial", spatialLevel)
        obj.put("reverb", reverbLevel)
        obj.put("reverbRoom", reverbRoom)
        obj.put("preamp", (preampDb * 10.0).roundToInt() / 10.0)
        return obj.toString(2)
    }

    fun toExportJson(): String = toExportJsonString()

    companion object {
        const val BAND_COUNT = 31

        val FREQUENCY_LABELS = listOf(
            "20Hz", "25Hz", "31.5Hz", "40Hz", "50Hz", "63Hz", "80Hz", "100Hz", "125Hz", "160Hz",
            "200Hz", "250Hz", "315Hz", "400Hz", "500Hz", "630Hz", "800Hz", "1kHz", "1.25kHz", "1.6kHz",
            "2kHz", "2.5kHz", "3.15kHz", "4kHz", "5kHz", "6.3kHz", "8kHz", "10kHz", "12.5kHz", "16kHz",
            "20kHz"
        )

        val FREQUENCY_HZ = listOf(
            20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
            200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
            2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f,
            20000f
        )

        val LEGACY_10_BAND_HZ = listOf(
            31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
        )

        /**
         * Ensures any band list (whether 31-band ISO 1/3-octave, 10-band octave, or partial)
         * is mapped smoothly in logarithmic frequency space to all 31 ISO 1/3-octave bands (20Hz..20kHz).
         */
        fun interpolateTo31Bands(gains: List<Float>): List<Float> {
            if (gains.isEmpty()) return List(BAND_COUNT) { 0f }
            if (gains.size == BAND_COUNT) {
                return gains.map { it.coerceIn(-15f, 15f) }
            }
            if (gains.size > BAND_COUNT) {
                return gains.take(BAND_COUNT).map { it.coerceIn(-15f, 15f) }
            }
            if (gains.size == 10) {
                val logAnchors = LEGACY_10_BAND_HZ.map { ln(it.toDouble()) }
                return FREQUENCY_HZ.map { targetHz ->
                    val logF = ln(targetHz.toDouble())
                    val rawDb = when {
                        logF <= logAnchors.first() -> {
                            // Slight sub-bass roll-on/preserve at 20Hz & 25Hz relative to 31.5Hz
                            val ratio = (targetHz / 31.5f).coerceIn(0.85f, 1.0f)
                            gains.first() * ratio
                        }
                        logF >= logAnchors.last() -> {
                            // Air band 20kHz follows 16kHz smoothly
                            val ratio = (16000f / targetHz).coerceIn(0.88f, 1.0f)
                            gains.last() * ratio
                        }
                        else -> {
                            var idx = 0
                            while (idx < logAnchors.size - 2 && logF > logAnchors[idx + 1]) {
                                idx++
                            }
                            val f0 = logAnchors[idx]
                            val f1 = logAnchors[idx + 1]
                            val t = ((logF - f0) / (f1 - f0)).coerceIn(0.0, 1.0).toFloat()
                            // Smoothstep interpolation for natural analog EQ curve transitions
                            val smoothT = t * t * (3f - 2f * t)
                            gains[idx] * (1f - smoothT) + gains[idx + 1] * smoothT
                        }
                    }
                    ((rawDb * 10f).roundToInt() / 10f).coerceIn(-15f, 15f)
                }
            }
            // Generic interpolation for any other size (e.g. 5-band or 15-band)
            return List(BAND_COUNT) { idx ->
                val pos = (idx.toFloat() / (BAND_COUNT - 1)) * (gains.size - 1)
                val i0 = pos.toInt().coerceIn(0, gains.size - 1)
                val i1 = (i0 + 1).coerceAtMost(gains.size - 1)
                val frac = pos - i0
                val v = gains[i0] * (1f - frac) + gains[i1] * frac
                ((v * 10f).roundToInt() / 10f).coerceIn(-15f, 15f)
            }
        }

        fun bandsTo31Csv(vararg anchorValues: Float): String {
            val interpolated = interpolateTo31Bands(anchorValues.toList())
            return interpolated.joinToString(",") { String.format(Locale.US, "%.1f", it) }
        }

        val BUILT_IN_PRESETS = listOf(
            EqPresetEntity(
                id = -1,
                name = "Flat",
                category = "Reference",
                description = "31-Band uncolored studio reference response (0.0 dB)",
                bandsCsv = bandsTo31Csv(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
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
                category = "Sub-Bass",
                description = "Deep 20Hz–125Hz sub-bass punch with clean vocal midrange",
                bandsCsv = bandsTo31Csv(7.0f, 6.0f, 4.5f, 2.0f, 0f, -0.5f, 0.5f, 1.5f, 2.0f, 2.0f),
                bassBoostStrength = 78,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 35,
                preampDb = 1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -3,
                name = "Extreme Bass",
                category = "Sub-Bass",
                description = "Maximum 20Hz–80Hz sub-woofer excursion with soft limiter protection",
                bandsCsv = bandsTo31Csv(9.5f, 8.5f, 6.5f, 3.0f, 0f, -1.0f, 0f, 1.5f, 2.5f, 3.0f),
                bassBoostStrength = 96,
                reverbLevel = 15,
                reverbRoom = "Medium Room",
                spatialLevel = 45,
                preampDb = 2.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -4,
                name = "Harman Target",
                category = "Audiophile",
                description = "31-Band Harman Target 2026 curve with +5.5dB sub-bass shelf & 3.15kHz ear-gain",
                bandsCsv = bandsTo31Csv(5.5f, 5.0f, 3.0f, 0.5f, -0.5f, 0.5f, 2.5f, 3.5f, 2.0f, 1.0f),
                bassBoostStrength = 48,
                reverbLevel = 10,
                reverbRoom = "Small Room",
                spatialLevel = 55,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -5,
                name = "Vocal Clarity",
                category = "Studio",
                description = "Reduced low-end mud with 1kHz–4kHz vocal intelligibility & 16kHz–20kHz air",
                bandsCsv = bandsTo31Csv(-2.5f, -2.0f, -1.0f, 1.5f, 3.5f, 5.0f, 4.5f, 3.0f, 1.5f, 0.5f),
                bassBoostStrength = 10,
                reverbLevel = 18,
                reverbRoom = "Plate",
                spatialLevel = 30,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -6,
                name = "Electronic",
                category = "Genre",
                description = "Tight 20Hz–63Hz kick impact and sparkling 10kHz–20kHz synth transients",
                bandsCsv = bandsTo31Csv(6.5f, 5.5f, 2.5f, -1.0f, -1.5f, 0.5f, 2.0f, 4.0f, 5.0f, 5.5f),
                bassBoostStrength = 72,
                reverbLevel = 28,
                reverbRoom = "Large Hall",
                spatialLevel = 68,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -7,
                name = "Rock & Metal",
                category = "Genre",
                description = "Aggressive 63Hz bass guitar drive, 2.5kHz guitar bite, and crisp 12.5kHz cymbals",
                bandsCsv = bandsTo31Csv(5.0f, 4.5f, 3.0f, 1.0f, -1.0f, 1.0f, 3.0f, 4.5f, 4.5f, 4.0f),
                bassBoostStrength = 60,
                reverbLevel = 22,
                reverbRoom = "Medium Room",
                spatialLevel = 50,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -8,
                name = "Dolby Cinema 3D",
                category = "Spatial",
                description = "Wide holographic 3D stage with theatrical LFE sub-bass and crisp dialogue",
                bandsCsv = bandsTo31Csv(6.0f, 5.5f, 3.5f, 0.5f, 1.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f),
                bassBoostStrength = 68,
                reverbLevel = 42,
                reverbRoom = "Large Hall",
                spatialLevel = 92,
                preampDb = 1.5f,
                isBuiltIn = true
            )
        )

        /**
         * 15 Latest Devices (Oct 2026) 31-Band AutoEQ-calibrated sound profiles.
         */
        val DEVICE_PRESETS = listOf(
            EqPresetEntity(
                id = -101,
                name = "Apple AirPods Pro 3",
                category = "Apple",
                description = "H3 Adaptive EQ compensation: +3.5dB sub-bass extension, smooth 2.5kHz pinna & 16kHz–20kHz air",
                bandsCsv = bandsTo31Csv(3.5f, 2.5f, 1.0f, -0.5f, -1.0f, 0.5f, 1.5f, 2.5f, 3.0f, 4.0f),
                bassBoostStrength = 52,
                reverbLevel = 10,
                reverbRoom = "Small Room",
                spatialLevel = 76,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -102,
                name = "Sony WF-1000XM6",
                category = "Sony",
                description = "Opens up Sony's warm 250Hz–400Hz tuning and restores 4kHz–20kHz planar treble sparkle",
                bandsCsv = bandsTo31Csv(2.0f, 1.0f, -1.5f, -2.5f, -1.0f, 1.0f, 2.5f, 3.5f, 4.5f, 4.0f),
                bassBoostStrength = 44,
                reverbLevel = 14,
                reverbRoom = "Small Room",
                spatialLevel = 68,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -103,
                name = "Samsung Galaxy Buds 3 Pro",
                category = "Samsung",
                description = "Dual-driver planar tweeter calibration: tames 8kHz peak & tightens 63Hz mid-bass",
                bandsCsv = bandsTo31Csv(2.5f, 1.5f, 0.0f, -1.0f, 0.5f, 1.5f, 1.0f, -1.5f, -2.0f, 1.5f),
                bassBoostStrength = 48,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 70,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -104,
                name = "Samsung Galaxy Buds 4 Pro",
                category = "Samsung",
                description = "2026 Flagship 24-bit UHQ tuning: deep 20Hz–31.5Hz rumble with studio-flat vocal timbre",
                bandsCsv = bandsTo31Csv(4.0f, 3.0f, 1.5f, 0.0f, 0.0f, 1.0f, 2.0f, 1.5f, 1.0f, 2.5f),
                bassBoostStrength = 58,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 78,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -105,
                name = "Bose QuietComfort Ultra (2nd Gen)",
                category = "Bose",
                description = "Reduces Bose upper-bass bloom at 125Hz–200Hz and adds crisp 2kHz–12.5kHz instrument separation",
                bandsCsv = bandsTo31Csv(1.5f, -1.0f, -3.0f, -2.0f, 0.5f, 2.0f, 3.0f, 3.5f, 2.5f, 2.0f),
                bassBoostStrength = 36,
                reverbLevel = 18,
                reverbRoom = "Medium Room",
                spatialLevel = 84,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -106,
                name = "Sennheiser Momentum TW 4",
                category = "Sennheiser",
                description = "TrueResponse transducer audiophile target: natural midrange with lifted 10kHz–20kHz air",
                bandsCsv = bandsTo31Csv(2.5f, 2.0f, 0.5f, -0.5f, 0.0f, 1.5f, 2.5f, 1.5f, 3.0f, 3.5f),
                bassBoostStrength = 45,
                reverbLevel = 15,
                reverbRoom = "Plate",
                spatialLevel = 65,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -107,
                name = "Google Pixel Buds Pro 2",
                category = "Google",
                description = "Tensor A1 acoustic curve: rich sub-bass warmth and smooth 3.15kHz–5kHz vocal presence",
                bandsCsv = bandsTo31Csv(4.5f, 3.5f, 1.5f, -0.5f, -0.5f, 1.0f, 2.5f, 3.0f, 2.0f, 2.5f),
                bassBoostStrength = 56,
                reverbLevel = 14,
                reverbRoom = "Small Room",
                spatialLevel = 72,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -108,
                name = "JBL Tour Pro 3",
                category = "JBL",
                description = "Hybrid dual-driver balanced studio curve: tames V-shaped highs and refines sub-bass",
                bandsCsv = bandsTo31Csv(3.0f, 2.0f, -0.5f, -1.0f, 1.0f, 2.0f, 1.5f, -0.5f, -1.0f, 1.5f),
                bassBoostStrength = 54,
                reverbLevel = 16,
                reverbRoom = "Medium Room",
                spatialLevel = 66,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -109,
                name = "OnePlus Buds Pro 3",
                category = "OnePlus",
                description = "Dynaudio co-tuned correction: controls 63Hz–125Hz bass boost and brings vocals forward",
                bandsCsv = bandsTo31Csv(1.5f, -1.5f, -2.5f, -1.0f, 1.5f, 2.5f, 3.0f, 2.0f, 1.0f, 2.0f),
                bassBoostStrength = 42,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 62,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -110,
                name = "Nothing Ear (2026)",
                category = "Nothing",
                description = "Ceramic diaphragm Harman tuning: punchy 25Hz–40Hz sub-bass and silky 8kHz–16kHz treble",
                bandsCsv = bandsTo31Csv(4.0f, 3.0f, 1.0f, -0.5f, 0.5f, 1.5f, 2.0f, 1.0f, -0.5f, 2.5f),
                bassBoostStrength = 55,
                reverbLevel = 15,
                reverbRoom = "Small Room",
                spatialLevel = 64,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -111,
                name = "Sony WH-1000XM5",
                category = "Sony Over-Ear",
                description = "30mm carbon driver AutoEQ: cleans up 160Hz–250Hz mud and restores studio vocal detail",
                bandsCsv = bandsTo31Csv(3.0f, 0.5f, -2.5f, -3.0f, -0.5f, 1.5f, 3.0f, 3.5f, 2.5f, 3.0f),
                bassBoostStrength = 50,
                reverbLevel = 20,
                reverbRoom = "Medium Room",
                spatialLevel = 75,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -112,
                name = "Sony WH-1000XM6",
                category = "Sony Over-Ear",
                description = "2026 Flagship Over-Ear: ultra-deep 20Hz–31.5Hz sub-bass with wide holographic soundstage",
                bandsCsv = bandsTo31Csv(4.5f, 3.0f, 0.5f, -1.5f, 0.0f, 1.5f, 2.5f, 3.0f, 3.5f, 3.5f),
                bassBoostStrength = 62,
                reverbLevel = 22,
                reverbRoom = "Large Hall",
                spatialLevel = 86,
                preampDb = 1.0f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -113,
                name = "Apple AirPods 4 (ANC)",
                category = "Apple",
                description = "Open-ear ANC compensation: restores 20Hz–63Hz sub-bass roll-off for rich full-bodied bass",
                bandsCsv = bandsTo31Csv(6.5f, 5.5f, 3.0f, 0.5f, -0.5f, 0.5f, 1.5f, 2.0f, 2.5f, 3.0f),
                bassBoostStrength = 70,
                reverbLevel = 12,
                reverbRoom = "Small Room",
                spatialLevel = 74,
                preampDb = 1.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -114,
                name = "boAt Airdopes / Realme Buds",
                category = "Indian TWS",
                description = "Tames boomy 100Hz–160Hz mid-bass and sharp 8kHz sibilance while keeping deep 31.5Hz thump",
                bandsCsv = bandsTo31Csv(3.5f, -1.5f, -4.0f, -2.5f, 1.0f, 3.0f, 3.5f, 1.0f, -2.5f, 1.5f),
                bassBoostStrength = 48,
                reverbLevel = 14,
                reverbRoom = "Small Room",
                spatialLevel = 58,
                preampDb = 0.5f,
                isBuiltIn = true
            ),
            EqPresetEntity(
                id = -115,
                name = "Flat Reference (Studio)",
                category = "Studio Monitor",
                description = "0.0 dB neutral 31-Band studio calibration for IEMs & reference headphones",
                bandsCsv = bandsTo31Csv(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                bassBoostStrength = 0,
                reverbLevel = 0,
                reverbRoom = "None",
                spatialLevel = 0,
                preampDb = 0f,
                isBuiltIn = true
            )
        )
    }
}
