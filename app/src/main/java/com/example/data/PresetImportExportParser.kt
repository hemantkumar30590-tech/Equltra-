package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln

/**
 * Parses and serializes custom EQ presets in:
 * 1. Equltra JSON / .equltra format: {"name": "", "bands": [31 or 10 values], "bass": 64, "spatial": 82}
 * 2. AutoEQ GitHub Parametric / FixedBandEQ / GraphicEQ .txt format:
 *    - Preamp: -4.2 dB
 *    - Filter 1: ON PK Fc 31 Hz Gain 3.5 dB Q 1.41
 *    - GraphicEQ: 20 3.5; 25 3.2; 31.5 3.0; ...
 */
object PresetImportExportParser {

    fun parseImportedContent(rawContent: String, fallbackFileName: String = "Imported Preset"): EqPresetEntity? {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return null

        val cleanDefaultName = fallbackFileName
            .substringBeforeLast(".")
            .replace("_", " ")
            .trim()
            .ifEmpty { "Imported Preset" }

        // 1. Check if JSON or .equltra JSON format (supports both 31-band and legacy 10-band arrays)
        if (trimmed.startsWith("{")) {
            runCatching {
                val json = JSONObject(trimmed)
                val name = json.optString("name", cleanDefaultName).ifBlank { cleanDefaultName }
                val desc = json.optString(
                    "description",
                    "Imported 31-Band .json / .equltra profile"
                )
                val bandsArray: JSONArray? = json.optJSONArray("bands")
                val rawBands = mutableListOf<Float>()
                if (bandsArray != null) {
                    for (i in 0 until bandsArray.length()) {
                        rawBands.add(bandsArray.optDouble(i, 0.0).toFloat().coerceIn(-15f, 15f))
                    }
                }
                val bands31 = EqPresetEntity.interpolateTo31Bands(rawBands)
                val bass = json.optInt("bass", json.optInt("bassBoost", 45)).coerceIn(0, 100)
                val spatial = json.optInt("spatial", json.optInt("spatialLevel", 40)).coerceIn(0, 100)
                val reverb = json.optInt("reverb", json.optInt("reverbLevel", 15)).coerceIn(0, 100)
                val preamp = json.optDouble("preamp", json.optDouble("preampDb", 0.0)).toFloat().coerceIn(-12f, 12f)

                return EqPresetEntity(
                    name = name,
                    category = "Imported",
                    description = desc,
                    bandsCsv = bands31.joinToString(",") { String.format(Locale.US, "%.1f", it) },
                    bassBoostStrength = bass,
                    reverbLevel = reverb,
                    reverbRoom = "Small Room",
                    spatialLevel = spatial,
                    preampDb = preamp,
                    isBuiltIn = false
                )
            }
        }

        // 2. Parse AutoEQ .txt format (Parametric EQ "Filter N: ON ... Fc X Hz Gain Y dB" or "GraphicEQ:")
        return parseAutoEqTxt(trimmed, cleanDefaultName)
    }

    private fun parseAutoEqTxt(text: String, presetName: String): EqPresetEntity? {
        val targetFreqs = EqPresetEntity.FREQUENCY_HZ // 31 ISO 1/3-octave frequencies
        val bandGains = FloatArray(EqPresetEntity.BAND_COUNT) { 0f }
        var preampDb = 0f
        var matchedAny = false

        val preampRegex = Regex("""Preamp:\s*([+-]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
        val filterRegex = Regex(
            """Filter\s+\d+:\s*ON\s+\w+\s+Fc\s+(\d+(?:\.\d+)?)\s*Hz\s+Gain\s+([+-]?\d+(?:\.\d+)?)\s*dB""",
            RegexOption.IGNORE_CASE
        )

        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            preampRegex.find(trimmed)?.let { m ->
                preampDb = m.groupValues[1].toFloatOrNull()?.coerceIn(-12f, 12f) ?: 0f
            }

            if (trimmed.startsWith("GraphicEQ:", ignoreCase = true)) {
                val pairs = trimmed.substringAfter(":").split(";")
                val points = mutableListOf<Pair<Float, Float>>()
                for (pair in pairs) {
                    val parts = pair.trim().split(Regex("""\s+"""))
                    if (parts.size >= 2) {
                        val f = parts[0].toFloatOrNull()
                        val g = parts[1].toFloatOrNull()
                        if (f != null && g != null) {
                            points.add(f to g)
                        }
                    }
                }
                if (points.isNotEmpty()) {
                    matchedAny = true
                    for (b in 0 until EqPresetEntity.BAND_COUNT) {
                        val targetHz = targetFreqs[b]
                        val closest = points.minByOrNull { abs(ln(it.first.coerceAtLeast(1f)) - ln(targetHz)) }
                        if (closest != null) {
                            bandGains[b] = closest.second.coerceIn(-15f, 15f)
                        }
                    }
                }
            }

            filterRegex.find(trimmed)?.let { m ->
                val fc = m.groupValues[1].toFloatOrNull()
                val gain = m.groupValues[2].toFloatOrNull()
                if (fc != null && gain != null && fc > 10f) {
                    matchedAny = true
                    // Spread parametric filter energy across nearest 1/3-octave bands in log-frequency space
                    var bestBand = 0
                    var bestDist = Float.MAX_VALUE
                    for (b in 0 until EqPresetEntity.BAND_COUNT) {
                        val dist = abs(ln(fc) - ln(targetFreqs[b]))
                        if (dist < bestDist) {
                            bestDist = dist
                            bestBand = b
                        }
                    }
                    bandGains[bestBand] = (bandGains[bestBand] + gain).coerceIn(-15f, 15f)
                    if (bestBand > 0) {
                        bandGains[bestBand - 1] = (bandGains[bestBand - 1] + gain * 0.45f).coerceIn(-15f, 15f)
                    }
                    if (bestBand < EqPresetEntity.BAND_COUNT - 1) {
                        bandGains[bestBand + 1] = (bandGains[bestBand + 1] + gain * 0.45f).coerceIn(-15f, 15f)
                    }
                }
            }
        }

        // Fallback: if plain comma or space separated 10 or 31 numbers
        if (!matchedAny) {
            val numbers = Regex("""[+-]?\d+(?:\.\d+)?""")
                .findAll(text)
                .mapNotNull { it.value.toFloatOrNull() }
                .toList()
            if (numbers.size >= 10) {
                matchedAny = true
                val interpolated = EqPresetEntity.interpolateTo31Bands(
                    if (numbers.size >= 31) numbers.take(31) else numbers.take(10)
                )
                for (i in 0 until EqPresetEntity.BAND_COUNT) {
                    bandGains[i] = interpolated[i]
                }
            }
        }

        if (!matchedAny) return null

        val subAvg = (bandGains.take(6).average().toFloat().coerceAtLeast(0f) * 8f).toInt().coerceIn(20, 85)

        return EqPresetEntity(
            name = presetName,
            category = "Imported",
            description = "AutoEQ / Parametric 31-Band .txt imported curve",
            bandsCsv = bandGains.joinToString(",") { String.format(Locale.US, "%.1f", it) },
            bassBoostStrength = subAvg,
            reverbLevel = 12,
            reverbRoom = "Small Room",
            spatialLevel = 45,
            preampDb = preampDb,
            isBuiltIn = false
        )
    }
}
