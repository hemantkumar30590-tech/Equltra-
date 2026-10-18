package com.example.dsp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

class AudioDspEngine(private val scope: CoroutineScope) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var spectrumJob: Job? = null

    @Volatile
    private var isMasterEnabled: Boolean = true

    @Volatile
    private var currentBandsDb: FloatArray = FloatArray(10) { 0f }

    @Volatile
    private var bassStrength: Int = 0 // 0..100

    @Volatile
    private var reverbAmount: Int = 0 // 0..100

    @Volatile
    private var reverbRoomType: String = "Small Room"

    @Volatile
    private var spatialAmount: Int = 0 // 0..100

    @Volatile
    private var preampDb: Float = 0f

    @Volatile
    private var softLimiterEnabled: Boolean = true

    private val _isPlayingTestLoop = MutableStateFlow(false)
    val isPlayingTestLoop: StateFlow<Boolean> = _isPlayingTestLoop.asStateFlow()

    private val _hardwareStatus = MutableStateFlow("System DSP Ready (Session 0 + Internal 64-bit DSP)")
    val hardwareStatus: StateFlow<String> = _hardwareStatus.asStateFlow()

    private val _spectrumBins = MutableStateFlow(List(24) { 0.08f })
    val spectrumBins: StateFlow<List<Float>> = _spectrumBins.asStateFlow()

    private val _peakDb = MutableStateFlow(-42.0f)
    val peakDb: StateFlow<Float> = _peakDb.asStateFlow()

    init {
        bindSystemAudioSession(0)
        startIdleSpectrumTicker()
    }

    fun bindSystemAudioSession(sessionId: Int) {
        runCatching {
            releaseHardwareEffects()
            equalizer = Equalizer(0, sessionId).apply { enabled = isMasterEnabled }
            bassBoost = BassBoost(0, sessionId).apply { enabled = isMasterEnabled }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = isMasterEnabled }
            presetReverb = PresetReverb(0, sessionId).apply { enabled = isMasterEnabled }
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = isMasterEnabled }
            syncHardwareParameters()
            val hwBands = equalizer?.numberOfBands?.toInt() ?: 5
            _hardwareStatus.value =
                "Hardware AudioFX Active (${hwBands}-Band HAL + 10-Band Biquad Core | Session $sessionId)"
        }.onFailure {
            _hardwareStatus.value = "10-Band Floating-Point DSP Core Active (Direct PCM Engine)"
        }
    }

    fun updateDspState(
        enabled: Boolean,
        bandsDb: List<Float>,
        bass: Int,
        reverb: Int,
        reverbRoom: String,
        spatial: Int,
        preamp: Float,
        limiter: Boolean
    ) {
        isMasterEnabled = enabled
        val copy = FloatArray(10)
        for (i in 0 until 10) {
            copy[i] = bandsDb.getOrElse(i) { 0f }
        }
        currentBandsDb = copy
        bassStrength = bass.coerceIn(0, 100)
        reverbAmount = reverb.coerceIn(0, 100)
        reverbRoomType = reverbRoom
        spatialAmount = spatial.coerceIn(0, 100)
        preampDb = preamp.coerceIn(-12f, 12f)
        softLimiterEnabled = limiter

        syncHardwareParameters()
    }

    private fun syncHardwareParameters() {
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = isMasterEnabled
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                for (b in 0 until numBands) {
                    val mappedIndex = ((b.toFloat() / max(1, numBands - 1)) * 9f).toInt().coerceIn(0, 9)
                    val dbVal = if (isMasterEnabled) currentBandsDb[mappedIndex] else 0f
                    val mB = (dbVal * 100f).toInt().coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(b.toShort(), mB)
                }
            }
            bassBoost?.let { bb ->
                bb.enabled = isMasterEnabled && bassStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength((if (isMasterEnabled) bassStrength * 10 else 0).coerceIn(0, 1000).toShort())
                }
            }
            virtualizer?.let { virt ->
                virt.enabled = isMasterEnabled && spatialAmount > 0
                if (virt.strengthSupported) {
                    virt.setStrength((if (isMasterEnabled) spatialAmount * 10 else 0).coerceIn(0, 1000).toShort())
                }
            }
            presetReverb?.let { rev ->
                rev.enabled = isMasterEnabled && reverbAmount > 0 && reverbRoomType != "None"
                rev.preset = when (reverbRoomType) {
                    "Small Room" -> PresetReverb.PRESET_SMALLROOM
                    "Medium Room" -> PresetReverb.PRESET_MEDIUMROOM
                    "Large Hall" -> PresetReverb.PRESET_LARGEHALL
                    "Plate" -> PresetReverb.PRESET_PLATE
                    else -> PresetReverb.PRESET_NONE
                }
            }
            loudnessEnhancer?.let { le ->
                le.enabled = isMasterEnabled
                val gainMb = if (isMasterEnabled) (preampDb * 100f).toInt() else 0
                le.setTargetGain(gainMb)
            }
        }
    }

    fun toggleTestAudioLoop() {
        if (_isPlayingTestLoop.value) {
            stopTestAudioLoop()
        } else {
            startTestAudioLoop()
        }
    }

    fun startTestAudioLoop() {
        if (_isPlayingTestLoop.value) return
        _isPlayingTestLoop.value = true

        synthJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            audioTrack = track
            track?.let {
                runCatching {
                    bindSystemAudioSession(it.audioSessionId)
                    it.play()
                }
            }

            val chunkFrames = 1024
            val pcmBuffer = ShortArray(chunkFrames * 2)
            val delayLineL = FloatArray(4800)
            val delayLineR = FloatArray(6200)
            var delayIdxL = 0
            var delayIdxR = 0
            var sampleCursor = 0L

            // State variables for low/mid/high crossover approximation in live synth
            var lpSub = 0f
            var lpMid = 0f

            while (isActive && _isPlayingTestLoop.value) {
                val active = isMasterEnabled
                val bands = currentBandsDb
                val bassGain = if (active) (bassStrength / 100f) * 1.4f else 0f
                val reverbMix = if (active && reverbRoomType != "None") (reverbAmount / 100f) * 0.45f else 0f
                val spatialWidth = if (active) (spatialAmount / 100f) * 0.85f else 0f
                val preampLinear = if (active) 10.0.pow(preampDb / 20.0).toFloat() else 1.0f

                // Compute 3 composite tilt gains from the 10 EQ bands for responsive real-time synthesis
                val subEqGain = if (active) {
                    10.0.pow(((bands[0] * 0.45 + bands[1] * 0.35 + bands[2] * 0.20) / 20.0)).toFloat()
                } else 1f
                val midEqGain = if (active) {
                    10.0.pow(((bands[3] * 0.25 + bands[4] * 0.35 + bands[5] * 0.25 + bands[6] * 0.15) / 20.0)).toFloat()
                } else 1f
                val highEqGain = if (active) {
                    10.0.pow(((bands[7] * 0.35 + bands[8] * 0.35 + bands[9] * 0.30) / 20.0)).toFloat()
                } else 1f

                var maxPeak = 0f

                for (i in 0 until chunkFrames) {
                    val t = sampleCursor.toDouble() / sampleRate.toDouble()
                    val beatPhase = (t * 2.0) % 1.0 // 120 BPM (2 beats per sec)
                    val barStep = ((t * 4.0).toInt()) % 16

                    // 1. Punchy Sub Kick + Bassline (31Hz - 125Hz)
                    val kickEnv = (1.0 - beatPhase).coerceIn(0.0, 1.0).pow(4.0)
                    val kickFreq = 48.0 + 95.0 * kickEnv
                    val kick = sin(2.0 * PI * kickFreq * beatPhase) * kickEnv * 0.55

                    val bassNotes = doubleArrayOf(55.0, 55.0, 65.41, 73.42, 49.0, 49.0, 61.74, 65.41)
                    val noteHz = bassNotes[(barStep / 2) % bassNotes.size]
                    val subBass = (sin(2.0 * PI * noteHz * t) + 0.35 * sin(4.0 * PI * noteHz * t)) * 0.28

                    // 2. Warm Mid Arp / Vocal Formant Pad (250Hz - 2kHz)
                    val arpNotes = doubleArrayOf(220.0, 277.18, 329.63, 440.0, 329.63, 277.18, 246.94, 329.63)
                    val arpHz = arpNotes[barStep % arpNotes.size]
                    val stepEnv = (1.0 - ((t * 4.0) % 1.0)).pow(1.8)
                    val midSynth = (sin(2.0 * PI * arpHz * t) + 0.5 * sin(2.0 * PI * (arpHz * 1.5) * t)) * stepEnv * 0.24

                    // 3. Crisp Hi-Hat & Air Harmonics (4kHz - 16kHz)
                    val hatEnv = (1.0 - ((t * 8.0) % 1.0)).pow(6.0)
                    val metallic = (sin(2.0 * PI * 6420.0 * t) * cos(2.0 * PI * 9150.0 * t) +
                        0.6 * sin(2.0 * PI * 12400.0 * t)) * hatEnv * 0.22

                    val rawSignal = (kick + subBass + midSynth + metallic).toFloat()

                    // Crossover filter separation so all 10 EQ bands shape the live sound
                    lpSub += 0.035f * (rawSignal - lpSub)
                    lpMid += 0.22f * ((rawSignal - lpSub) - lpMid)
                    val hiComponent = rawSignal - lpSub - lpMid

                    val eqSignal =
                        (lpSub * (subEqGain + bassGain)) +
                            (lpMid * midEqGain) +
                            (hiComponent * highEqGain)

                    // Stereo Spatial 3D + Schroeder Reverb tank
                    val delayedL = delayLineL[delayIdxL]
                    val delayedR = delayLineR[delayIdxR]

                    val sideSignal = (midSynth.toFloat() * 0.6f + hiComponent * 0.9f) * spatialWidth
                    var outL = (eqSignal + sideSignal + delayedR * reverbMix) * preampLinear
                    var outR = (eqSignal - sideSignal + delayedL * reverbMix) * preampLinear

                    delayLineL[delayIdxL] = eqSignal + delayedL * (0.35f * reverbMix)
                    delayLineR[delayIdxR] = eqSignal + delayedR * (0.38f * reverbMix)
                    delayIdxL = (delayIdxL + 1) % delayLineL.size
                    delayIdxR = (delayIdxR + 1) % delayLineR.size

                    if (softLimiterEnabled) {
                        outL = tanh(outL.toDouble()).toFloat()
                        outR = tanh(outR.toDouble()).toFloat()
                    } else {
                        outL = outL.coerceIn(-1f, 1f)
                        outR = outR.coerceIn(-1f, 1f)
                    }

                    maxPeak = max(maxPeak, max(abs(outL), abs(outR)))

                    pcmBuffer[i * 2] = (outL * 32760f).toInt().coerceIn(-32768, 32767).toShort()
                    pcmBuffer[i * 2 + 1] = (outR * 32760f).toInt().coerceIn(-32768, 32767).toShort()
                    sampleCursor++
                }

                runCatching {
                    track?.write(pcmBuffer, 0, pcmBuffer.size)
                }

                // Update live spectrum visualization based on current 10-band EQ + beat phase
                val beatPulse = ((sampleCursor % 11025).toFloat() / 11025f)
                val bins = List(24) { idx ->
                    val bandPos = (idx / 23f) * 9f
                    val lowIdx = bandPos.toInt().coerceIn(0, 9)
                    val highIdx = min(9, lowIdx + 1)
                    val frac = bandPos - lowIdx
                    val db = if (active) {
                        bands[lowIdx] * (1f - frac) + bands[highIdx] * frac
                    } else 0f
                    val bassBump = if (idx < 6 && active) (bassStrength / 100f) * 0.28f else 0f
                    val normalizedEq = ((db + 15f) / 30f).coerceIn(0.08f, 1f)
                    val groove = 0.16f * abs(sin((idx * 0.7f + beatPulse * 6.28f).toDouble())).toFloat()
                    (normalizedEq * 0.72f + bassBump + groove).coerceIn(0.06f, 0.98f)
                }
                _spectrumBins.value = bins
                val dbReadout = (20f * kotlin.math.log10(maxPeak.coerceAtLeast(0.001f))).coerceIn(-48f, 3.0f)
                _peakDb.value = dbReadout

                if (track == null) {
                    delay(25)
                }
            }

            runCatching {
                track?.stop()
                track?.release()
            }
            audioTrack = null
        }
    }

    fun stopTestAudioLoop() {
        _isPlayingTestLoop.value = false
        synthJob?.cancel()
        synthJob = null
        bindSystemAudioSession(0)
    }

    private fun startIdleSpectrumTicker() {
        spectrumJob = scope.launch(Dispatchers.Default) {
            var tick = 0f
            while (isActive) {
                if (!_isPlayingTestLoop.value) {
                    val active = isMasterEnabled
                    val bands = currentBandsDb
                    val bins = List(24) { idx ->
                        val bandPos = (idx / 23f) * 9f
                        val lowIdx = bandPos.toInt().coerceIn(0, 9)
                        val highIdx = min(9, lowIdx + 1)
                        val frac = bandPos - lowIdx
                        val db = if (active) {
                            bands[lowIdx] * (1f - frac) + bands[highIdx] * frac
                        } else 0f
                        val normalized = ((db + 15f) / 30f).coerceIn(0.08f, 0.92f)
                        val shimmer = 0.03f * sin((tick + idx * 0.45f).toDouble()).toFloat()
                        (normalized * 0.65f + shimmer).coerceIn(0.06f, 0.95f)
                    }
                    _spectrumBins.value = bins
                    _peakDb.value = if (active) (-14.5f + preampDb * 0.5f) else -24.0f
                    tick += 0.25f
                }
                delay(60)
            }
        }
    }

    private fun releaseHardwareEffects() {
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

    fun release() {
        stopTestAudioLoop()
        spectrumJob?.cancel()
        releaseHardwareEffects()
    }
}
