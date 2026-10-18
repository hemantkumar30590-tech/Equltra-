package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EqDatabase
import com.example.data.EqPresetEntity
import com.example.data.PresetImportExportParser
import com.example.data.PresetManager
import com.example.dsp.AiAutoEqEngine
import com.example.dsp.AiVoiceControlEngine
import com.example.dsp.AudioDspEngine
import com.example.dsp.VoiceControlAction
import com.example.service.EqForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class EqUiState(
    val isMasterEnabled: Boolean = true,
    val isBackgroundServiceEnabled: Boolean = true,
    val selectedTab: Int = 0, // 0: EQ, 1: Bass & DSP, 2: Device Presets, 3: Presets, 4: Get APK
    val deviceSearchQuery: String = "",
    val activePresetName: String = "Bass Boost",
    val bandGainsDb: List<Float> = EqPresetEntity.BUILT_IN_PRESETS[1].getBandGains(),
    val bassBoostStrength: Int = 78, // 0..100
    val reverbLevel: Int = 12, // 0..100
    val reverbRoom: String = "Small Room",
    val spatialLevel: Int = 35, // 0..100
    val preampDb: Float = -2.0f, // -12..+12 dB
    val softLimiterEnabled: Boolean = true,
    val isGlobalSystemMode: Boolean = true,
    val showSavePresetDialog: Boolean = false,
    val showPasteImportDialog: Boolean = false,
    val showAiAutoEqDialog: Boolean = false,
    val showAiProUnlockDialog: Boolean = false,
    val showVoiceControlDialog: Boolean = false,
    val isAiProUnlocked: Boolean = false,
    val isGeneratingAiEq: Boolean = false,
    val aiPromptInput: String = "",
    val aiSelectedDevice: String = "Apple AirPods Pro 3",
    val lastAiAnalysis: String = "PRO MODE LOCKED — Exclusive AI Auto-EQ + Voice Control Mastering Engine. Unlock with Owner Key to use.",
    val lastAiEngineBadge: String = "Gemini 3.5 Flash AI",
    val lastVoiceCommandSummary: String = "Say \"Bass full 100%\", \"AirPods Pro 3\", \"Sync YouTube\", or \"AI Punjabi Bass\"",
    val voiceCommandHistory: List<String> = listOf(
        "🎙️ Ready: Speak in Hindi, Hinglish, or English to control 10-Band EQ, Bass, 3D, Presets & AI Auto-EQ"
    )
)

class EqViewModel(application: Application) : AndroidViewModel(application) {

    private val presetManager: PresetManager
    private val dspEngine: AudioDspEngine
    private val voiceEngine: AiVoiceControlEngine

    private val _uiState: MutableStateFlow<EqUiState>
    val uiState: StateFlow<EqUiState>

    val devicePresets: List<EqPresetEntity>
    val allPresets: StateFlow<List<EqPresetEntity>>
    val importedPresets: StateFlow<List<EqPresetEntity>>
    val isPlayingTestLoop: StateFlow<Boolean>
    val isAudioRecordActive: StateFlow<Boolean>
    val isSystemAudioActive: StateFlow<Boolean>
    val systemVolumePercent: StateFlow<Int>
    val waveformSamples: StateFlow<List<Float>>
    val stereoVuLevels: StateFlow<Pair<Float, Float>>
    val spectrumBins: StateFlow<List<Float>>
    val peakHoldBins: StateFlow<List<Float>>
    val dominantFreqHz: StateFlow<Int>
    val peakDb: StateFlow<Float>
    val hardwareStatus: StateFlow<String>

    val isVoiceListening: StateFlow<Boolean>
    val isVoiceHandsFree: StateFlow<Boolean>
    val isVoiceTtsEnabled: StateFlow<Boolean>
    val voiceRmsLevel: StateFlow<Float>
    val voiceLiveTranscript: StateFlow<String>
    val voiceStatusBadge: StateFlow<String>

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EqForegroundService.ACTION_SERVICE_STATE_CHANGED) {
                reloadFromPresetManager()
            }
        }
    }

    init {
        val db = EqDatabase.getInstance(application)
        presetManager = PresetManager(application, db.eqPresetDao())
        dspEngine = AudioDspEngine(application, viewModelScope)
        voiceEngine = AiVoiceControlEngine(application, viewModelScope) { recognizedSpeech ->
            executeVoiceCommand(recognizedSpeech)
        }
        devicePresets = presetManager.devicePresets

        val restored = presetManager.loadSavedActiveProfile()
        val bgEnabled = presetManager.isBackgroundServiceEnabled()
        val proUnlocked = presetManager.isAiProUnlocked()
        _uiState = MutableStateFlow(
            EqUiState(
                isMasterEnabled = presetManager.isDspEnabled(),
                isBackgroundServiceEnabled = bgEnabled,
                isAiProUnlocked = proUnlocked,
                activePresetName = restored.name,
                bandGainsDb = restored.getBandGains(),
                bassBoostStrength = restored.bassBoostStrength,
                reverbLevel = restored.reverbLevel,
                reverbRoom = restored.reverbRoom,
                spatialLevel = restored.spatialLevel,
                preampDb = restored.preampDb,
                lastAiAnalysis = if (proUnlocked) {
                    "OWNER PRO UNLOCKED 👑 — Tap any AI Scenario Chip, use Voice Control 🎙️, or open AI Studio to auto-tune all 10 bands & DSP."
                } else {
                    "PRO MODE LOCKED 🔒 — Exclusive AI Auto-EQ + Voice Control Mastering Engine. Tap UNLOCK PRO (or say 'Unlock Pro 30590') to access."
                }
            )
        )
        uiState = _uiState.asStateFlow()

        allPresets = presetManager.allSoundProfiles
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EqPresetEntity.BUILT_IN_PRESETS
            )

        importedPresets = presetManager.importedProfiles
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        isPlayingTestLoop = dspEngine.isPlayingTestLoop
        isAudioRecordActive = dspEngine.isAudioRecordActive
        isSystemAudioActive = dspEngine.isSystemAudioActive
        systemVolumePercent = dspEngine.systemVolumePercent
        waveformSamples = dspEngine.waveformSamples
        stereoVuLevels = dspEngine.stereoVuLevels
        spectrumBins = dspEngine.spectrumBins
        peakHoldBins = dspEngine.peakHoldBins
        dominantFreqHz = dspEngine.dominantFreqHz
        peakDb = dspEngine.peakDb
        hardwareStatus = dspEngine.hardwareStatus

        isVoiceListening = voiceEngine.isListening
        isVoiceHandsFree = voiceEngine.isHandsFreeLoop
        isVoiceTtsEnabled = voiceEngine.isVoiceTtsEnabled
        voiceRmsLevel = voiceEngine.voiceRmsLevel
        voiceLiveTranscript = voiceEngine.liveTranscript
        voiceStatusBadge = voiceEngine.voiceStatusBadge

        runCatching {
            ContextCompat.registerReceiver(
                application,
                serviceReceiver,
                IntentFilter(EqForegroundService.ACTION_SERVICE_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        syncDspEngine()
    }

    private fun reloadFromPresetManager() {
        val restored = presetManager.loadSavedActiveProfile()
        val enabled = presetManager.isDspEnabled()
        val bgEnabled = presetManager.isBackgroundServiceEnabled()
        _uiState.update {
            it.copy(
                isMasterEnabled = enabled,
                isBackgroundServiceEnabled = bgEnabled,
                activePresetName = restored.name,
                bandGainsDb = restored.getBandGains(),
                bassBoostStrength = restored.bassBoostStrength,
                reverbLevel = restored.reverbLevel,
                reverbRoom = restored.reverbRoom,
                spatialLevel = restored.spatialLevel,
                preampDb = restored.preampDb
            )
        }
        dspEngine.updateDspState(
            enabled = enabled,
            bandsDb = restored.getBandGains(),
            bass = restored.bassBoostStrength,
            reverb = restored.reverbLevel,
            reverbRoom = restored.reverbRoom,
            spatial = restored.spatialLevel,
            preamp = restored.preampDb,
            limiter = _uiState.value.softLimiterEnabled
        )
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex.coerceIn(0, 4)) }
    }

    fun updateDeviceSearchQuery(query: String) {
        _uiState.update { it.copy(deviceSearchQuery = query) }
    }

    fun toggleMasterEnabled() {
        _uiState.update { it.copy(isMasterEnabled = !it.isMasterEnabled) }
        syncDspEngine()
    }

    fun setMasterEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isMasterEnabled = enabled) }
        syncDspEngine()
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        presetManager.setBackgroundServiceEnabled(enabled)
        _uiState.update { it.copy(isBackgroundServiceEnabled = enabled) }
        val app = getApplication<Application>()
        if (enabled) {
            EqForegroundService.startOrUpdate(app)
        } else {
            EqForegroundService.stop(app)
        }
    }

    fun updateBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..9) return
        val clamped = (kotlin.math.round(gainDb * 2f) / 2f).coerceIn(-15f, 15f)
        _uiState.update { state ->
            val updated = state.bandGainsDb.toMutableList()
            updated[bandIndex] = clamped
            state.copy(
                bandGainsDb = updated,
                activePresetName = "Custom Curve"
            )
        }
        syncDspEngine()
    }

    fun resetToFlat() {
        applyPreset(EqPresetEntity.BUILT_IN_PRESETS.first())
    }

    fun updateBassBoost(strength: Int) {
        _uiState.update { it.copy(bassBoostStrength = strength.coerceIn(0, 100)) }
        syncDspEngine()
    }

    fun updateSpatialLevel(level: Int) {
        _uiState.update { it.copy(spatialLevel = level.coerceIn(0, 100)) }
        syncDspEngine()
    }

    fun updateReverbLevel(level: Int) {
        _uiState.update { it.copy(reverbLevel = level.coerceIn(0, 100)) }
        syncDspEngine()
    }

    fun updateReverbRoom(room: String) {
        _uiState.update { it.copy(reverbRoom = room) }
        syncDspEngine()
    }

    fun updatePreampDb(db: Float) {
        val clamped = (kotlin.math.round(db * 2f) / 2f).coerceIn(-12f, 12f)
        _uiState.update { it.copy(preampDb = clamped) }
        syncDspEngine()
    }

    fun toggleSoftLimiter() {
        _uiState.update { it.copy(softLimiterEnabled = !it.softLimiterEnabled) }
        syncDspEngine()
    }

    fun applyPreset(preset: EqPresetEntity) {
        _uiState.update { state ->
            state.copy(
                activePresetName = preset.name,
                bandGainsDb = preset.getBandGains(),
                bassBoostStrength = preset.bassBoostStrength,
                reverbLevel = preset.reverbLevel,
                reverbRoom = preset.reverbRoom,
                spatialLevel = preset.spatialLevel,
                preampDb = preset.preampDb
            )
        }
        syncDspEngine()
    }

    fun setSavePresetDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSavePresetDialog = visible) }
    }

    fun setPasteImportDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showPasteImportDialog = visible) }
    }

    fun setAiAutoEqDialogVisible(visible: Boolean) {
        if (visible && !_uiState.value.isAiProUnlocked) {
            _uiState.update { it.copy(showAiProUnlockDialog = true) }
            return
        }
        _uiState.update { it.copy(showAiAutoEqDialog = visible) }
    }

    fun setAiProUnlockDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAiProUnlockDialog = visible) }
    }

    fun setVoiceControlDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showVoiceControlDialog = visible) }
    }

    fun startVoiceListening() {
        voiceEngine.startListening()
    }

    fun stopVoiceListening() {
        voiceEngine.stopListening()
    }

    fun toggleVoiceHandsFree() {
        val next = !isVoiceHandsFree.value
        voiceEngine.setHandsFreeLoop(next)
        if (next && !isVoiceListening.value) {
            voiceEngine.startListening()
        } else if (!next && isVoiceListening.value) {
            voiceEngine.stopListening()
        }
    }

    fun toggleVoiceTts() {
        voiceEngine.setVoiceTtsEnabled(!isVoiceTtsEnabled.value)
    }

    fun executeVoiceCommand(
        rawSpokenText: String,
        onFeedbackMessage: (String) -> Unit = {}
    ) {
        val cleanText = rawSpokenText.trim()
        if (cleanText.isEmpty()) return

        val currentState = _uiState.value
        val parsed = AiVoiceControlEngine.parseVoiceCommand(
            rawCommand = cleanText,
            currentBands = currentState.bandGainsDb,
            devicePresets = devicePresets,
            allPresets = allPresets.value
        )

        var unlockedNow = currentState.isAiProUnlocked

        for (action in parsed.actions) {
            when (action) {
                is VoiceControlAction.UnlockProWithKey -> {
                    val ok = tryUnlockAiProMode(action.key)
                    unlockedNow = ok
                }
                is VoiceControlAction.LockProMode -> {
                    lockAiProMode()
                    unlockedNow = false
                }
                is VoiceControlAction.SelectTab -> {
                    selectTab(action.tabIndex)
                }
                is VoiceControlAction.SetMasterEnabled -> {
                    setMasterEnabled(action.enabled)
                }
                is VoiceControlAction.SetBackgroundService -> {
                    setBackgroundServiceEnabled(action.enabled)
                }
                is VoiceControlAction.SyncYouTube -> {
                    rehookYouTubeAudio()
                }
                is VoiceControlAction.SetTestAudioLoop -> {
                    if (isPlayingTestLoop.value != action.play) {
                        toggleTestAudioLoop()
                    }
                }
                is VoiceControlAction.SetMicSpectrum -> {
                    setAudioRecordSpectrumEnabled(action.enabled)
                }
                is VoiceControlAction.SetBassBoost -> {
                    updateBassBoost(action.strength)
                }
                is VoiceControlAction.AdjustBassBoost -> {
                    updateBassBoost((_uiState.value.bassBoostStrength + action.delta).coerceIn(0, 100))
                }
                is VoiceControlAction.SetSpatial3D -> {
                    updateSpatialLevel(action.level)
                }
                is VoiceControlAction.AdjustSpatial3D -> {
                    updateSpatialLevel((_uiState.value.spatialLevel + action.delta).coerceIn(0, 100))
                }
                is VoiceControlAction.SetReverbLevel -> {
                    updateReverbLevel(action.level)
                }
                is VoiceControlAction.SetReverbRoom -> {
                    updateReverbRoom(action.room)
                    action.level?.let { updateReverbLevel(it) }
                }
                is VoiceControlAction.SetPreampDb -> {
                    updatePreampDb(action.db)
                }
                is VoiceControlAction.SetSoftLimiter -> {
                    if (_uiState.value.softLimiterEnabled != action.enabled) {
                        toggleSoftLimiter()
                    }
                }
                is VoiceControlAction.SetSingleBand -> {
                    updateBandGain(action.bandIndex, action.gainDb)
                }
                is VoiceControlAction.ApplyCurveOffsets -> {
                    _uiState.update {
                        it.copy(
                            bandGainsDb = action.targetBands,
                            activePresetName = "Voice: ${action.label}"
                        )
                    }
                    syncDspEngine()
                }
                is VoiceControlAction.ResetFlat -> {
                    resetToFlat()
                }
                is VoiceControlAction.ApplyPreset -> {
                    applyPreset(action.preset)
                }
                is VoiceControlAction.RunAiAutoEq -> {
                    if (action.deviceHint != null) {
                        updateAiSelectedDevice(action.deviceHint)
                    }
                    if (!unlockedNow) {
                        // Automatically unlock for Voice AI command or prompt if locked
                        _uiState.update {
                            it.copy(
                                aiPromptInput = action.prompt,
                                showAiProUnlockDialog = true,
                                lastVoiceCommandSummary = "🔒 PRO Locked: Say 'Unlock Pro 30590' or enter Owner Key to run AI Auto-EQ"
                            )
                        }
                        voiceEngine.speakFeedback("A I Auto E Q is locked in Pro Mode. Say Unlock Pro 3 0 5 9 0 to unlock.")
                        onFeedbackMessage("PRO Mode Locked 🔒 — Say 'Unlock Pro 30590' or unlock with Owner PIN!")
                        return
                    } else {
                        generateAndApplyAiAutoEq(
                            overridePrompt = action.prompt,
                            saveToLibrary = true
                        ) { aiPreset ->
                            voiceEngine.speakFeedback("Applied ${aiPreset.name}. Bass ${aiPreset.bassBoostStrength} percent, Spatial ${aiPreset.spatialLevel} percent.")
                            onFeedbackMessage("🎙️ Voice AI Auto-EQ Applied: ${aiPreset.name}")
                        }
                    }
                }
            }
        }

        val logEntry = "🎙️ \"${parsed.spokenTranscript}\" → ${parsed.actionTitle}"
        _uiState.update { state ->
            val updatedHistory = (listOf(logEntry) + state.voiceCommandHistory).take(8)
            state.copy(
                lastVoiceCommandSummary = "${parsed.actionTitle} (\"${parsed.spokenTranscript}\")",
                voiceCommandHistory = updatedHistory
            )
        }

        if (parsed.actions.none { it is VoiceControlAction.RunAiAutoEq }) {
            voiceEngine.speakFeedback(parsed.spokenFeedback)
            onFeedbackMessage("🎙️ ${parsed.actionTitle}")
        }
    }

    fun tryUnlockAiProMode(ownerKeyInput: String): Boolean {
        val valid = presetManager.verifyOwnerProAccess(ownerKeyInput)
        if (valid) {
            presetManager.setAiProUnlocked(true)
            _uiState.update {
                it.copy(
                    isAiProUnlocked = true,
                    showAiProUnlockDialog = false,
                    lastAiAnalysis = "OWNER PRO UNLOCKED 👑 — Tap any AI Scenario Chip, use Voice Control 🎙️, or open AI Studio to auto-tune all 10 bands & DSP."
                )
            }
        }
        return valid
    }

    fun lockAiProMode() {
        presetManager.setAiProUnlocked(false)
        _uiState.update {
            it.copy(
                isAiProUnlocked = false,
                showAiAutoEqDialog = false,
                lastAiAnalysis = "PRO MODE LOCKED 🔒 — Exclusive AI Auto-EQ + Voice Control Mastering Engine. Tap UNLOCK PRO (Owner Only) to access."
            )
        }
    }

    fun updateAiPromptInput(prompt: String) {
        _uiState.update { it.copy(aiPromptInput = prompt) }
    }

    fun updateAiSelectedDevice(device: String) {
        _uiState.update { it.copy(aiSelectedDevice = device) }
    }

    fun generateAndApplyAiAutoEq(
        overridePrompt: String? = null,
        saveToLibrary: Boolean = true,
        onComplete: (EqPresetEntity) -> Unit = {}
    ) {
        val state = _uiState.value
        if (!state.isAiProUnlocked) {
            _uiState.update { it.copy(showAiProUnlockDialog = true) }
            return
        }
        val promptToUse = (overridePrompt ?: state.aiPromptInput).trim()
            .ifEmpty { "Live Spectrum Smart Calibration" }

        _uiState.update {
            it.copy(
                isGeneratingAiEq = true,
                aiPromptInput = promptToUse
            )
        }

        viewModelScope.launch {
            val result = AiAutoEqEngine.generateAiPreset(
                userPrompt = promptToUse,
                targetDeviceName = state.aiSelectedDevice,
                spectrumBins = spectrumBins.value,
                dominantFreqHz = dominantFreqHz.value,
                peakDb = peakDb.value
            )

            val finalPreset = if (saveToLibrary) {
                presetManager.saveCustomSoundProfile(
                    name = result.preset.name,
                    description = result.preset.description,
                    bandGainsDb = result.preset.getBandGains(),
                    bassBoostStrength = result.preset.bassBoostStrength,
                    reverbLevel = result.preset.reverbLevel,
                    reverbRoom = result.preset.reverbRoom,
                    spatialLevel = result.preset.spatialLevel,
                    preampDb = result.preset.preampDb
                )
            } else {
                result.preset
            }

            _uiState.update {
                it.copy(
                    isGeneratingAiEq = false,
                    activePresetName = finalPreset.name,
                    bandGainsDb = finalPreset.getBandGains(),
                    bassBoostStrength = finalPreset.bassBoostStrength,
                    reverbLevel = finalPreset.reverbLevel,
                    reverbRoom = finalPreset.reverbRoom,
                    spatialLevel = finalPreset.spatialLevel,
                    preampDb = finalPreset.preampDb,
                    lastAiAnalysis = result.acousticAnalysis,
                    lastAiEngineBadge = result.sourceEngine
                )
            }
            syncDspEngine()
            onComplete(finalPreset)
        }
    }

    fun saveCurrentAsCustomPreset(name: String, description: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val saved = presetManager.saveCustomSoundProfile(
                name = name,
                description = description,
                bandGainsDb = state.bandGainsDb,
                bassBoostStrength = state.bassBoostStrength,
                reverbLevel = state.reverbLevel,
                reverbRoom = state.reverbRoom,
                spatialLevel = state.spatialLevel,
                preampDb = state.preampDb
            )
            _uiState.update {
                it.copy(
                    activePresetName = saved.name,
                    showSavePresetDialog = false
                )
            }
            if (_uiState.value.isBackgroundServiceEnabled) {
                EqForegroundService.startOrUpdate(getApplication())
            }
        }
    }

    fun importPresetFromContent(
        rawContent: String,
        fallbackFileName: String = "Imported Preset",
        onResult: (EqPresetEntity?) -> Unit = {}
    ) {
        val parsed = PresetImportExportParser.parseImportedContent(rawContent, fallbackFileName)
        if (parsed == null) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            val saved = presetManager.saveImportedSoundProfile(parsed)
            _uiState.update {
                it.copy(
                    activePresetName = saved.name,
                    bandGainsDb = saved.getBandGains(),
                    bassBoostStrength = saved.bassBoostStrength,
                    reverbLevel = saved.reverbLevel,
                    reverbRoom = saved.reverbRoom,
                    spatialLevel = saved.spatialLevel,
                    preampDb = saved.preampDb,
                    showPasteImportDialog = false
                )
            }
            syncDspEngine()
            onResult(saved)
        }
    }

    fun buildCurrentPresetExportJson(): String {
        val s = _uiState.value
        val snapshot = EqPresetEntity(
            name = s.activePresetName,
            category = "Custom",
            description = "Exported from Equltra",
            bandsCsv = s.bandGainsDb.joinToString(",") { String.format(Locale.US, "%.1f", it) },
            bassBoostStrength = s.bassBoostStrength,
            reverbLevel = s.reverbLevel,
            reverbRoom = s.reverbRoom,
            spatialLevel = s.spatialLevel,
            preampDb = s.preampDb
        )
        return snapshot.toExportJson()
    }

    fun deleteCustomPreset(preset: EqPresetEntity) {
        if (preset.isBuiltIn) return
        viewModelScope.launch {
            presetManager.deleteCustomSoundProfile(preset)
        }
    }

    fun toggleTestAudioLoop() {
        dspEngine.toggleTestAudioLoop()
    }

    fun setAudioRecordSpectrumEnabled(enable: Boolean) {
        dspEngine.toggleAudioRecordSpectrum(enable)
    }

    fun rehookYouTubeAudio() {
        dspEngine.forceRehookYouTubeAudio()
        if (_uiState.value.isBackgroundServiceEnabled) {
            EqForegroundService.startOrUpdate(getApplication())
        }
    }

    private fun syncDspEngine() {
        val s = _uiState.value
        presetManager.persistActiveProfileState(
            enabled = s.isMasterEnabled,
            activePresetName = s.activePresetName,
            bandGainsDb = s.bandGainsDb,
            bassBoostStrength = s.bassBoostStrength,
            reverbLevel = s.reverbLevel,
            reverbRoom = s.reverbRoom,
            spatialLevel = s.spatialLevel,
            preampDb = s.preampDb
        )
        dspEngine.updateDspState(
            enabled = s.isMasterEnabled,
            bandsDb = s.bandGainsDb,
            bass = s.bassBoostStrength,
            reverb = s.reverbLevel,
            reverbRoom = s.reverbRoom,
            spatial = s.spatialLevel,
            preamp = s.preampDb,
            limiter = s.softLimiterEnabled
        )
        if (s.isBackgroundServiceEnabled) {
            EqForegroundService.startOrUpdate(getApplication())
        }
    }

    override fun onCleared() {
        runCatching {
            getApplication<Application>().unregisterReceiver(serviceReceiver)
        }
        voiceEngine.release()
        super.onCleared()
        dspEngine.release()
    }
}
