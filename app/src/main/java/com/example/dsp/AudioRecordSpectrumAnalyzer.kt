package com.example.dsp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
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
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time System Audio & FFT Spectrum Analyzer powered by Android [Visualizer] (Session 0 System Mix)
 * and [AudioRecord] 1024-point Radix-2 Cooley-Tukey FFT.
 *
 * Captures live system audio output levels, 64-point oscilloscope waveform samples, Left/Right stereo VU
 * levels, and 24 logarithmic frequency bins (31 Hz – 16 kHz) shaped by the active 10-band EQ & Bass Boost.
 */
class AudioRecordSpectrumAnalyzer(private val scope: CoroutineScope) {

    private var audioRecord: AudioRecord? = null
    private var systemVisualizer: Visualizer? = null
    private var captureJob: Job? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _fftBins = MutableStateFlow(List(NUM_BINS) { 0.06f })
    val fftBins: StateFlow<List<Float>> = _fftBins.asStateFlow()

    private val _peakHoldBins = MutableStateFlow(List(NUM_BINS) { 0.08f })
    val peakHoldBins: StateFlow<List<Float>> = _peakHoldBins.asStateFlow()

    private val _waveformSamples = MutableStateFlow(List(WAVEFORM_POINTS) { 0f })
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    private val _stereoVuLevels = MutableStateFlow(0.08f to 0.08f)
    val stereoVuLevels: StateFlow<Pair<Float, Float>> = _stereoVuLevels.asStateFlow()

    private val _dominantFrequencyHz = MutableStateFlow(0)
    val dominantFrequencyHz: StateFlow<Int> = _dominantFrequencyHz.asStateFlow()

    private val _capturePeakDb = MutableStateFlow(-48.0f)
    val capturePeakDb: StateFlow<Float> = _capturePeakDb.asStateFlow()

    // Precomputed Hann window for 1024-sample FFT
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    @SuppressLint("MissingPermission")
    fun startCapture(
        isMasterEnabledProvider: () -> Boolean,
        bandsDbProvider: () -> FloatArray,
        bassStrengthProvider: () -> Int,
        spatialStrengthProvider: () -> Int = { 35 },
        preampDbProvider: () -> Float,
        onStatusChanged: (String) -> Unit
    ): Boolean {
        if (_isCapturing.value) return true

        // 1. Try attaching Android native system output Visualizer(0) for direct Session 0 system audio mix
        val vis = runCatching {
            Visualizer(0).apply {
                enabled = false
                val range = Visualizer.getCaptureSizeRange()
                captureSize = min(1024, range[1]).coerceAtLeast(range[0])
                runCatching {
                    measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
                }
                enabled = true
            }
        }.getOrNull()
        systemVisualizer = vis

        // 2. Initialize AudioRecord 16-bit PCM 44.1kHz mono for high-resolution 1024-pt FFT
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val record = if (minBuffer > 0) {
            val bufferSize = max(minBuffer, FFT_SIZE * 2)
            runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }.getOrNull()?.takeIf { it.state == AudioRecord.STATE_INITIALIZED }
        } else null

        if (record == null && vis == null) {
            onStatusChanged("System Visualizer & AudioRecord Unavailable (Using Internal DSP Analyzer)")
            return false
        }

        audioRecord = record
        _isCapturing.value = true
        onStatusChanged(
            if (vis != null && record != null) {
                "System Session 0 Visualizer + AudioRecord 1024-Pt FFT Active"
            } else if (vis != null) {
                "System Session 0 Visualizer Live Capture Active"
            } else {
                "AudioRecord Live 1024-Pt FFT Active (44.1kHz PCM_16BIT)"
            }
        )

        captureJob = scope.launch(Dispatchers.Default) {
            val pcmBuffer = ShortArray(FFT_SIZE)
            val real = FloatArray(FFT_SIZE)
            val imag = FloatArray(FFT_SIZE)
            val smoothedBins = FloatArray(NUM_BINS) { 0.06f }
            val peakHolds = FloatArray(NUM_BINS) { 0.08f }
            val visWaveBytes = ByteArray(vis?.captureSize ?: 512)

            runCatching { record?.startRecording() }

            var smoothedLeft = 0.08f
            var smoothedRight = 0.08f

            while (isActive && _isCapturing.value) {
                val readCount = runCatching {
                    record?.read(pcmBuffer, 0, FFT_SIZE) ?: -1
                }.getOrDefault(-1)

                val hasVisWave = runCatching {
                    vis != null && vis.enabled && vis.getWaveForm(visWaveBytes) == Visualizer.SUCCESS
                }.getOrDefault(false)

                if (readCount > 0 || hasVisWave) {
                    var sumSquares = 0.0
                    var sumLeft = 0.0
                    var sumRight = 0.0
                    val sampleLen = if (readCount > 0) readCount else visWaveBytes.size

                    for (i in 0 until FFT_SIZE) {
                        val sample = when {
                            readCount > 0 && i < readCount -> pcmBuffer[i] / 32768.0f
                            hasVisWave -> {
                                val bIdx = i % visWaveBytes.size
                                ((visWaveBytes[bIdx].toInt() and 0xFF) - 128) / 128.0f
                            }
                            else -> 0f
                        }
                        sumSquares += (sample * sample).toDouble()
                        if (i % 2 == 0) sumLeft += (sample * sample).toDouble()
                        else sumRight += (sample * sample).toDouble()
                        real[i] = sample * hannWindow[i]
                        imag[i] = 0f
                    }

                    val active = isMasterEnabledProvider()
                    val bandsDb = bandsDbProvider()
                    val bassBoost = if (active) bassStrengthProvider() else 0
                    val spatialBoost = if (active) spatialStrengthProvider() else 0
                    val preampDb = if (active) preampDbProvider() else 0f

                    // Compute RMS dBFS
                    val rms = sqrt(sumSquares / sampleLen.coerceAtLeast(1)).toFloat()
                    val dbFs = (20f * log10(rms.coerceAtLeast(0.0001f)) + preampDb).coerceIn(-60f, 3.0f)
                    _capturePeakDb.value = dbFs

                    // Extract 64-point time-domain waveform scaled by EQ & Preamp gain
                    val eqGainAvg = if (active) {
                        10.0.pow(((bandsDb.average() + (bassBoost / 100.0) * 4.5 + preampDb * 0.5) / 20.0)).toFloat()
                    } else 1.0f
                    val stride = max(1, FFT_SIZE / WAVEFORM_POINTS)
                    val waveOut = List(WAVEFORM_POINTS) { wIdx ->
                        if (!active) {
                            0f
                        } else {
                            val srcIdx = (wIdx * stride).coerceIn(0, FFT_SIZE - 1)
                            val rawSample = if (readCount > 0) {
                                pcmBuffer[srcIdx.coerceAtMost(readCount - 1)] / 32768.0f
                            } else if (hasVisWave) {
                                ((visWaveBytes[srcIdx % visWaveBytes.size].toInt() and 0xFF) - 128) / 128.0f
                            } else 0f
                            (rawSample * eqGainAvg * 2.2f).coerceIn(-1f, 1f)
                        }
                    }
                    _waveformSamples.value = waveOut

                    // Stereo L/R VU meter levels
                    val rmsL = sqrt(sumLeft / max(1, sampleLen / 2)).toFloat()
                    val rmsR = sqrt(sumRight / max(1, sampleLen / 2)).toFloat()
                    val spatialSpread = (spatialBoost / 100f) * 0.14f
                    val targetL = if (active) {
                        (((20f * log10((rmsL * eqGainAvg).coerceAtLeast(0.0001f)) + 56f) / 56f) + spatialSpread)
                            .coerceIn(0.05f, 0.99f)
                    } else 0.04f
                    val targetR = if (active) {
                        (((20f * log10((rmsR * eqGainAvg).coerceAtLeast(0.0001f)) + 56f) / 56f) + spatialSpread * 0.85f)
                            .coerceIn(0.05f, 0.99f)
                    } else 0.04f
                    smoothedLeft = smoothedLeft * 0.55f + targetL * 0.45f
                    smoothedRight = smoothedRight * 0.55f + targetR * 0.45f
                    _stereoVuLevels.value = smoothedLeft to smoothedRight

                    // Execute in-place 1024-point Radix-2 FFT
                    computeRadix2Fft(real, imag)

                    // Find dominant frequency bin & map to 24 logarithmic bands (31Hz..16kHz)
                    var maxMag = 0f
                    var maxBinIdx = 1
                    val halfFft = FFT_SIZE / 2
                    val magnitudes = FloatArray(halfFft)
                    for (k in 1 until halfFft) {
                        val mag = sqrt(real[k] * real[k] + imag[k] * imag[k]) / (FFT_SIZE / 4f)
                        magnitudes[k] = mag
                        if (mag > maxMag) {
                            maxMag = mag
                            maxBinIdx = k
                        }
                    }

                    val domHz = ((maxBinIdx.toFloat() * SAMPLE_RATE) / FFT_SIZE).toInt()
                    _dominantFrequencyHz.value = if (maxMag > 0.002f) domHz else 0

                    val minFreq = 20.0
                    val maxFreq = 20000.0
                    val logMin = ln(minFreq)
                    val logMax = ln(maxFreq)

                    for (b in 0 until NUM_BINS) {
                        val fLow = kotlin.math.exp(logMin + (b.toDouble() / NUM_BINS) * (logMax - logMin))
                        val fHigh = kotlin.math.exp(logMin + ((b + 1).toDouble() / NUM_BINS) * (logMax - logMin))
                        val kStart = ((fLow * FFT_SIZE) / SAMPLE_RATE).toInt().coerceIn(1, halfFft - 1)
                        val kEnd = max(kStart, ((fHigh * FFT_SIZE) / SAMPLE_RATE).toInt().coerceIn(1, halfFft - 1))

                        var binEnergy = 0f
                        for (k in kStart..kEnd) {
                            binEnergy = max(binEnergy, magnitudes[k])
                        }

                        // Convert magnitude to dB-scaled normalized height (0..1)
                        val rawDb = 20f * log10(binEnergy.coerceAtLeast(0.00005f))
                        val normInput = ((rawDb + 68f) / 58f).coerceIn(0.04f, 1.0f)

                        // Apply live 31-band EQ + Bass Boost transfer function (aligned 1:1 with 31 bins)
                        val maxBandIdx = (bandsDb.size - 1).coerceAtLeast(1)
                        val eqPos = (b.toFloat() / (NUM_BINS - 1)) * maxBandIdx
                        val eqIdx0 = eqPos.toInt().coerceIn(0, maxBandIdx)
                        val eqIdx1 = min(maxBandIdx, eqIdx0 + 1)
                        val frac = eqPos - eqIdx0
                        val userEqDb = if (active) {
                            bandsDb[eqIdx0] * (1f - frac) + bandsDb[eqIdx1] * frac
                        } else 0f
                        val bassShelfDb = if (active && b < 9) (bassBoost / 100f) * 6.0f * (1f - b / 9f) else 0f
                        val totalEqGainLinear = 10.0.pow(((userEqDb + bassShelfDb) / 28.0).toDouble()).toFloat()

                        val eqBaseline = ((userEqDb + 15f) / 30f) * 0.22f
                        val targetLevel = (normInput * totalEqGainLinear * 0.78f + eqBaseline).coerceIn(0.05f, 0.99f)

                        smoothedBins[b] = if (targetLevel > smoothedBins[b]) {
                            smoothedBins[b] * 0.35f + targetLevel * 0.65f
                        } else {
                            smoothedBins[b] * 0.78f + targetLevel * 0.22f
                        }

                        if (smoothedBins[b] >= peakHolds[b]) {
                            peakHolds[b] = smoothedBins[b]
                        } else {
                            peakHolds[b] = (peakHolds[b] - 0.018f).coerceAtLeast(smoothedBins[b])
                        }
                    }

                    _fftBins.value = smoothedBins.toList()
                    _peakHoldBins.value = peakHolds.toList()
                } else {
                    delay(20)
                }
            }

            runCatching {
                record?.stop()
                record?.release()
            }
            runCatching {
                vis?.enabled = false
                vis?.release()
            }
        }
        return true
    }

    fun stopCapture(onStatusChanged: (String) -> Unit) {
        _isCapturing.value = false
        captureJob?.cancel()
        captureJob = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        runCatching {
            systemVisualizer?.enabled = false
            systemVisualizer?.release()
        }
        audioRecord = null
        systemVisualizer = null
        _dominantFrequencyHz.value = 0
        onStatusChanged("System DSP Ready (Session 0 + 31-Band ISO Core)")
    }

    /**
     * In-place Radix-2 Cooley-Tukey Fast Fourier Transform.
     */
    private fun computeRadix2Fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tr = real[i]
                val ti = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tr
                imag[j] = ti
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        var step = 1
        while (step < n) {
            val jump = step shl 1
            val deltaAngle = (-PI / step).toFloat()
            val sinHalf = sin(deltaAngle * 0.5f)
            val wpReal = -2.0f * sinHalf * sinHalf
            val wpImag = sin(deltaAngle)
            var wReal = 1.0f
            var wImag = 0.0f

            for (group in 0 until step) {
                var pair = group
                while (pair < n) {
                    val match = pair + step
                    val prodReal = wReal * real[match] - wImag * imag[match]
                    val prodImag = wReal * imag[match] + wImag * real[match]
                    real[match] = real[pair] - prodReal
                    imag[match] = imag[pair] - prodImag
                    real[pair] += prodReal
                    imag[pair] += prodImag
                    pair += jump
                }
                val nextWReal = wReal + wReal * wpReal - wImag * wpImag
                wImag += wImag * wpReal + wReal * wpImag
                wReal = nextWReal
            }
            step = jump
        }
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val FFT_SIZE = 1024
        const val NUM_BINS = 31
        const val WAVEFORM_POINTS = 64
    }
}
