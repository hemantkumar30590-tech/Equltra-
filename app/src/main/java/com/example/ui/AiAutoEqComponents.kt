package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EqPresetEntity
import com.example.dsp.AiVoiceControlEngine
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AI_QUICK_SCENARIOS = listOf(
    "Phonk / Gym Sub-Bass" to "Phonk & Gym Heavy Sub-Bass Slam",
    "Bollywood Vocals" to "Bollywood Lead Vocal Clarity & Air",
    "BGMI / FPS Footsteps" to "BGMI & Valorant Tactical Footsteps Clarity",
    "Dolby Cinema 3D" to "Dolby Atmos Cinema 3D Surround",
    "Fix Boomy Buds" to "Fix Boomy Muddy Budget Earbuds",
    "Lo-Fi Night" to "Lo-Fi Late Night Warm Velvet",
    "Live Spectrum AI" to "Live Spectrum Smart Calibration"
)

@Composable
fun AiAutoEqBannerCard(
    uiState: EqUiState,
    isVoiceListening: Boolean = false,
    voiceStatusBadge: String = "VOICE AI READY",
    onQuickScenarioClick: (String) -> Unit,
    onOpenAiDialog: () -> Unit,
    onOpenVoiceControl: () -> Unit = {},
    onToggleLiveMic: () -> Unit = {},
    onExecuteVoiceCommand: (String) -> Unit = {},
    onLockProMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUnlocked = uiState.isAiProUnlocked

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.4.dp,
            brush = if (isVoiceListening) {
                Brush.horizontalGradient(
                    colors = listOf(SignalEmerald, ElectricCyan, NeonMagenta)
                )
            } else if (isUnlocked) {
                Brush.horizontalGradient(
                    colors = listOf(ElectricCyan, NeonMagenta, HyperViolet)
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(StudioAmber, NeonMagenta, HyperViolet)
                )
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (isVoiceListening) {
                                        listOf(SignalEmerald, ElectricCyan)
                                    } else if (isUnlocked) {
                                        listOf(ElectricCyan, NeonMagenta)
                                    } else {
                                        listOf(StudioAmber, NeonMagenta)
                                    }
                                )
                            )
                            .clickable { onOpenVoiceControl() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVoiceListening) {
                                Icons.Default.Mic
                            } else if (isUnlocked) {
                                Icons.Default.AutoAwesome
                            } else {
                                Icons.Default.Lock
                            },
                            contentDescription = "AI Auto-EQ & Voice Control",
                            tint = AmoledBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI AUTO-EQ + VOICE",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUnlocked) "PRO UNLOCKED 👑" else "PRO LOCKED 🔒",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = AmoledBlack,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        if (isUnlocked) SignalEmerald else StudioAmber,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = if (isVoiceListening) {
                                "🎙️ $voiceStatusBadge"
                            } else if (isUnlocked) {
                                "Voice & AI Active • ${uiState.aiSelectedDevice}"
                            } else {
                                "Voice DSP Active • AI Auto-EQ PRO (Owner Only)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isVoiceListening) SignalEmerald else if (isUnlocked) NeonMagenta else StudioAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUnlocked) {
                        IconButton(
                            onClick = onLockProMode,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("relock_ai_pro_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Re-lock PRO Mode",
                                tint = StudioAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // 1-Tap Voice Command Center Button
                    Button(
                        onClick = onOpenVoiceControl,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVoiceListening) SignalEmerald else NeonMagenta,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_voice_control_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Control Everything",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isVoiceListening) "LIVE" else "VOICE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onOpenAiDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) ElectricCyan else StudioAmber,
                            contentColor = AmoledBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_ai_autoeq_btn")
                    ) {
                        if (uiState.isGeneratingAiEq) {
                            CircularProgressIndicator(
                                color = AmoledBlack,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                contentDescription = if (isUnlocked) "Open AI Studio" else "Unlock PRO Mode",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUnlocked) "AI EQ" else "PRO",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Voice Control Bar inside the Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmoledBlack)
                    .clickable { onOpenVoiceControl() }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isVoiceListening) SignalEmerald else NeonMagenta.copy(alpha = 0.22f))
                            .clickable { onToggleLiveMic() }
                            .testTag("quick_mic_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Toggle Live Voice Mic",
                            tint = if (isVoiceListening) AmoledBlack else NeonMagenta,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VOICE CONTROL EVERYTHING (HINDI / EN)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.lastVoiceCommandSummary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SPEAK 🎙️",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = AmoledBlack,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(ElectricCyan, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 1-Tap AI Auto-EQ & Voice Scenario Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AiVoiceControlEngine.QUICK_VOICE_COMMANDS.take(5)) { (chipLabel, voiceCmd) ->
                    FilterChip(
                        selected = false,
                        onClick = { onExecuteVoiceCommand(voiceCmd) },
                        label = {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AmoledBlack,
                            labelColor = SignalEmerald
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = SignalEmerald.copy(alpha = 0.55f)
                        )
                    )
                }

                items(AI_QUICK_SCENARIOS) { (chipLabel, fullScenarioPrompt) ->
                    val isSelected = isUnlocked &&
                        uiState.aiPromptInput.equals(fullScenarioPrompt, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onQuickScenarioClick(fullScenarioPrompt) },
                        modifier = Modifier.testTag("ai_chip_$chipLabel"),
                        label = {
                            Text(
                                text = if (isUnlocked) "✨ $chipLabel" else "🔒 $chipLabel (PRO)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonMagenta,
                            selectedLabelColor = Color.White,
                            containerColor = AmoledBlack,
                            labelColor = if (isUnlocked) ElectricCyan else StudioAmber
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ObsidianBorder,
                            selectedBorderColor = NeonMagenta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // AI Acoustic Reasoning / PRO Lock Status Readout
            Text(
                text = uiState.lastAiAnalysis,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = if (isUnlocked) TextSecondary else StudioAmber,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledBlack)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AiVoiceControlCenterDialog(
    uiState: EqUiState,
    isListening: Boolean,
    isHandsFree: Boolean,
    isVoiceTtsEnabled: Boolean,
    voiceRmsLevel: Float,
    liveTranscript: String,
    voiceStatusBadge: String,
    onToggleListening: () -> Unit,
    onToggleHandsFree: () -> Unit,
    onToggleVoiceTts: () -> Unit,
    onExecuteVoiceCommand: (String) -> Unit,
    onOpenAiStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var customSpokenInput by remember { mutableStateOf("") }

    // System Speech Recognizer Intent Launcher (works on devices with Google Voice Input)
    val systemVoiceIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            if (best.isNotEmpty()) {
                onExecuteVoiceCommand(best)
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "voice_radar_pulse")
    val radarPhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_phase"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (isListening) {
                                        listOf(SignalEmerald, ElectricCyan)
                                    } else {
                                        listOf(NeonMagenta, ElectricCyan)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "AI Voice Command Center",
                            tint = AmoledBlack,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Voice Control Center 🎙️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = voiceStatusBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isListening) SignalEmerald else ElectricCyan
                        )
                    }
                }

                // Voice TTS Talkback Toggle
                OutlinedButton(
                    onClick = onToggleVoiceTts,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, if (isVoiceTtsEnabled) SignalEmerald else ObsidianBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Toggle AI Spoken Feedback",
                        tint = if (isVoiceTtsEnabled) SignalEmerald else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVoiceTtsEnabled) "TTS ON" else "TTS OFF",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isVoiceTtsEnabled) SignalEmerald else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Voice Radar & Waveform Visualizer Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmoledBlack)
                        .clickable { onToggleListening() }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(86.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val activeEnergy = if (isListening) (0.35f + voiceRmsLevel * 0.65f) else 0.14f

                        // Pulsing concentric voice reactor rings
                        for (ring in 1..3) {
                            val r = (18.dp.toPx() * ring) * (1f + activeEnergy * 0.25f * sin(radarPhase + ring))
                            drawCircle(
                                color = if (isListening) {
                                    SignalEmerald.copy(alpha = (0.38f / ring).coerceIn(0.05f, 0.4f))
                                } else {
                                    ElectricCyan.copy(alpha = 0.14f / ring)
                                },
                                radius = r,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // 28 symmetrical voice waveform bars across the width
                        val barCount = 28
                        val spacing = size.width / (barCount + 2)
                        for (i in 0 until barCount) {
                            val distFromCenter = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                            val waveFactor = (1f - distFromCenter * 0.65f) *
                                (0.4f + 0.6f * kotlin.math.abs(sin(radarPhase * 1.5f + i * 0.45f)))
                            val barH = (size.height * 0.78f * activeEnergy * waveFactor).coerceAtLeast(6.dp.toPx())
                            val x = spacing * (i + 1.5f)
                            drawLine(
                                brush = Brush.verticalGradient(
                                    colors = if (isListening) {
                                        listOf(SignalEmerald, ElectricCyan, NeonMagenta)
                                    } else {
                                        listOf(ElectricCyan.copy(alpha = 0.45f), NeonMagenta.copy(alpha = 0.35f))
                                    }
                                ),
                                start = Offset(x, cy - barH / 2f),
                                end = Offset(x, cy + barH / 2f),
                                strokeWidth = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) SignalEmerald else ElectricCyan
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Tap to Speak",
                                tint = AmoledBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isListening) {
                                liveTranscript.ifBlank { "Listening... Speak in Hindi / English" }
                            } else {
                                "Tap Mic to Speak or Choose Instant Voice Command"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isListening) SignalEmerald else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dual Mic Action Buttons + Hands-Free Loop Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onToggleListening,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) SignalEmerald else ElectricCyan,
                            contentColor = AmoledBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_live_mic_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Live SpeechRecognizer",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isListening) "STOP MIC" else "SPEAK LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "Speak Equltra Command (e.g. 'Bass 100%', 'AirPods Pro 3', 'AI Punjabi Bass')"
                                    )
                                }
                                systemVoiceIntentLauncher.launch(intent)
                            }
                        },
                        border = BorderStroke(1.dp, NeonMagenta),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "GOOGLE MIC",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleHandsFree,
                        border = BorderStroke(1.dp, if (isHandsFree) SignalEmerald else StudioAmber),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isHandsFree) "LOOP: ON" else "LOOP: OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isHandsFree) SignalEmerald else StudioAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Type or Dictate any Hindi/Hinglish/English Voice Command directly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customSpokenInput,
                        onValueChange = { customSpokenInput = it },
                        label = { Text("Speak or Type Voice Command (Hindi / EN)") },
                        placeholder = {
                            Text(
                                "e.g., 'Bass full kar do', 'Unlock Pro 30590', 'AirPods Pro 3'",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AmoledBlack,
                            unfocusedContainerColor = AmoledBlack,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_command_text_input")
                    )

                    Button(
                        onClick = {
                            if (customSpokenInput.isNotBlank()) {
                                onExecuteVoiceCommand(customSpokenInput)
                                customSpokenInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = AmoledBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("execute_voice_cmd_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Execute Voice Command",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "1-Tap Instant Voice Commands (Tap to Execute + Hear AI Voice):",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AiVoiceControlEngine.QUICK_VOICE_COMMANDS) { (label, cmd) ->
                        OutlinedButton(
                            onClick = { onExecuteVoiceCommand(cmd) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, SignalEmerald),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = SignalEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Voice Command Guide + Recent Execution Log
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmoledBlack)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "EVERYTHING YOU CAN CONTROL BY VOICE:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = StudioAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Bass & 3D: \"Bass full 100%\", \"Bass badhao\", \"3D spatial 90%\", \"Large Hall reverb\"\n" +
                            "• AI Auto-EQ: \"AI Punjabi Sub-Bass\", \"BGMI footsteps clear karo\", \"Arijit Singh vocal\"\n" +
                            "• 15 Earbuds: \"AirPods Pro 3\", \"Sony XM6\", \"Galaxy Buds 4 Pro\", \"Boat Airdopes\"\n" +
                            "• System & PRO: \"Sync YouTube\", \"Play test audio\", \"Reset flat\", \"Unlock Pro 30590\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RECENT VOICE EXECUTIONS:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                    uiState.voiceCommandHistory.take(3).forEach { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenAiStudio()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMagenta,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open AI Auto-EQ Studio", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AiProOwnerUnlockDialog(
    onVerifyOwnerKey: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var ownerInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(StudioAmber, NeonMagenta)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PRO Locked",
                        tint = AmoledBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Auto-EQ • PRO MODE 🔒",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StudioAmber
                    )
                    Text(
                        text = "Locked • Exclusive Owner Access Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonMagenta
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI-Based Auto-EQ Preset System is locked under Equltra PRO Mode so only the app owner can access it. (Tip: You can also unlock via Voice by saying \"Unlock Pro 30590\"!)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = ownerInput,
                    onValueChange = {
                        ownerInput = it
                        errorMessage = null
                    },
                    label = { Text("Enter Owner Email / GitHub ID / PIN") },
                    placeholder = {
                        Text(
                            "Owner Email or Master PIN (30590)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Owner Key",
                            tint = StudioAmber
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledBlack,
                        unfocusedContainerColor = AmoledBlack,
                        focusedBorderColor = StudioAmber,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_pro_key_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unlocked = onVerifyOwnerKey(ownerInput)
                    if (!unlocked) {
                        errorMessage = "Access Denied: Only the Owner (Hemant) can unlock AI Auto-EQ PRO."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioAmber,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier.testTag("confirm_unlock_pro_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unlock Owner PRO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Locked", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AiAutoEqDialog(
    uiState: EqUiState,
    devicePresets: List<EqPresetEntity>,
    dominantFreqHz: Int,
    peakDb: Float,
    isVoiceListening: Boolean = false,
    onPromptChanged: (String) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onGenerateAiPreset: (String?) -> Unit,
    onToggleVoiceMic: () -> Unit = {},
    onOpenVoiceCenter: () -> Unit = {},
    onLockProMode: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Auto-EQ",
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Auto-EQ + Voice (PRO)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OWNER UNLOCKED 👑 • ${uiState.lastAiEngineBadge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SignalEmerald
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenVoiceCenter,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Open Voice Command Center",
                            tint = if (isVoiceListening) SignalEmerald else NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = onLockProMode,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock PRO",
                            tint = StudioAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOCK",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select Your Earbuds / Headphones:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(devicePresets) { device ->
                        val selected = uiState.aiSelectedDevice == device.name
                        FilterChip(
                            selected = selected,
                            onClick = { onDeviceSelected(device.name) },
                            label = {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = if (selected) AmoledBlack else ElectricCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = AmoledBlack,
                                containerColor = AmoledBlack,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.aiPromptInput,
                    onValueChange = onPromptChanged,
                    label = { Text("Speak 🎙️ or Type Song, Genre, or Gaming Goal") },
                    placeholder = {
                        Text(
                            "e.g., Punjabi Sub-Bass, BGMI Footsteps, Arijit Singh Vocals...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onToggleVoiceMic) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate AI Prompt by Voice",
                                tint = if (isVoiceListening) SignalEmerald else NeonMagenta
                            )
                        }
                    },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledBlack,
                        unfocusedContainerColor = AmoledBlack,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input")
                )

                // Quick Scenario Chips inside dialog
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AI_QUICK_SCENARIOS) { (label, prompt) ->
                        OutlinedButton(
                            onClick = {
                                onPromptChanged(prompt)
                                onGenerateAiPreset(prompt)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, NeonMagenta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "✨ $label",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NeonMagenta
                            )
                        }
                    }
                }

                // Live Spectrum Calibration Button
                OutlinedButton(
                    onClick = {
                        onGenerateAiPreset("Live Spectrum Smart Calibration (Dom ${dominantFreqHz}Hz)")
                    },
                    border = BorderStroke(1.dp, SignalEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_live_spectrum_calibrate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Calibrate from Live Spectrum",
                        tint = SignalEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTO-CALIBRATE FROM LIVE SPECTRUM (${dominantFreqHz}Hz)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preview of current AI-tuned 10-band curve + analysis
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmoledBlack)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE: ${uiState.activePresetName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "BASS ${uiState.bassBoostStrength}% • 3D ${uiState.spatialLevel}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    MiniFrequencyCurvePreview(
                        bandGains = uiState.bandGainsDb,
                        accentColor = ElectricCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.lastAiAnalysis,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure GEMINI_API_KEY in AI Studio Secrets for cloud Gemini 3.5 Flash tuning (prototype APK keys should not be shared publicly).",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerateAiPreset(null)
                },
                enabled = !uiState.isGeneratingAiEq,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier.testTag("generate_ai_autoeq_btn")
            ) {
                if (uiState.isGeneratingAiEq) {
                    CircularProgressIndicator(
                        color = AmoledBlack,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generating...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate & Save AI EQ", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TextSecondary)
            }
        }
    )
}
