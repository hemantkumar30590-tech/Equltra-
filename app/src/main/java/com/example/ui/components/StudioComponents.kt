package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperViolet
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class VisualizerCanvasMode(val label: String) {
    SPECTRUM("BARS"),
    WAVEFORM("WAVE"),
    RADIAL("RADIAL")
}

@Composable
fun FrequencyResponseAndSpectrumCanvas(
    bandGainsDb: List<Float>,
    spectrumBins: List<Float>,
    peakHoldBins: List<Float>,
    dominantFreqHz: Int,
    isAudioRecordActive: Boolean,
    isEnabled: Boolean,
    peakDb: Float,
    onToggleAudioRecord: () -> Unit,
    modifier: Modifier = Modifier,
    waveformSamples: List<Float> = emptyList(),
    stereoVuLevels: Pair<Float, Float> = 0.08f to 0.08f,
    systemVolumePercent: Int = 75,
    isSystemAudioActive: Boolean = false
) {
    var modeName by rememberSaveable { mutableStateOf(VisualizerCanvasMode.SPECTRUM.name) }
    val currentMode = runCatching { VisualizerCanvasMode.valueOf(modeName) }
        .getOrDefault(VisualizerCanvasMode.SPECTRUM)

    val activeColor = if (isEnabled) ElectricCyan else TextMuted
    val secondaryGlow = if (isEnabled) NeonMagenta else TextMuted

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B14), AmoledBlack)
                )
            )
            .border(
                1.dp,
                when {
                    !isEnabled -> ObsidianBorder
                    isAudioRecordActive -> NeonMagenta.copy(alpha = 0.8f)
                    isSystemAudioActive -> ElectricCyan.copy(alpha = 0.65f)
                    else -> ObsidianBorder
                },
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .testTag("realtime_audio_visualizer_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp)
        ) {
            val w = size.width
            val h = size.height
            val vuInset = 10.dp.toPx()
            val plotLeft = vuInset + 4.dp.toPx()
            val plotRight = (w - vuInset - 4.dp.toPx()).coerceAtLeast(plotLeft + 10f)
            val plotWidth = plotRight - plotLeft

            // 1. Draw Left & Right Stereo System Audio VU Meters on Canvas edges
            drawStereoVuRails(
                leftVu = if (isEnabled) stereoVuLevels.first else 0.03f,
                rightVu = if (isEnabled) stereoVuLevels.second else 0.03f,
                isEnabled = isEnabled,
                railWidth = 5.dp.toPx()
            )

            // 2. Render selected real-time Canvas visualizer mode
            when (currentMode) {
                VisualizerCanvasMode.SPECTRUM -> {
                    drawSpectrumAndEqCurve(
                        plotLeft = plotLeft,
                        plotWidth = plotWidth,
                        h = h,
                        bandGainsDb = bandGainsDb,
                        spectrumBins = spectrumBins,
                        peakHoldBins = peakHoldBins,
                        isEnabled = isEnabled,
                        isAudioRecordActive = isAudioRecordActive,
                        activeColor = activeColor,
                        secondaryGlow = secondaryGlow
                    )
                }

                VisualizerCanvasMode.WAVEFORM -> {
                    drawOscilloscopeWaveformAndEqNodes(
                        plotLeft = plotLeft,
                        plotWidth = plotWidth,
                        h = h,
                        waveformSamples = waveformSamples,
                        spectrumBins = spectrumBins,
                        bandGainsDb = bandGainsDb,
                        isEnabled = isEnabled,
                        activeColor = activeColor,
                        secondaryGlow = secondaryGlow
                    )
                }

                VisualizerCanvasMode.RADIAL -> {
                    drawRadialBassReactorCanvas(
                        plotLeft = plotLeft,
                        plotWidth = plotWidth,
                        h = h,
                        spectrumBins = spectrumBins,
                        peakHoldBins = peakHoldBins,
                        waveformSamples = waveformSamples,
                        isEnabled = isEnabled,
                        activeColor = activeColor,
                        secondaryGlow = secondaryGlow
                    )
                }
            }
        }

        // Top HUD: Live System Audio Status + Volume/dB Telemetry + Mode Switcher + Live FFT Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !isEnabled -> TextMuted
                                isAudioRecordActive -> NeonMagenta
                                isSystemAudioActive -> SignalEmerald
                                else -> ElectricCyan
                            }
                        )
                )

                Text(
                    text = when {
                        !isEnabled -> "EQ BYPASSED"
                        isAudioRecordActive -> "SYS+MIC FFT • ${dominantFreqHz}Hz"
                        isSystemAudioActive -> "SYS AUDIO • ${dominantFreqHz}Hz"
                        else -> "DSP ACTIVE • ${dominantFreqHz}Hz"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = when {
                        !isEnabled -> TextMuted
                        isAudioRecordActive -> NeonMagenta
                        else -> ElectricCyan
                    },
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )

                Text(
                    text = String.format(Locale.US, "%+.1fdB • VOL %d%%", peakDb, systemVolumePercent),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (!isEnabled) TextMuted else if (peakDb > -3f) StudioAmber else SignalEmerald,
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 3-Mode Real-Time Canvas Visualizer Switcher (BARS / WAVE / RADIAL)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(7.dp))
                        .padding(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    VisualizerCanvasMode.entries.forEach { mode ->
                        val selected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (selected) ElectricCyan else Color.Transparent)
                                .clickable { modeName = mode.name }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                .testTag("visualizer_mode_${mode.name}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = if (selected) AmoledBlack else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Live AudioRecord / System Visualizer FFT Capture Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAudioRecordActive) NeonMagenta else ObsidianCard)
                        .border(
                            1.dp,
                            if (isAudioRecordActive) NeonMagenta else ElectricCyan,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onToggleAudioRecord)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .testTag("audiorecord_fft_toggle_btn")
                ) {
                    Text(
                        text = if (isAudioRecordActive) "● LIVE" else "MIC FFT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isAudioRecordActive) Color.White else ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Frequency / Stereo Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "L",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = if (isEnabled) SignalEmerald else TextMuted,
                fontWeight = FontWeight.Bold
            )
            listOf("31Hz", "125Hz", "500Hz", "2kHz", "8kHz", "16kHz").forEach { freq ->
                Text(
                    text = freq,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextSecondary
                )
            }
            Text(
                text = "R",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = if (isEnabled) SignalEmerald else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun DrawScope.drawStereoVuRails(
    leftVu: Float,
    rightVu: Float,
    isEnabled: Boolean,
    railWidth: Float
) {
    val w = size.width
    val h = size.height
    val segments = 14
    val segGap = 2.dp.toPx()
    val segHeight = ((h - segGap * (segments - 1)) / segments).coerceAtLeast(2f)

    val leftActiveSegs = (leftVu.coerceIn(0f, 1f) * segments).toInt()
    val rightActiveSegs = (rightVu.coerceIn(0f, 1f) * segments).toInt()

    for (s in 0 until segments) {
        val segIndexFromBottom = segments - 1 - s
        val y = s * (segHeight + segGap)
        val segColor = when {
            !isEnabled -> Color(0xFF1A2234)
            segIndexFromBottom >= segments - 2 -> NeonMagenta
            segIndexFromBottom >= segments - 5 -> StudioAmber
            else -> SignalEmerald
        }

        // Left VU segment
        drawRoundRect(
            color = if (isEnabled && segIndexFromBottom < leftActiveSegs) segColor else Color(0xFF141B29),
            topLeft = Offset(0f, y),
            size = Size(railWidth, segHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Right VU segment
        drawRoundRect(
            color = if (isEnabled && segIndexFromBottom < rightActiveSegs) segColor else Color(0xFF141B29),
            topLeft = Offset(w - railWidth, y),
            size = Size(railWidth, segHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

private fun DrawScope.drawSpectrumAndEqCurve(
    plotLeft: Float,
    plotWidth: Float,
    h: Float,
    bandGainsDb: List<Float>,
    spectrumBins: List<Float>,
    peakHoldBins: List<Float>,
    isEnabled: Boolean,
    isAudioRecordActive: Boolean,
    activeColor: Color,
    secondaryGlow: Color
) {
    val midY = h / 2f

    // Horizontal dB grid lines (+15, +7.5, 0, -7.5, -15)
    val gridSteps = 4
    for (i in 0..gridSteps) {
        val y = (h / gridSteps) * i
        val isZeroLine = i == gridSteps / 2
        drawLine(
            color = if (isZeroLine) Color(0xFF28354E) else Color(0xFF141B29),
            start = Offset(plotLeft, y),
            end = Offset(plotLeft + plotWidth, y),
            strokeWidth = if (isZeroLine) 1.5.dp.toPx() else 1.dp.toPx()
        )
    }

    // Real-time 24-bin FFT spectrum bars + floating Amber Peak-Hold caps
    val binCount = spectrumBins.size.coerceAtLeast(1)
    val barSpacing = plotWidth / binCount
    val barWidth = (barSpacing * 0.66f).coerceAtLeast(3.5f)
    for (i in spectrumBins.indices) {
        val amp = if (isEnabled) spectrumBins[i].coerceIn(0.04f, 1f) else 0.04f
        val barHeight = amp * h * 0.90f
        val x = plotLeft + i * barSpacing + (barSpacing - barWidth) / 2f
        val y = h - barHeight
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (isAudioRecordActive) NeonMagenta.copy(alpha = 0.72f) else activeColor.copy(alpha = 0.56f),
                    activeColor.copy(alpha = 0.36f),
                    HyperViolet.copy(alpha = 0.20f),
                    Color.Transparent
                ),
                startY = y,
                endY = h
            ),
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Floating Peak-Hold cap above each FFT bar
        val peakHold = if (isEnabled) {
            peakHoldBins.getOrElse(i) { amp }.coerceIn(amp, 1f)
        } else 0.05f
        val peakY = (h - peakHold * h * 0.90f).coerceIn(2f, h - 3f)
        drawLine(
            color = if (isEnabled) StudioAmber else TextMuted,
            start = Offset(x, peakY),
            end = Offset(x + barWidth, peakY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Compute 10-band control points for EQ response curve
    if (bandGainsDb.size >= 10) {
        val points = List(10) { idx ->
            val x = plotLeft + (idx.toFloat() / 9f) * plotWidth
            val db = if (isEnabled) bandGainsDb[idx].coerceIn(-15f, 15f) else 0f
            val normalized = (db / 15f)
            val y = (midY - normalized * (midY * 0.82f)).coerceIn(6f, h - 6f)
            Offset(x, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val ctrlX = (p0.x + p1.x) / 2f
                cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.26f),
                    secondaryGlow.copy(alpha = 0.08f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = curvePath,
            color = activeColor,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { idx, pt ->
            val nodeColor = if (bandGainsDb[idx] > 4f && isEnabled) NeonMagenta else activeColor
            drawCircle(
                color = nodeColor.copy(alpha = 0.3f),
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = nodeColor,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

private fun DrawScope.drawOscilloscopeWaveformAndEqNodes(
    plotLeft: Float,
    plotWidth: Float,
    h: Float,
    waveformSamples: List<Float>,
    spectrumBins: List<Float>,
    bandGainsDb: List<Float>,
    isEnabled: Boolean,
    activeColor: Color,
    secondaryGlow: Color
) {
    val midY = h / 2f

    // Grid lines
    val gridSteps = 4
    for (i in 0..gridSteps) {
        val y = (h / gridSteps) * i
        val isZeroLine = i == gridSteps / 2
        drawLine(
            color = if (isZeroLine) Color(0xFF28354E) else Color(0xFF141B29),
            start = Offset(plotLeft, y),
            end = Offset(plotLeft + plotWidth, y),
            strokeWidth = if (isZeroLine) 1.5.dp.toPx() else 1.dp.toPx()
        )
    }

    val samples = if (waveformSamples.isNotEmpty()) {
        waveformSamples
    } else {
        List(64) { i ->
            if (!isEnabled) 0f
            else {
                val bin = spectrumBins.getOrElse(i % spectrumBins.size.coerceAtLeast(1)) { 0.1f }
                (sin(i * 0.35f) * bin * 0.8f)
            }
        }
    }

    val count = samples.size.coerceAtLeast(2)
    val primaryPath = Path()
    val harmonicPath = Path()
    val ribbonFill = Path()

    for (i in 0 until count) {
        val x = plotLeft + (i.toFloat() / (count - 1)) * plotWidth
        val s = if (isEnabled) samples[i].coerceIn(-1f, 1f) else 0f
        val yPrimary = (midY - s * (midY * 0.85f)).coerceIn(4f, h - 4f)

        val sHarmonic = if (isEnabled) {
            val neighbor = samples[(i + 3) % count].coerceIn(-1f, 1f)
            (s * 0.45f - neighbor * 0.55f)
        } else 0f
        val yHarmonic = (midY - sHarmonic * (midY * 0.65f)).coerceIn(4f, h - 4f)

        if (i == 0) {
            primaryPath.moveTo(x, yPrimary)
            harmonicPath.moveTo(x, yHarmonic)
            ribbonFill.moveTo(x, midY)
            ribbonFill.lineTo(x, yPrimary)
        } else {
            val prevX = plotLeft + ((i - 1).toFloat() / (count - 1)) * plotWidth
            val prevS = if (isEnabled) samples[i - 1].coerceIn(-1f, 1f) else 0f
            val prevY = (midY - prevS * (midY * 0.85f)).coerceIn(4f, h - 4f)
            val ctrlX = (prevX + x) / 2f
            primaryPath.cubicTo(ctrlX, prevY, ctrlX, yPrimary, x, yPrimary)
            harmonicPath.lineTo(x, yHarmonic)
            ribbonFill.lineTo(x, yPrimary)
        }
    }

    ribbonFill.lineTo(plotLeft + plotWidth, midY)
    ribbonFill.close()

    drawPath(
        path = ribbonFill,
        brush = Brush.verticalGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.25f),
                secondaryGlow.copy(alpha = 0.18f),
                activeColor.copy(alpha = 0.25f)
            )
        )
    )

    drawPath(
        path = harmonicPath,
        color = secondaryGlow.copy(alpha = if (isEnabled) 0.65f else 0.25f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    drawPath(
        path = primaryPath,
        color = activeColor,
        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Subtle 10-band EQ reference markers along the oscilloscope
    if (bandGainsDb.size >= 10) {
        for (idx in 0 until 10) {
            val x = plotLeft + (idx.toFloat() / 9f) * plotWidth
            val db = if (isEnabled) bandGainsDb[idx].coerceIn(-15f, 15f) else 0f
            val y = (midY - (db / 15f) * (midY * 0.80f)).coerceIn(6f, h - 6f)
            drawCircle(
                color = StudioAmber.copy(alpha = if (isEnabled) 0.75f else 0.25f),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawRadialBassReactorCanvas(
    plotLeft: Float,
    plotWidth: Float,
    h: Float,
    spectrumBins: List<Float>,
    peakHoldBins: List<Float>,
    waveformSamples: List<Float>,
    isEnabled: Boolean,
    activeColor: Color,
    secondaryGlow: Color
) {
    val cx = plotLeft + plotWidth / 2f
    val cy = h / 2f
    val maxRadius = (min(plotWidth, h) * 0.46f).coerceAtLeast(24f)

    // Compute sub-bass & overall energy for reactor core pulse
    val bassEnergy = if (isEnabled && spectrumBins.isNotEmpty()) {
        spectrumBins.take(6).average().toFloat().coerceIn(0.05f, 1f)
    } else 0.05f

    val baseCoreRadius = maxRadius * (0.28f + bassEnergy * 0.18f)

    // Outer ambient glow ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondaryGlow.copy(alpha = if (isEnabled) 0.32f * bassEnergy else 0.05f),
                activeColor.copy(alpha = if (isEnabled) 0.16f else 0.02f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = maxRadius
        ),
        radius = maxRadius,
        center = Offset(cx, cy)
    )

    // 24 radial frequency bars around the circle (mirrored symmetrically for 48 spokes)
    val binCount = spectrumBins.size.coerceAtLeast(1)
    val totalSpokes = binCount * 2
    for (s in 0 until totalSpokes) {
        val binIdx = if (s < binCount) s else (totalSpokes - 1 - s)
        val amp = if (isEnabled) spectrumBins[binIdx].coerceIn(0.05f, 1f) else 0.05f
        val peak = if (isEnabled) peakHoldBins.getOrElse(binIdx) { amp }.coerceIn(amp, 1f) else 0.06f

        val angleRad = (s.toFloat() / totalSpokes) * 2.0 * PI - (PI / 2.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val rStart = baseCoreRadius + 3.dp.toPx()
        val rEnd = rStart + amp * (maxRadius - rStart)
        val rPeak = rStart + peak * (maxRadius - rStart)

        val spokeColor = when {
            !isEnabled -> TextMuted
            binIdx < 6 -> NeonMagenta
            binIdx < 15 -> activeColor
            else -> HyperViolet
        }

        drawLine(
            color = spokeColor,
            start = Offset(cx + cosA * rStart, cy + sinA * rStart),
            end = Offset(cx + cosA * rEnd, cy + sinA * rEnd),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = if (isEnabled) StudioAmber else TextMuted,
            radius = 1.8.dp.toPx(),
            center = Offset(cx + cosA * rPeak, cy + sinA * rPeak)
        )
    }

    // Inner waveform ring
    if (waveformSamples.isNotEmpty() && isEnabled) {
        val ringPath = Path()
        val count = waveformSamples.size
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2.0 * PI
            val r = (baseCoreRadius * 0.72f) + waveformSamples[i] * (baseCoreRadius * 0.22f)
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
        }
        ringPath.close()
        drawPath(
            path = ringPath,
            color = ElectricCyan.copy(alpha = 0.85f),
            style = Stroke(width = 1.8.dp.toPx())
        )
    }

    // Core border ring
    drawCircle(
        color = activeColor,
        radius = baseCoreRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx())
    )
}

/**
 * Dedicated Real-Time System Audio Level & Stereo Spatial Visualizer Card
 * for the Bass & DSP tab, responding to system audio levels, sub-bass energy,
 * and 3D spatial width when the equalizer is active.
 */
@Composable
fun SystemAudioLevelVisualizerCard(
    isEnabled: Boolean,
    spectrumBins: List<Float>,
    waveformSamples: List<Float>,
    stereoVuLevels: Pair<Float, Float>,
    systemVolumePercent: Int,
    peakDb: Float,
    bassBoostPercent: Int,
    spatialPercent: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dsp_system_audio_visualizer_card"),
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) ElectricCyan.copy(alpha = 0.45f) else ObsidianBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) SignalEmerald else TextMuted)
                    )
                    Text(
                        text = if (isEnabled) {
                            "LIVE SYSTEM AUDIO LEVEL & SUB-BASS MONITOR"
                        } else {
                            "SYSTEM AUDIO MONITOR (EQ BYPASSED)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEnabled) ElectricCyan else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = String.format(Locale.US, "SYS VOL %d%% • %+.1f dB", systemVolumePercent, peakDb),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isEnabled) StudioAmber else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Background center axis
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Symmetrical mirrored stereo waveform bars across width
                val barCount = 32
                val spacing = w / barCount
                val barW = (spacing * 0.62f).coerceAtLeast(3f)
                for (i in 0 until barCount) {
                    val binIdx = ((i.toFloat() / barCount) * (spectrumBins.size - 1).coerceAtLeast(1)).toInt()
                    val binAmp = if (isEnabled) spectrumBins.getOrElse(binIdx) { 0.08f } else 0.04f
                    val waveMod = if (isEnabled && waveformSamples.isNotEmpty()) {
                        abs(waveformSamples[i % waveformSamples.size]) * 0.35f
                    } else 0f
                    val totalAmp = (binAmp * 0.75f + waveMod).coerceIn(0.05f, 0.98f)
                    val halfBarH = totalAmp * (h * 0.45f)
                    val x = i * spacing + (spacing - barW) / 2f

                    val barColor = when {
                        !isEnabled -> TextMuted
                        i < 10 -> NeonMagenta
                        i < 22 -> ElectricCyan
                        else -> HyperViolet
                    }

                    drawRoundRect(
                        color = barColor.copy(alpha = if (isEnabled) 0.85f else 0.3f),
                        topLeft = Offset(x, midY - halfBarH),
                        size = Size(barW, halfBarH * 2f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal L/R Stereo System VU Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leftPct = if (isEnabled) (stereoVuLevels.first * 100).toInt().coerceIn(0, 100) else 0
                val rightPct = if (isEnabled) (stereoVuLevels.second * 100).toInt().coerceIn(0, 100) else 0
                Text(
                    text = "L $leftPct%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = if (isEnabled) SignalEmerald else TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BASS ${if (isEnabled) bassBoostPercent else 0}% • 3D ${if (isEnabled) spatialPercent else 0}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "R $rightPct%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = if (isEnabled) SignalEmerald else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VerticalEqBandFader(
    bandIndex: Int,
    frequencyLabel: String,
    gainDb: Float,
    isEnabled: Boolean,
    onGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = when {
        !isEnabled -> TextMuted
        gainDb > 5.0f -> NeonMagenta
        gainDb < -5.0f -> HyperViolet
        else -> ElectricCyan
    }

    Column(
        modifier = modifier
            .width(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // dB readout at top
        Text(
            text = String.format(Locale.US, "%+.1f", gainDb),
            style = MaterialTheme.typography.labelSmall,
            color = if (gainDb != 0f && isEnabled) TextPrimary else TextSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive vertical studio fader track (drag or tap)
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(176.dp)
                .minimumInteractiveComponentSize()
                .testTag("band_slider_$bandIndex")
                .pointerInput(bandIndex, isEnabled) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val y = change.position.y.coerceIn(0f, h)
                        val normalized = 1f - (y / h) // 0 at bottom (-15dB), 1 at top (+15dB)
                        val newDb = (normalized * 30f) - 15f
                        onGainChanged(newDb)
                    }
                }
                .clickable(
                    onClickLabel = "Reset $frequencyLabel band to 0 dB"
                ) {
                    // Single tap cycles or resets to 0dB if already boosted, or +3dB if at 0dB
                    if (gainDb != 0f) onGainChanged(0f) else onGainChanged(3.0f)
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val midY = h / 2f

                // Scale ticks on sides
                val ticks = 9
                for (t in 0 until ticks) {
                    val ty = (h / (ticks - 1)) * t
                    val tickW = if (t == ticks / 2) 10.dp.toPx() else 5.dp.toPx()
                    drawLine(
                        color = if (t == ticks / 2) Color(0xFF475569) else Color(0xFF1E293B),
                        start = Offset(centerX - tickW - 4.dp.toPx(), ty),
                        end = Offset(centerX + tickW + 4.dp.toPx(), ty),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Background slot track
                drawRoundRect(
                    color = AmoledBlack,
                    topLeft = Offset(centerX - 3.dp.toPx(), 4.dp.toPx()),
                    size = Size(6.dp.toPx(), h - 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Active fill from 0dB center line to current gain
                val norm = ((gainDb.coerceIn(-15f, 15f) + 15f) / 30f) // 0..1
                val thumbY = ((1f - norm) * (h - 20.dp.toPx())) + 10.dp.toPx()
                val topFill = minOf(midY, thumbY)
                val fillHeight = abs(thumbY - midY).coerceAtLeast(2f)

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(centerX - 3.dp.toPx(), topFill),
                    size = Size(6.dp.toPx(), fillHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Hardware fader cap
                val capW = 30.dp.toPx()
                val capH = 15.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF1A2234),
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Center indicator line on fader cap
                drawLine(
                    color = TextPrimary,
                    start = Offset(centerX - 8.dp.toPx(), thumbY),
                    end = Offset(centerX + 8.dp.toPx(), thumbY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Frequency label at bottom
        Text(
            text = frequencyLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StudioRotaryKnobCard(
    title: String,
    subtitle: String,
    valuePercent: Int,
    accentColor: Color,
    testTag: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (valuePercent > 0) accentColor else TextMuted)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rotary gauge visualization
            Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 8.dp.toPx()
                    val arcSize = size.minDimension - strokeW * 2
                    val topLeft = Offset(strokeW, strokeW)
                    val sweepMax = 270f
                    val startAngle = 135f

                    // Track arc
                    drawArc(
                        color = AmoledBlack,
                        startAngle = startAngle,
                        sweepAngle = sweepMax,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active value arc
                    val activeSweep = (valuePercent.coerceIn(0, 100) / 100f) * sweepMax
                    drawArc(
                        color = accentColor,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Inner metallic dial circle
                    val dialRadius = (arcSize / 2f) - 10.dp.toPx()
                    drawCircle(
                        color = Color(0xFF070A10),
                        radius = dialRadius,
                        center = center
                    )
                    drawCircle(
                        color = ObsidianBorder,
                        radius = dialRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Pointer notch
                    val angleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val indicatorStart = Offset(
                        x = center.x + (dialRadius * 0.35f) * cos(angleRad).toFloat(),
                        y = center.y + (dialRadius * 0.35f) * sin(angleRad).toFloat()
                    )
                    val indicatorEnd = Offset(
                        x = center.x + (dialRadius * 0.82f) * cos(angleRad).toFloat(),
                        y = center.y + (dialRadius * 0.82f) * sin(angleRad).toFloat()
                    )
                    drawLine(
                        color = accentColor,
                        start = indicatorStart,
                        end = indicatorEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$valuePercent%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (valuePercent == 0) "BYPASS" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (valuePercent > 0) accentColor else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = valuePercent.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = AmoledBlack
                )
            )
        }
    }
}
