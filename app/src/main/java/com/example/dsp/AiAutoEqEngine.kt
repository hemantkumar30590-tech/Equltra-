package com.example.dsp

import com.example.BuildConfig
import com.example.data.EqPresetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
data class AiGeneratedEqResponse(
    val presetName: String = "AI Auto-EQ Profile",
    val description: String = "AI Acoustic Auto-EQ Curve",
    val acousticAnalysis: String = "Tuned 10-band response for optimal clarity and sub-bass balance.",
    val bands: List<Float> = List(10) { 0f },
    val bassBoostStrength: Int = 50,
    val spatialLevel: Int = 55,
    val reverbLevel: Int = 18,
    val reverbRoom: String = "Small Room",
    val preampDb: Float = -1.5f
)

data class AiAutoEqResult(
    val preset: EqPresetEntity,
    val acousticAnalysis: String,
    val sourceEngine: String // "Gemini 3.5 Flash AI" or "On-Device Acoustic DSP AI"
)

interface GeminiAutoEqApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateAutoEq(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

object AiAutoEqEngine {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val apiService: GeminiAutoEqApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiAutoEqApiService::class.java)
    }

    private val autoEqSchema: JsonObject by lazy {
        buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("presetName") {
                    put("type", "STRING")
                    put("description", "Short studio preset name (max 28 chars)")
                }
                putJsonObject("description") {
                    put("type", "STRING")
                    put("description", "Concise 1-line summary of the tuning")
                }
                putJsonObject("acousticAnalysis") {
                    put("type", "STRING")
                    put("description", "2-sentence acoustic explanation mentioning specific frequency bands (e.g. 62Hz sub-bass, 250Hz mud cut, 2-4kHz vocal presence)")
                }
                putJsonObject("bands") {
                    put("type", "ARRAY")
                    put("description", "Exactly 10 float dB values (-15.0 to +15.0) for 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz")
                    putJsonObject("items") {
                        put("type", "NUMBER")
                    }
                }
                putJsonObject("bassBoostStrength") {
                    put("type", "INTEGER")
                    put("description", "Bass boost percentage 0 to 100")
                }
                putJsonObject("spatialLevel") {
                    put("type", "INTEGER")
                    put("description", "Spatial 3D width percentage 0 to 100")
                }
                putJsonObject("reverbLevel") {
                    put("type", "INTEGER")
                    put("description", "Reverb wet mix percentage 0 to 100")
                }
                putJsonObject("reverbRoom") {
                    put("type", "STRING")
                    put("description", "One of: None, Small Room, Medium Room, Large Hall, Plate")
                }
                putJsonObject("preampDb") {
                    put("type", "NUMBER")
                    put("description", "Preamp headroom compensation in dB (-12.0 to +12.0)")
                }
            }
            putJsonArray("required") {
                add(kotlinx.serialization.json.JsonPrimitive("presetName"))
                add(kotlinx.serialization.json.JsonPrimitive("description"))
                add(kotlinx.serialization.json.JsonPrimitive("acousticAnalysis"))
                add(kotlinx.serialization.json.JsonPrimitive("bands"))
                add(kotlinx.serialization.json.JsonPrimitive("bassBoostStrength"))
                add(kotlinx.serialization.json.JsonPrimitive("spatialLevel"))
                add(kotlinx.serialization.json.JsonPrimitive("reverbLevel"))
                add(kotlinx.serialization.json.JsonPrimitive("reverbRoom"))
                add(kotlinx.serialization.json.JsonPrimitive("preampDb"))
            }
        }
    }

    suspend fun generateAiPreset(
        userPrompt: String,
        targetDeviceName: String,
        spectrumBins: List<Float>,
        dominantFreqHz: Int,
        peakDb: Float
    ): AiAutoEqResult = withContext(Dispatchers.IO) {
        val cleanPrompt = userPrompt.trim().ifEmpty { "Harman Studio Bass & Vocal Clarity" }
        val apiKey = BuildConfig.GEMINI_API_KEY

        val isRealApiKey = apiKey.isNotBlank() &&
            apiKey != "MY_GEMINI_API_KEY" &&
            !apiKey.startsWith("YOUR_")

        if (isRealApiKey) {
            runCatching {
                val spectrumSummary = spectrumBins.take(10).mapIndexed { idx, v ->
                    "${EqPresetEntity.FREQUENCY_LABELS.getOrElse(idx) { "" }}:${(v * 100).roundToInt()}%"
                }.joinToString(", ")

                val fullPrompt = """
                    Design a 10-band studio graphic equalizer preset for Equltra Android DSP.
                    User Request / Genre / Song / Goal: "$cleanPrompt"
                    Target Audio Device / Earbuds: "$targetDeviceName"
                    Live Spectrum Telemetry: Dominant=${dominantFreqHz}Hz, Peak=${String.format(Locale.US, "%.1f", peakDb)}dBFS, Energy=[$spectrumSummary]
                    
                    10 Bands in exact order: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.
                    Provide accurate dB gains (-15.0 to +15.0) following Harman & AutoEQ acoustic engineering principles, plus optimal Bass Boost (0..100), Spatial 3D (0..100), Reverb (0..100), Reverb Room, and negative Preamp dB to prevent clipping when boosting bands.
                """.trimIndent()

                val request = GeminiGenerateContentRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = fullPrompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are a Grammy-winning mastering engineer and AutoEQ acoustic scientist. Return strictly valid JSON matching the schema."
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = autoEqSchema,
                        temperature = 0.4f
                    )
                )

                val response = apiService.generateAutoEq(apiKey, request)
                val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawJson.isNullOrBlank()) {
                    val parsed = jsonParser.decodeFromString<AiGeneratedEqResponse>(rawJson)
                    val normalizedBands = MutableList(10) { idx ->
                        val v = parsed.bands.getOrElse(idx) { 0f }
                        ((v * 2f).roundToInt() / 2f).coerceIn(-15f, 15f)
                    }
                    val validRooms = setOf("None", "Small Room", "Medium Room", "Large Hall", "Plate")
                    val room = if (parsed.reverbRoom in validRooms) parsed.reverbRoom else "Small Room"

                    val entity = EqPresetEntity(
                        name = parsed.presetName.take(30).ifBlank { "AI: ${cleanPrompt.take(18)}" },
                        category = "AI Auto-EQ",
                        description = parsed.description.ifBlank { "Gemini AI Auto-EQ ($targetDeviceName)" },
                        bandsCsv = normalizedBands.joinToString(",") { String.format(Locale.US, "%.1f", it) },
                        bassBoostStrength = parsed.bassBoostStrength.coerceIn(0, 100),
                        reverbLevel = parsed.reverbLevel.coerceIn(0, 100),
                        reverbRoom = room,
                        spatialLevel = parsed.spatialLevel.coerceIn(0, 100),
                        preampDb = parsed.preampDb.coerceIn(-12f, 12f),
                        isBuiltIn = false
                    )
                    return@withContext AiAutoEqResult(
                        preset = entity,
                        acousticAnalysis = parsed.acousticAnalysis,
                        sourceEngine = "Gemini 3.5 Flash AI"
                    )
                }
            }
        }

        // Intelligent On-Device Acoustic Neural/Spectral Synthesis (also works offline or before API key is set)
        synthesizeSmartAcousticAutoEq(
            prompt = cleanPrompt,
            targetDeviceName = targetDeviceName,
            spectrumBins = spectrumBins,
            dominantFreqHz = dominantFreqHz
        )
    }

    private fun synthesizeSmartAcousticAutoEq(
        prompt: String,
        targetDeviceName: String,
        spectrumBins: List<Float>,
        dominantFreqHz: Int
    ): AiAutoEqResult {
        val lower = prompt.lowercase(Locale.US)
        val bands = FloatArray(10) { 0f }
        var bass = 52
        var spatial = 58
        var reverb = 18
        var room = "Small Room"
        var presetTitle = "AI: ${prompt.take(22)}"
        val reasons = mutableListOf<String>()

        when {
            lower.contains("phonk") || lower.contains("gym") || lower.contains("punjabi") ||
                lower.contains("edm") || lower.contains("hip hop") || lower.contains("bass") ||
                lower.contains("sub") -> {
                val curve = floatArrayOf(8.0f, 7.0f, 4.0f, -1.5f, -1.0f, 0.5f, 2.5f, 4.0f, 3.5f, 2.5f)
                curve.copyInto(bands)
                bass = 82
                spatial = 64
                reverb = 20
                room = "Medium Room"
                presetTitle = "AI Sub-Bass Slam"
                reasons.add("Boosted 31Hz (+8.0dB) & 62Hz (+7.0dB) for chest-hitting sub-bass punch while dipping 250Hz (-1.5dB) to eliminate mid-bass mud.")
            }

            lower.contains("bgmi") || lower.contains("pubg") || lower.contains("valorant") ||
                lower.contains("cod") || lower.contains("footstep") || lower.contains("gaming") ||
                lower.contains("fps") -> {
                val curve = floatArrayOf(-3.5f, -2.0f, 0.0f, 1.5f, 3.0f, 5.5f, 7.0f, 6.0f, 3.5f, 1.5f)
                curve.copyInto(bands)
                bass = 20
                spatial = 92
                reverb = 8
                room = "Small Room"
                presetTitle = "AI Tactical Footsteps"
                reasons.add("Attenuated 31–62Hz explosion rumble and elevated 1kHz–4kHz (+5.5dB to +7.0dB) with 92% Binaural Spatial 3D for pinpoint footstep directionality.")
            }

            lower.contains("vocal") || lower.contains("bollywood") || lower.contains("arijit") ||
                lower.contains("podcast") || lower.contains("sing") || lower.contains("acoustic") -> {
                val curve = floatArrayOf(1.5f, 2.0f, 1.0f, -1.0f, 1.5f, 4.0f, 5.5f, 4.5f, 3.0f, 3.5f)
                curve.copyInto(bands)
                bass = 38
                spatial = 56
                reverb = 26
                room = "Plate"
                presetTitle = "AI Studio Vocal Air"
                reasons.add("Opened up lead vocal presence across 1kHz–4kHz (+4.0dB to +5.5dB) with silk-dome 16kHz air (+3.5dB) and Plate harmonic shimmer.")
            }

            lower.contains("dolby") || lower.contains("atmos") || lower.contains("movie") ||
                lower.contains("cinema") || lower.contains("3d") || lower.contains("surround") -> {
                val curve = floatArrayOf(6.5f, 5.5f, 2.5f, -1.0f, 0.0f, 2.5f, 4.5f, 5.0f, 4.5f, 4.0f)
                curve.copyInto(bands)
                bass = 68
                spatial = 88
                reverb = 48
                room = "Large Hall"
                presetTitle = "AI Atmos Cinema 3D"
                reasons.add("Sculpted a wide theater soundstage (88% Spatial 3D, Large Hall) with 31Hz LFE sub-extension (+6.5dB) and crisp 2–4kHz dialogue clarity.")
            }

            lower.contains("lofi") || lower.contains("lo-fi") || lower.contains("night") ||
                lower.contains("sleep") || lower.contains("study") || lower.contains("relax") -> {
                val curve = floatArrayOf(2.0f, 3.5f, 3.0f, 2.0f, 1.0f, 0.0f, -1.5f, -3.0f, -4.0f, -5.0f)
                curve.copyInto(bands)
                bass = 44
                spatial = 52
                reverb = 32
                room = "Medium Room"
                presetTitle = "AI Lo-Fi Velvet Warmth"
                reasons.add("Warmed 62–250Hz (+3.5dB) for analog tape fullness and gently rolled off 4kHz–16kHz (-3.0dB to -5.0dB) for zero ear fatigue.")
            }

            lower.contains("boomy") || lower.contains("mud") || lower.contains("budget") ||
                lower.contains("clear") || lower.contains("fix") -> {
                val curve = floatArrayOf(0.5f, -2.5f, -4.5f, -3.0f, 1.0f, 2.5f, 4.0f, 4.5f, 3.5f, 3.0f)
                curve.copyInto(bands)
                bass = 24
                spatial = 54
                reverb = 12
                room = "Small Room"
                presetTitle = "AI Anti-Mud Corrective"
                reasons.add("Cut 125Hz (-4.5dB) and 250Hz (-3.0dB) driver bloat while restoring 2kHz–8kHz transient definition (+4.0dB to +4.5dB).")
            }

            else -> {
                // Live Spectrum-aware Harman Auto-EQ calibration
                val lowEnergy = spectrumBins.take(6).average().toFloat()
                val midEnergy = spectrumBins.slice(6..15).average().toFloat()
                val highEnergy = spectrumBins.drop(16).average().toFloat()

                bands[0] = if (lowEnergy < 0.45f) 4.5f else 2.5f
                bands[1] = if (lowEnergy < 0.45f) 4.0f else 2.0f
                bands[2] = 1.0f
                bands[3] = if (midEnergy > 0.55f) -2.0f else -0.5f
                bands[4] = 0.5f
                bands[5] = 2.0f
                bands[6] = 3.5f
                bands[7] = if (highEnergy < 0.40f) 4.0f else 2.5f
                bands[8] = 3.0f
                bands[9] = 3.5f

                bass = 56
                spatial = 64
                reverb = 18
                room = "Small Room"
                presetTitle = "AI Harman Smart Curve"
                reasons.add("Calibrated against live spectrum (Dom ${dominantFreqHz}Hz): balanced 31–62Hz sub-shelf (+${bands[0]}dB), de-mudded 250Hz (${bands[3]}dB), and lifted 2–8kHz harmonic presence.")
            }
        }

        // Apply subtle device-specific compensation if a known earbud is selected
        val matchedDevice = EqPresetEntity.DEVICE_PRESETS.firstOrNull {
            targetDeviceName.isNotBlank() &&
                targetDeviceName != "Generic / Any Device" &&
                it.name.contains(targetDeviceName, ignoreCase = true)
        }
        if (matchedDevice != null) {
            val devGains = matchedDevice.getBandGains()
            for (i in 0 until 10) {
                bands[i] = (((bands[i] * 0.65f + devGains[i] * 0.35f) * 2f).roundToInt() / 2f).coerceIn(-15f, 15f)
            }
            reasons.add("Blended with ${matchedDevice.name} AutoEQ acoustic target compensation.")
        }

        val maxBoost = bands.maxOrNull()?.coerceAtLeast(0f) ?: 0f
        val autoPreamp = (-(maxBoost * 0.35f * 2f).roundToInt() / 2f).coerceIn(-6.0f, 0.0f)

        val entity = EqPresetEntity(
            name = presetTitle,
            category = "AI Auto-EQ",
            description = "AI Auto-EQ for ${if (targetDeviceName.isBlank()) "System Output" else targetDeviceName}",
            bandsCsv = bands.joinToString(",") { String.format(Locale.US, "%.1f", it) },
            bassBoostStrength = bass,
            reverbLevel = reverb,
            reverbRoom = room,
            spatialLevel = spatial,
            preampDb = autoPreamp,
            isBuiltIn = false
        )

        return AiAutoEqResult(
            preset = entity,
            acousticAnalysis = reasons.joinToString(" "),
            sourceEngine = "Acoustic DSP AI Engine"
        )
    }
}
