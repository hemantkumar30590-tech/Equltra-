package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EqPresetEntity
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
    onQuickScenarioClick: (String) -> Unit,
    onOpenAiDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(ElectricCyan, NeonMagenta, HyperViolet)
            )
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricCyan, NeonMagenta)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Auto-EQ",
                            tint = AmoledBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI AUTO-EQ PRESET SYSTEM",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.lastAiEngineBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = AmoledBlack,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(ElectricCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = "Target: ${uiState.aiSelectedDevice}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = NeonMagenta
                        )
                    }
                }

                Button(
                    onClick = onOpenAiDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TUNING...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Open AI Auto-EQ",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1-Tap AI Auto-EQ Scenario Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AI_QUICK_SCENARIOS) { (chipLabel, fullScenarioPrompt) ->
                    val isSelected = uiState.aiPromptInput.equals(fullScenarioPrompt, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onQuickScenarioClick(fullScenarioPrompt) },
                        modifier = Modifier.testTag("ai_chip_$chipLabel"),
                        label = {
                            Text(
                                text = "✨ $chipLabel",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonMagenta,
                            selectedLabelColor = Color.White,
                            containerColor = AmoledBlack,
                            labelColor = ElectricCyan
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

            // AI Acoustic Reasoning Readout
            Text(
                text = uiState.lastAiAnalysis,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSecondary,
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
fun AiAutoEqDialog(
    uiState: EqUiState,
    devicePresets: List<EqPresetEntity>,
    dominantFreqHz: Int,
    peakDb: Float,
    onPromptChanged: (String) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onGenerateAiPreset: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Auto-EQ",
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Auto-EQ Preset System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.lastAiEngineBadge} • 10-Band + DSP Mastering",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
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
                    label = { Text("Song, Genre, Mood, or Gaming Goal") },
                    placeholder = {
                        Text(
                            "e.g., Punjabi Sub-Bass, BGMI Footsteps, Arijit Singh Vocals...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
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
