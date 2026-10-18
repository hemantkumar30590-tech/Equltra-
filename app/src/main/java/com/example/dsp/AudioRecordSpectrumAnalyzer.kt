package com.example.dsp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time FFT Audio Spectrum Analyzer powered by the Android [AudioRecord] API.
 * Captures 16-bit PCM mono audio at 44.1 kHz, applies a Hann window, runs an in-place
 * 1024-point Radix-2 Cooley-Tukey FFT, maps magnitudes to 24 logarithmic frequency bins
 * (31 Hz – 16 kHz), and overlays the user's 10-band EQ & Bass Boost transfer curve
 * for immediate visual feedback.
 */
class AudioRecordSpectrumAnalyzer(private val scope: CoroutineScope) {

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _fftBins = MutableStateFlow(List(NUM_BINS) { 0.06f })
    val fftBins: StateFlow<List<Float>> = _fftBins.asStateFlow()

    private val _peakHoldBins = MutableStateFlow(List(NUM_BINS) { 0.08f })
    val peakHoldBins: StateFlow<List<Float>> = _peakHoldBins.asStateFlow()

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
        preampDbProvider: () -> Float,
        onStatusChanged: (String) -> Unit
    ): Boolean {
        if (_isCapturing.value) return true

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            onStatusChanged("AudioRecord Unavailable (Using Internal DSP Analyzer)")
            return false
        }

        val bufferSize = max(minBuffer, FFT_SIZE * 2)
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }.getOrNull()

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { record?.release() }
            onStatusChanged("AudioRecord Init Blocked (Using Internal DSP Analyzer)")
            return false
        }

        audioRecord = record
        _isCapturing.value = true
        onStatusChanged("AudioRecord Live 1024-Pt FFT Active (44.1kHz PCM_16BIT)")

        captureJob = scope.launch(Dispatchers.Default) {
            val pcmBuffer = ShortArray(FFT_SIZE)
            val real = FloatArray(FFT_SIZE)
            val imag = FloatArray(FFT_SIZE)
            val smoothedBins = FloatArray(NUM_BINS) { 0.06f }
            val peakHolds = FloatArray(NUM_BINS) { 0.08f }

            runCatching { record.startRecording() }

            while (isActive && _isCapturing.value) {
                val readCount = runCatching {
                    record.read(pcmBuffer, 0, FFT_SIZE)
                }.getOrDefault(-1)

                if (readCount > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until FFT_SIZE) {
                        val sample = if (i < readCount) pcmBuffer[i] / 32768.0f else 0f
                        sumSquares += (sample * sample).toDouble()
                        real[i] = sample * hannWindow[i]
                        imag[i] = 0f
                    }

                    // Compute RMS dBFS
                    val rms = sqrt(sumSquares / readCount.coerceAtLeast(1)).toFloat()
                    val preampDb = if (isMasterEnabledProvider()) preampDbProvider() else 0f
                    val dbFs = (20f * log10(rms.coerceAtLeast(0.0001f)) + preampDb).coerceIn(-60f, 3.0f)
                    _capturePeakDb.value = dbFs

                    // Execute in-place 1024-point Radix-2 FFT
                    computeRadix2Fft(real, imag)

                    // Find dominant frequency bin & map to 24 logarithmic bands (31Hz..16kHz)
                    val active = isMasterEnabledProvider()
                    val bandsDb = bandsDbProvider()
                    val bassBoost = if (active) bassStrengthProvider() else 0

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

                    val minFreq = 31.0
                    val maxFreq = 16000.0
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

                        // Apply live 10-band EQ + Bass Boost transfer function so the user
                        // visually sees their EQ adjustments shaping the captured audio spectrum!
                        val eqPos = (b.toFloat() / (NUM_BINS - 1)) * 9f
                        val eqIdx0 = eqPos.toInt().coerceIn(0, 9)
                        val eqIdx1 = min(9, eqIdx0 + 1)
                        val frac = eqPos - eqIdx0
                        val userEqDb = if (active) {
                            bandsDb[eqIdx0] * (1f - frac) + bandsDb[eqIdx1] * frac
                        } else 0f
                        val bassShelfDb = if (active && b < 6) (bassBoost / 100f) * 6.0f * (1f - b / 6f) else 0f
                        val totalEqGainLinear = 10.0.pow(((userEqDb + bassShelfDb) / 28.0).toDouble()).toFloat()

                        // Combine captured acoustic energy with subtle EQ baseline feedback
                        val eqBaseline = ((userEqDb + 15f) / 30f) * 0.22f
                        val targetLevel = (normInput * totalEqGainLinear * 0.78f + eqBaseline).coerceIn(0.05f, 0.99f)

                        // Fast attack, smooth release
                        smoothedBins[b] = if (targetLevel > smoothedBins[b]) {
                            smoothedBins[b] * 0.35f + targetLevel * 0.65f
                        } else {
                            smoothedBins[b] * 0.78f + targetLevel * 0.22f
                        }

                        // Peak-hold with gentle gravity decay
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
                record.stop()
                record.release()
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
        audioRecord = null
        _dominantFrequencyHz.value = 0
        onStatusChanged("System DSP Ready (Session 0 + 10-Band Biquad Core)")
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
        const val NUM_BINS = 24
    }
}
