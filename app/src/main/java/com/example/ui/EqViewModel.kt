package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EqDatabase
import com.example.data.EqPresetEntity
import com.example.data.EqRepository
import com.example.dsp.AudioDspEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EqUiState(
    val isMasterEnabled: Boolean = true,
    val selectedTab: Int = 0, // 0: EQ, 1: Bass & Spatial, 2: Presets, 3: Get APK
    val activePresetName: String = "Bass Boost",
    val bandGainsDb: List<Float> = EqPresetEntity.BUILT_IN_PRESETS[1].getBandGains(),
    val bassBoostStrength: Int = 78, // 0..100
    val reverbLevel: Int = 12, // 0..100
    val reverbRoom: String = "Small Room",
    val spatialLevel: Int = 35, // 0..100
    val preampDb: Float = -2.0f, // -12..+12 dB
    val softLimiterEnabled: Boolean = true,
    val isGlobalSystemMode: Boolean = true,
    val showSavePresetDialog: Boolean = false
)

class EqViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EqRepository
    private val dspEngine: AudioDspEngine

    private val _uiState = MutableStateFlow(EqUiState())
    val uiState: StateFlow<EqUiState> = _uiState.asStateFlow()

    val allPresets: StateFlow<List<EqPresetEntity>>
    val isPlayingTestLoop: StateFlow<Boolean>
    val spectrumBins: StateFlow<List<Float>>
    val peakDb: StateFlow<Float>
    val hardwareStatus: StateFlow<String>

    init {
        val db = EqDatabase.getInstance(application)
        repository = EqRepository(db.eqPresetDao())
        dspEngine = AudioDspEngine(viewModelScope)

        allPresets = repository.customPresets
            .combine(MutableStateFlow(EqPresetEntity.BUILT_IN_PRESETS)) { custom, builtIn ->
                builtIn + custom
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EqPresetEntity.BUILT_IN_PRESETS
            )

        isPlayingTestLoop = dspEngine.isPlayingTestLoop
        spectrumBins = dspEngine.spectrumBins
        peakDb = dspEngine.peakDb
        hardwareStatus = dspEngine.hardwareStatus

        syncDspEngine()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex.coerceIn(0, 3)) }
    }

    fun toggleMasterEnabled() {
        _uiState.update { it.copy(isMasterEnabled = !it.isMasterEnabled) }
        syncDspEngine()
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

    fun saveCurrentAsCustomPreset(name: String, description: String) {
        val trimmedName = name.trim().ifEmpty { "My Custom EQ" }
        val trimmedDesc = description.trim().ifEmpty { "User 10-band studio profile" }
        val state = _uiState.value
        val preset = EqPresetEntity(
            name = trimmedName,
            category = "Custom",
            description = trimmedDesc,
            bandsCsv = state.bandGainsDb.joinToString(",") { "%.1f".format(java.util.Locale.US, it) },
            bassBoostStrength = state.bassBoostStrength,
            reverbLevel = state.reverbLevel,
            reverbRoom = state.reverbRoom,
            spatialLevel = state.spatialLevel,
            preampDb = state.preampDb,
            isBuiltIn = false
        )
        viewModelScope.launch {
            repository.saveCustomPreset(preset)
            _uiState.update {
                it.copy(
                    activePresetName = trimmedName,
                    showSavePresetDialog = false
                )
            }
        }
    }

    fun deleteCustomPreset(preset: EqPresetEntity) {
        if (preset.isBuiltIn) return
        viewModelScope.launch {
            repository.deleteCustomPreset(preset.id)
        }
    }

    fun toggleTestAudioLoop() {
        dspEngine.toggleTestAudioLoop()
    }

    private fun syncDspEngine() {
        val s = _uiState.value
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
    }

    override fun onCleared() {
        super.onCleared()
        dspEngine.release()
    }
}
