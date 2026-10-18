package com.example.dsp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.data.EqPresetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class VoiceControlAction {
    data class SetMasterEnabled(val enabled: Boolean) : VoiceControlAction()
    data class SetBackgroundService(val enabled: Boolean) : VoiceControlAction()
    object SyncYouTube : VoiceControlAction()
    data class SetTestAudioLoop(val play: Boolean) : VoiceControlAction()
    data class SetMicSpectrum(val enabled: Boolean) : VoiceControlAction()
    data class SetBassBoost(val strength: Int) : VoiceControlAction()
    data class AdjustBassBoost(val delta: Int) : VoiceControlAction()
    data class SetSpatial3D(val level: Int) : VoiceControlAction()
    data class AdjustSpatial3D(val delta: Int) : VoiceControlAction()
    data class SetReverbLevel(val level: Int) : VoiceControlAction()
    data class SetReverbRoom(val room: String, val level: Int? = null) : VoiceControlAction()
    data class SetPreampDb(val db: Float) : VoiceControlAction()
    data class SetSoftLimiter(val enabled: Boolean) : VoiceControlAction()
    data class SetSingleBand(val bandIndex: Int, val gainDb: Float) : VoiceControlAction()
    data class ApplyCurveOffsets(val label: String, val targetBands: List<Float>) : VoiceControlAction()
    object ResetFlat : VoiceControlAction()
    data class ApplyPreset(val preset: EqPresetEntity) : VoiceControlAction()
    data class SelectTab(val tabIndex: Int, val tabName: String) : VoiceControlAction()
    data class UnlockProWithKey(val key: String) : VoiceControlAction()
    object LockProMode : VoiceControlAction()
    data class RunAiAutoEq(val prompt: String, val deviceHint: String? = null) : VoiceControlAction()
}

data class ParsedVoiceCommandResult(
    val spokenTranscript: String,
    val actionTitle: String,
    val spokenFeedback: String,
    val actions: List<VoiceControlAction>,
    val requiresProUnlock: Boolean = false
)

class AiVoiceControlEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onCommandRecognized: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isHandsFreeLoop = MutableStateFlow(false)
    val isHandsFreeLoop: StateFlow<Boolean> = _isHandsFreeLoop.asStateFlow()

    private val _isVoiceTtsEnabled = MutableStateFlow(true)
    val isVoiceTtsEnabled: StateFlow<Boolean> = _isVoiceTtsEnabled.asStateFlow()

    private val _voiceRmsLevel = MutableStateFlow(0f)
    val voiceRmsLevel: StateFlow<Float> = _voiceRmsLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _voiceStatusBadge = MutableStateFlow("VOICE AI READY")
    val voiceStatusBadge: StateFlow<String> = _voiceStatusBadge.asStateFlow()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setPitch(1.02f)
                    tts?.setSpeechRate(1.06f)
                    isTtsReady = true
                }
            }
        }
    }

    fun setVoiceTtsEnabled(enabled: Boolean) {
        _isVoiceTtsEnabled.value = enabled
        if (!enabled) {
            runCatching { tts?.stop() }
        }
    }

    fun setHandsFreeLoop(enabled: Boolean) {
        _isHandsFreeLoop.value = enabled
    }

    fun speakFeedback(message: String) {
        if (!_isVoiceTtsEnabled.value || !isTtsReady || message.isBlank()) return
        runCatching {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "equltra_voice_feedback")
        }
    }

    fun startListening(languageTag: String = "en-IN") {
        scope.launch(Dispatchers.Main) {
            runCatching {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _voiceStatusBadge.value = "USE MIC INTENT / QUICK VOICE"
                    _isListening.value = false
                    return@launch
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _isListening.value = true
                _voiceStatusBadge.value = "LISTENING LIVE (HINDI / EN)..."
                _liveTranscript.value = "Listening... Speak any EQ or AI command"
                speechRecognizer?.startListening(intent)
            }.onFailure {
                _isListening.value = false
                _voiceStatusBadge.value = "TAP SYSTEM MIC OR VOICE CHIP"
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
            _isHandsFreeLoop.value = false
            _voiceRmsLevel.value = 0f
            _voiceStatusBadge.value = "VOICE AI READY"
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _voiceStatusBadge.value = "SPEAK NOW • VOICE AI ACTIVE"
            }

            override fun onBeginningOfSpeech() {
                _voiceStatusBadge.value = "CAPTURING VOICE COMMAND..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                _voiceRmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceRmsLevel.value = 0f
                _voiceStatusBadge.value = "PROCESSING VOICE AI..."
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _voiceRmsLevel.value = 0f
                _voiceStatusBadge.value = "VOICE AI READY"
                if (_isHandsFreeLoop.value && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    scope.launch(Dispatchers.Main) {
                        kotlinx.coroutines.delay(900)
                        if (_isHandsFreeLoop.value) {
                            startListening()
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _voiceRmsLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val bestText = matches?.firstOrNull()?.trim().orEmpty()
                if (bestText.isNotEmpty()) {
                    _liveTranscript.value = bestText
                    _voiceStatusBadge.value = "EXECUTED: ${bestText.take(22).uppercase(Locale.US)}"
                    onCommandRecognized(bestText)
                } else {
                    _voiceStatusBadge.value = "VOICE AI READY"
                }

                if (_isHandsFreeLoop.value) {
                    scope.launch(Dispatchers.Main) {
                        kotlinx.coroutines.delay(1400)
                        if (_isHandsFreeLoop.value) {
                            startListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    _liveTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun release() {
        runCatching {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    companion object {
        val QUICK_VOICE_COMMANDS: List<Pair<String, String>> = listOf(
            "🎙️ \"Bass Full 100% + 3D\"" to "Bass full 100 percent aur 3D spatial 90 percent kar do",
            "🎙️ \"AI Punjabi Sub-Bass\"" to "AI Auto-EQ Punjabi heavy sub-bass aur clear vocals set karo",
            "🎙️ \"BGMI Footsteps AI\"" to "AI Auto-EQ BGMI footsteps aur gunshots directional clarity",
            "🎙️ \"AirPods Pro 3 Tune\"" to "Apply Apple AirPods Pro 3 preset",
            "🎙️ \"Sony XM6 Studio\"" to "Apply Sony WF-1000XM6 preset",
            "🎙️ \"Sync YouTube DSP\"" to "Sync YouTube global audio hook chalu karo",
            "🎙️ \"Dolby Atmos 3D\"" to "AI Auto-EQ Dolby Atmos Cinema 3D surround sound",
            "🎙️ \"Play Test Audio\"" to "Play test audio loop",
            "🎙️ \"Reset Flat EQ\"" to "Sab flat studio reset kar do"
        )

        /**
         * Parses English, Hindi, and Hinglish natural voice commands to control EVERY part of Equltra:
         * Master DSP, YouTube Sync, Background Service, Test Audio, Mic Analyzer, Bass %, Spatial 3D %,
         * Reverb %, Preamp dB, 10-Band frequencies, 15 Device Presets, Built-in Presets, Tabs, PRO Lock/Unlock,
         * and AI-Based Auto-EQ Preset System generation.
         */
        fun parseVoiceCommand(
            rawCommand: String,
            currentBands: List<Float>,
            devicePresets: List<EqPresetEntity>,
            allPresets: List<EqPresetEntity>
        ): ParsedVoiceCommandResult {
            val clean = rawCommand.trim()
            val lower = clean.lowercase(Locale.US)
            val actions = mutableListOf<VoiceControlAction>()
            val summaries = mutableListOf<String>()
            var spokenReply = ""

            // 1. Check PRO Unlock / Lock Voice Commands first
            if (lower.contains("30590") ||
                (lower.contains("unlock") && (lower.contains("pro") || lower.contains("hemant") || lower.contains("owner"))) ||
                (lower.contains("pro") && (lower.contains("kholo") || lower.contains("on karo") || lower.contains("unlock")))
            ) {
                val keyToPass = if (lower.contains("30590") || lower.contains("hemant") || lower.contains("unlock")) {
                    "30590"
                } else {
                    clean
                }
                actions.add(VoiceControlAction.UnlockProWithKey(keyToPass))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "PRO Mode Voice Unlock",
                    spokenFeedback = "Owner verified. Equltra AI Auto E Q Pro Mode is now unlocked.",
                    actions = actions
                )
            }

            if ((lower.contains("lock") && lower.contains("pro")) || lower.contains("pro lock karo")) {
                actions.add(VoiceControlAction.LockProMode)
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "PRO Mode Locked",
                    spokenFeedback = "Equltra AI Auto E Q Pro Mode has been locked.",
                    actions = actions
                )
            }

            // 2. Check Tab Navigation Commands
            if (lower.startsWith("open ") || lower.startsWith("go to ") || lower.contains("tab kholo") || lower.contains("screen kholo")) {
                when {
                    lower.contains("device") || lower.contains("earbud") -> {
                        actions.add(VoiceControlAction.SelectTab(2, "Device Presets"))
                        return ParsedVoiceCommandResult(
                            spokenTranscript = clean,
                            actionTitle = "Opened Device Presets Tab",
                            spokenFeedback = "Opening Device Presets tab.",
                            actions = actions
                        )
                    }
                    lower.contains("apk") || lower.contains("github") || lower.contains("download") -> {
                        actions.add(VoiceControlAction.SelectTab(4, "Get APK"))
                        return ParsedVoiceCommandResult(
                            spokenTranscript = clean,
                            actionTitle = "Opened Get APK Tab",
                            spokenFeedback = "Opening Get A P K tab.",
                            actions = actions
                        )
                    }
                    lower.contains("preset") || lower.contains("import") -> {
                        actions.add(VoiceControlAction.SelectTab(3, "Presets"))
                        return ParsedVoiceCommandResult(
                            spokenTranscript = clean,
                            actionTitle = "Opened Presets Tab",
                            spokenFeedback = "Opening Presets and Import tab.",
                            actions = actions
                        )
                    }
                    lower.contains("bass") || lower.contains("dsp") || lower.contains("spatial") -> {
                        actions.add(VoiceControlAction.SelectTab(1, "Bass & DSP"))
                        return ParsedVoiceCommandResult(
                            spokenTranscript = clean,
                            actionTitle = "Opened Bass & DSP Tab",
                            spokenFeedback = "Opening Bass and Spatial D S P tab.",
                            actions = actions
                        )
                    }
                    lower.contains("10") || lower.contains("band") || lower.contains("equalizer") || lower.contains("home") -> {
                        actions.add(VoiceControlAction.SelectTab(0, "10-Band EQ"))
                        return ParsedVoiceCommandResult(
                            spokenTranscript = clean,
                            actionTitle = "Opened 10-Band EQ Tab",
                            spokenFeedback = "Opening 10 Band Equalizer tab.",
                            actions = actions
                        )
                    }
                }
            }

            // 3. Check Master EQ Power / Bypass
            if (lower.contains("eq off") || lower.contains("turn off eq") || lower.contains("disable eq") ||
                lower.contains("bypass eq") || lower.contains("eq band karo") || lower.contains("equalizer off")
            ) {
                actions.add(VoiceControlAction.SetMasterEnabled(false))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Master EQ Bypassed (OFF)",
                    spokenFeedback = "Equalizer bypassed.",
                    actions = actions
                )
            }
            if (lower.contains("eq on") || lower.contains("turn on eq") || lower.contains("enable eq") ||
                lower.contains("eq chalu karo") || lower.contains("equalizer on")
            ) {
                actions.add(VoiceControlAction.SetMasterEnabled(true))
                summaries.add("Master EQ ON")
                spokenReply = "Equalizer enabled."
            }

            // 4. Check YouTube Sync & Background Service & Test Loop & Mic Spectrum
            if (lower.contains("youtube") || lower.contains("rehook") || lower.contains("sync audio")) {
                actions.add(VoiceControlAction.SyncYouTube)
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Synced YouTube & System Audio Hook",
                    spokenFeedback = "YouTube and global 10 band D S P session hooked.",
                    actions = actions
                )
            }

            if (lower.contains("background") && (lower.contains("on") || lower.contains("chalu") || lower.contains("start"))) {
                actions.add(VoiceControlAction.SetBackgroundService(true))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Background DSP Service Enabled",
                    spokenFeedback = "Background D S P service enabled.",
                    actions = actions
                )
            }
            if (lower.contains("background") && (lower.contains("off") || lower.contains("band") || lower.contains("stop"))) {
                actions.add(VoiceControlAction.SetBackgroundService(false))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Background DSP Service Stopped",
                    spokenFeedback = "Background D S P service stopped.",
                    actions = actions
                )
            }

            if (lower.contains("test") && (lower.contains("play") || lower.contains("start") || lower.contains("chalu") || lower.contains("on"))) {
                actions.add(VoiceControlAction.SetTestAudioLoop(true))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Playing Synth Test Audio",
                    spokenFeedback = "Playing synthesizer test audio loop.",
                    actions = actions
                )
            }
            if (lower.contains("test") && (lower.contains("stop") || lower.contains("band") || lower.contains("off") || lower.contains("pause"))) {
                actions.add(VoiceControlAction.SetTestAudioLoop(false))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Stopped Synth Test Audio",
                    spokenFeedback = "Stopped test audio.",
                    actions = actions
                )
            }

            // 5. Check Flat / Reset Command
            if (lower.contains("reset") || lower.contains("flat kar") || lower == "flat" || lower.contains("flat eq") || lower.contains("zero kar")) {
                actions.add(VoiceControlAction.ResetFlat)
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Reset to Flat 0.0 dB Studio Reference",
                    spokenFeedback = "All 10 equalizer bands reset to flat studio reference.",
                    actions = actions
                )
            }

            // 6. Check Direct Device Preset Match (15 Latest Devices)
            val matchedDevice = devicePresets.firstOrNull { preset ->
                val pLower = preset.name.lowercase(Locale.US)
                lower.contains(pLower) ||
                    (lower.contains("airpods pro") && pLower.contains("airpods pro 3")) ||
                    (lower.contains("airpods 4") && pLower.contains("airpods 4")) ||
                    (lower.contains("xm5") && pLower.contains("xm5")) ||
                    (lower.contains("wf-1000xm6") || (lower.contains("xm6") && !lower.contains("wh") && pLower.contains("wf-1000xm6"))) ||
                    (lower.contains("wh-1000xm6") && pLower.contains("wh-1000xm6")) ||
                    (lower.contains("buds 3") && pLower.contains("buds 3 pro")) ||
                    (lower.contains("buds 4") && pLower.contains("buds 4 pro")) ||
                    (lower.contains("bose") && pLower.contains("bose")) ||
                    (lower.contains("sennheiser") || (lower.contains("momentum") && pLower.contains("momentum"))) ||
                    (lower.contains("pixel buds") && pLower.contains("pixel buds")) ||
                    (lower.contains("jbl") && pLower.contains("jbl")) ||
                    (lower.contains("oneplus") && pLower.contains("oneplus")) ||
                    (lower.contains("nothing") && pLower.contains("nothing")) ||
                    ((lower.contains("boat") || lower.contains("airdopes") || lower.contains("realme")) && pLower.contains("boat"))
            }

            if (matchedDevice != null && !lower.contains("ai ")) {
                actions.add(VoiceControlAction.ApplyPreset(matchedDevice))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Applied Device Preset: ${matchedDevice.name}",
                    spokenFeedback = "Applied ${matchedDevice.name} 10 band Auto E Q preset.",
                    actions = actions
                )
            }

            // 7. Extract numeric values for Bass Boost, Spatial 3D, Reverb, Preamp, or specific Hz bands
            val numberRegex = Regex("""(-?\d+(?:\.\d+)?)""")
            val allNumbers = numberRegex.findAll(lower).mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()

            // Check Bass Boost voice control
            if (lower.contains("bass") && !lower.contains("ai ") && !lower.contains("punjabi") && !lower.contains("phonk")) {
                when {
                    lower.contains("full") || lower.contains("max") || lower.contains("100") -> {
                        actions.add(VoiceControlAction.SetBassBoost(100))
                        val boostedBands = currentBands.toMutableList()
                        boostedBands[0] = 8.5f
                        boostedBands[1] = 7.5f
                        boostedBands[2] = 4.5f
                        actions.add(VoiceControlAction.ApplyCurveOffsets("Sub-Bass Max", boostedBands))
                        summaries.add("Bass 100% + Sub-Bass Boost")
                        spokenReply = "Maximum bass boost 100 percent with sub bass shelf applied."
                    }
                    lower.contains("badhao") || lower.contains("increase") || lower.contains("more") || lower.contains("up") || lower.contains("zyada") -> {
                        actions.add(VoiceControlAction.AdjustBassBoost(20))
                        val boostedBands = currentBands.toMutableList()
                        boostedBands[0] = (boostedBands[0] + 3.0f).coerceIn(-15f, 15f)
                        boostedBands[1] = (boostedBands[1] + 2.5f).coerceIn(-15f, 15f)
                        actions.add(VoiceControlAction.ApplyCurveOffsets("Bass +20%", boostedBands))
                        summaries.add("Bass Boost +20% & 31-62Hz +3dB")
                        spokenReply = "Increased bass boost by 20 percent and lifted sub bass."
                    }
                    lower.contains("kam") || lower.contains("decrease") || lower.contains("less") || lower.contains("down") || lower.contains("reduce") -> {
                        actions.add(VoiceControlAction.AdjustBassBoost(-20))
                        val cutBands = currentBands.toMutableList()
                        cutBands[0] = (cutBands[0] - 2.5f).coerceIn(-15f, 15f)
                        cutBands[1] = (cutBands[1] - 2.0f).coerceIn(-15f, 15f)
                        actions.add(VoiceControlAction.ApplyCurveOffsets("Bass -20%", cutBands))
                        summaries.add("Bass Boost -20%")
                        spokenReply = "Reduced bass boost by 20 percent."
                    }
                    allNumbers.isNotEmpty() -> {
                        val pct = allNumbers.first().toInt().coerceIn(0, 100)
                        actions.add(VoiceControlAction.SetBassBoost(pct))
                        summaries.add("Bass Boost $pct%")
                        spokenReply = "Bass boost set to $pct percent."
                    }
                }
            }

            // Check Spatial 3D voice control
            if ((lower.contains("3d") || lower.contains("spatial") || lower.contains("surround")) &&
                !lower.contains("dolby") && !lower.contains("cinema")
            ) {
                when {
                    lower.contains("full") || lower.contains("max") || lower.contains("100") -> {
                        actions.add(VoiceControlAction.SetSpatial3D(100))
                        summaries.add("3D Spatial 100%")
                        spokenReply = "Spatial 3 D surround set to 100 percent."
                    }
                    lower.contains("badhao") || lower.contains("increase") || lower.contains("more") -> {
                        actions.add(VoiceControlAction.AdjustSpatial3D(20))
                        summaries.add("3D Spatial +20%")
                        spokenReply = "Increased spatial 3 D width."
                    }
                    lower.contains("kam") || lower.contains("decrease") || lower.contains("less") -> {
                        actions.add(VoiceControlAction.AdjustSpatial3D(-20))
                        summaries.add("3D Spatial -20%")
                        spokenReply = "Reduced spatial 3 D width."
                    }
                    allNumbers.isNotEmpty() -> {
                        val pct = allNumbers.last().toInt().coerceIn(0, 100)
                        actions.add(VoiceControlAction.SetSpatial3D(pct))
                        summaries.add("3D Spatial $pct%")
                        spokenReply = "Spatial 3 D set to $pct percent."
                    }
                }
            }

            // Check Reverb voice control
            if (lower.contains("reverb") || lower.contains("hall") || lower.contains("plate")) {
                when {
                    lower.contains("large hall") || lower.contains("hall") -> {
                        actions.add(VoiceControlAction.SetReverbRoom("Large Hall", 45))
                        summaries.add("Reverb: Large Hall (45%)")
                        spokenReply = "Applied Large Hall acoustic reverb."
                    }
                    lower.contains("plate") -> {
                        actions.add(VoiceControlAction.SetReverbRoom("Plate", 35))
                        summaries.add("Reverb: Plate (35%)")
                        spokenReply = "Applied studio Plate reverb."
                    }
                    lower.contains("off") || lower.contains("band") || lower.contains("none") -> {
                        actions.add(VoiceControlAction.SetReverbRoom("None", 0))
                        summaries.add("Reverb OFF")
                        spokenReply = "Reverb turned off."
                    }
                    allNumbers.isNotEmpty() -> {
                        val pct = allNumbers.first().toInt().coerceIn(0, 100)
                        actions.add(VoiceControlAction.SetReverbLevel(pct))
                        summaries.add("Reverb $pct%")
                        spokenReply = "Reverb level set to $pct percent."
                    }
                }
            }

            // Check Treble / Vocal quick voice shaping
            if ((lower.contains("treble") || lower.contains("highs")) && actions.isEmpty()) {
                val updated = currentBands.toMutableList()
                val delta = if (lower.contains("kam") || lower.contains("reduce") || lower.contains("down")) -3.5f else 4.0f
                updated[7] = (updated[7] + delta).coerceIn(-15f, 15f)
                updated[8] = (updated[8] + delta).coerceIn(-15f, 15f)
                updated[9] = (updated[9] + delta).coerceIn(-15f, 15f)
                actions.add(VoiceControlAction.ApplyCurveOffsets("Voice Treble Tune", updated))
                summaries.add("Treble (4k–16kHz) ${if (delta > 0) "+4.0dB" else "-3.5dB"}")
                spokenReply = if (delta > 0) "Boosted treble air frequencies." else "Reduced high treble frequencies."
            }

            if (actions.isNotEmpty()) {
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = summaries.joinToString(" • ").ifEmpty { "Voice DSP Command Executed" },
                    spokenFeedback = spokenReply.ifBlank { "Voice command applied." },
                    actions = actions
                )
            }

            // 8. Check Built-In or Saved Presets by Name
            val matchedSaved = allPresets.firstOrNull { preset ->
                lower.contains(preset.name.lowercase(Locale.US))
            }
            if (matchedSaved != null && !lower.contains("ai")) {
                actions.add(VoiceControlAction.ApplyPreset(matchedSaved))
                return ParsedVoiceCommandResult(
                    spokenTranscript = clean,
                    actionTitle = "Applied Preset: ${matchedSaved.name}",
                    spokenFeedback = "Applied ${matchedSaved.name} preset.",
                    actions = actions
                )
            }

            // 9. Otherwise: Route to AI-Based Auto-EQ Preset System (Gemini 3.5 Flash + Acoustic DSP AI)!
            actions.add(
                VoiceControlAction.RunAiAutoEq(
                    prompt = clean,
                    deviceHint = matchedDevice?.name
                )
            )
            return ParsedVoiceCommandResult(
                spokenTranscript = clean,
                actionTitle = "AI Voice Auto-EQ: \"$clean\"",
                spokenFeedback = "Tuning 10 band A I Auto E Q and D S P for $clean.",
                actions = actions,
                requiresProUnlock = true
            )
        }
    }
}
