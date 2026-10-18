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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
    onLockProMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUnlocked = uiState.isAiProUnlocked

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.3.dp,
            brush = if (isUnlocked) {
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (isUnlocked) {
                                        listOf(ElectricCyan, NeonMagenta)
                                    } else {
                                        listOf(StudioAmber, NeonMagenta)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                            contentDescription = "AI Auto-EQ PRO",
                            tint = AmoledBlack,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI AUTO-EQ PRESET",
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
                            text = if (isUnlocked) {
                                "Target: ${uiState.aiSelectedDevice} • ${uiState.lastAiEngineBadge}"
                            } else {
                                "PRO Exclusive • Locked (Owner Access Only)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isUnlocked) NeonMagenta else StudioAmber
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUnlocked) {
                        IconButton(
                            onClick = onLockProMode,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("relock_ai_pro_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Re-lock PRO Mode",
                                tint = StudioAmber,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Button(
                        onClick = onOpenAiDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) ElectricCyan else StudioAmber,
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
                                imageVector = if (isUnlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                contentDescription = if (isUnlocked) "Open AI Studio" else "Unlock PRO Mode",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUnlocked) "AI STUDIO" else "UNLOCK PRO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1-Tap AI Auto-EQ Scenario Chips (Locked badge shown when PRO is locked)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    text = "AI-Based Auto-EQ Preset System is currently locked under Equltra PRO Mode so only the app owner can access it.",
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
    onPromptChanged: (String) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onGenerateAiPreset: (String?) -> Unit,
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
                            text = "AI Auto-EQ Preset System (PRO)",
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
