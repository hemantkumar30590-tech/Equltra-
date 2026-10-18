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
import androidx.compose.foundation.layout.fillMaxHeight
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FrequencyResponseAndSpectrumCanvas(
    bandGainsDb: List<Float>,
    spectrumBins: List<Float>,
    isEnabled: Boolean,
    peakDb: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isEnabled) ElectricCyan else TextMuted
    val secondaryGlow = if (isEnabled) NeonMagenta else TextMuted

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B14), AmoledBlack)
                )
            )
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Horizontal dB grid lines (+15, +7.5, 0, -7.5, -15)
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = (h / gridSteps) * i
                val isZeroLine = i == gridSteps / 2
                drawLine(
                    color = if (isZeroLine) Color(0xFF28354E) else Color(0xFF141B29),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (isZeroLine) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // Real-time 24-bin spectrum bars in background
            val binCount = spectrumBins.size.coerceAtLeast(1)
            val barSpacing = w / binCount
            val barWidth = (barSpacing * 0.62f).coerceAtLeast(4f)
            for (i in spectrumBins.indices) {
                val amp = if (isEnabled) spectrumBins[i].coerceIn(0.04f, 1f) else 0.05f
                val barHeight = amp * h * 0.86f
                val x = i * barSpacing + (barSpacing - barWidth) / 2f
                val y = h - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.42f),
                            HyperViolet.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startY = y,
                        endY = h
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Compute 10-band control points
            if (bandGainsDb.size >= 10) {
                val points = List(10) { idx ->
                    val x = (idx.toFloat() / 9f) * w
                    val db = if (isEnabled) bandGainsDb[idx].coerceIn(-15f, 15f) else 0f
                    val normalized = (db / 15f) // -1..+1
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
                            activeColor.copy(alpha = 0.30f),
                            secondaryGlow.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = curvePath,
                    color = activeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw glowing node dots for each of the 10 bands
                points.forEachIndexed { idx, pt ->
                    val nodeColor = if (bandGainsDb[idx] > 4f && isEnabled) NeonMagenta else activeColor
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.3f),
                        radius = 7.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Overlay HUD telemetry badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (isEnabled) "SPLINE 10-BAND • 64-BIT DSP" else "DSP BYPASSED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isEnabled) ElectricCyan else TextMuted,
                modifier = Modifier
                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Text(
                text = String.format(Locale.US, "PEAK %+.1f dBFS", peakDb),
                style = MaterialTheme.typography.labelSmall,
                color = if (peakDb > -3f) StudioAmber else SignalEmerald,
                modifier = Modifier
                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
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
                val fillHeight = kotlin.math.abs(thumbY - midY).coerceAtLeast(2f)

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
