package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EqPresetEntity
import com.example.ui.components.FrequencyResponseAndSpectrumCanvas
import com.example.ui.components.StudioRotaryKnobCard
import com.example.ui.components.VerticalEqBandFader
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperViolet
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun EqultraScreen(
    viewModel: EqViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val isPlayingTest by viewModel.isPlayingTestLoop.collectAsStateWithLifecycle()
    val isAudioRecordActive by viewModel.isAudioRecordActive.collectAsStateWithLifecycle()
    val spectrumBins by viewModel.spectrumBins.collectAsStateWithLifecycle()
    val peakHoldBins by viewModel.peakHoldBins.collectAsStateWithLifecycle()
    val dominantFreqHz by viewModel.dominantFreqHz.collectAsStateWithLifecycle()
    val peakDb by viewModel.peakDb.collectAsStateWithLifecycle()
    val hardwareStatus by viewModel.hardwareStatus.collectAsStateWithLifecycle()

    if (uiState.selectedTab != 0) {
        BackHandler {
            viewModel.selectTab(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple(0, "10-Band EQ", Icons.Default.Equalizer),
                    Triple(1, "Bass & DSP", Icons.Default.Speaker),
                    Triple(2, "Presets", Icons.Default.LibraryMusic),
                    Triple(3, "Get APK", Icons.Default.Download)
                )
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        modifier = Modifier.testTag("nav_tab_$index"),
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmoledBlack,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Top Studio Rack Header & Runtime Permission Launchers
                val context = LocalContext.current
                val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { _ ->
                    viewModel.setBackgroundServiceEnabled(true)
                }
                val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        viewModel.setAudioRecordSpectrumEnabled(true)
                    } else {
                        Toast.makeText(
                            context,
                            "Microphone permission needed for live AudioRecord FFT capture",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                StudioTopHeader(
                    isEnabled = uiState.isMasterEnabled,
                    isBackgroundServiceEnabled = uiState.isBackgroundServiceEnabled,
                    activePresetName = uiState.activePresetName,
                    isPlayingTest = isPlayingTest,
                    hardwareStatus = hardwareStatus,
                    onToggleMaster = viewModel::toggleMasterEnabled,
                    onToggleBackgroundService = { enableBg ->
                        if (enableBg && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setBackgroundServiceEnabled(true)
                            }
                        } else {
                            viewModel.setBackgroundServiceEnabled(enableBg)
                        }
                    },
                    onToggleTestLoop = viewModel::toggleTestAudioLoop,
                    onRehookYouTube = {
                        viewModel.rehookYouTubeAudio()
                        Toast.makeText(
                            context,
                            "YouTube & Global 10-Band DynamicsProcessing Hooked!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (uiState.selectedTab) {
                    0 -> TenBandEqualizerTab(
                        uiState = uiState,
                        presets = presets,
                        spectrumBins = spectrumBins,
                        peakHoldBins = peakHoldBins,
                        dominantFreqHz = dominantFreqHz,
                        isAudioRecordActive = isAudioRecordActive,
                        peakDb = peakDb,
                        onToggleAudioRecord = {
                            if (isAudioRecordActive) {
                                viewModel.setAudioRecordSpectrumEnabled(false)
                            } else {
                                val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasMicPerm) {
                                    viewModel.setAudioRecordSpectrumEnabled(true)
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onBandGainChanged = viewModel::updateBandGain,
                        onSelectPreset = viewModel::applyPreset,
                        onResetFlat = viewModel::resetToFlat,
                        onOpenSavePreset = { viewModel.setSavePresetDialogVisible(true) }
                    )

                    1 -> BassAndSpatialDspTab(
                        uiState = uiState,
                        spectrumBins = spectrumBins,
                        peakDb = peakDb,
                        onBassChanged = viewModel::updateBassBoost,
                        onSpatialChanged = viewModel::updateSpatialLevel,
                        onReverbChanged = viewModel::updateReverbLevel,
                        onReverbRoomChanged = viewModel::updateReverbRoom,
                        onPreampChanged = viewModel::updatePreampDb,
                        onToggleLimiter = viewModel::toggleSoftLimiter
                    )

                    2 -> PresetsLibraryTab(
                        presets = presets,
                        activePresetName = uiState.activePresetName,
                        onSelectPreset = {
                            viewModel.applyPreset(it)
                        },
                        onDeleteCustomPreset = viewModel::deleteCustomPreset,
                        onOpenSaveDialog = { viewModel.setSavePresetDialogVisible(true) }
                    )

                    3 -> ApkAndGithubGuideTab()
                }
            }
        }
    }

    if (uiState.showSavePresetDialog) {
        SaveCustomPresetDialog(
            onDismiss = { viewModel.setSavePresetDialogVisible(false) },
            onConfirm = { name, desc ->
                viewModel.saveCurrentAsCustomPreset(name, desc)
            }
        )
    }
}

@Composable
private fun StudioTopHeader(
    isEnabled: Boolean,
    isBackgroundServiceEnabled: Boolean,
    activePresetName: String,
    isPlayingTest: Boolean,
    hardwareStatus: String,
    onToggleMaster: () -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleTestLoop: () -> Unit,
    onRehookYouTube: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricCyan, HyperViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "VoidEQ Equltra Logo",
                            tint = AmoledBlack,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VoidEQ Equltra",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "v1.0 PRO",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmoledBlack,
                                modifier = Modifier
                                    .background(ElectricCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "PRESET: ${activePresetName.uppercase(Locale.US)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled) ElectricCyan else TextMuted
                        )
                    }
                }

                // Master DSP Power Button
                IconButton(
                    onClick = onToggleMaster,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(if (isEnabled) ElectricCyan.copy(alpha = 0.16f) else AmoledBlack)
                        .border(
                            1.5.dp,
                            if (isEnabled) ElectricCyan else ObsidianBorder,
                            CircleShape
                        )
                        .testTag("master_power_switch")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = if (isEnabled) "Disable DSP Engine" else "Enable DSP Engine",
                        tint = if (isEnabled) ElectricCyan else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware / YouTube Hook Status Readout
            Text(
                text = hardwareStatus,
                style = MaterialTheme.typography.labelSmall,
                color = SignalEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: SYNC YOUTUBE + TEST AUDIO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRehookYouTube,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("rehook_youtube_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, ElectricCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-Hook YouTube Audio",
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYNC YOUTUBE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleTestLoop,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("test_audio_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingTest) NeonMagenta else ElectricCyan,
                        contentColor = if (isPlayingTest) Color.White else AmoledBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingTest) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingTest) "Stop Synth Loop" else "Play Synth Loop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlayingTest) "STOP TEST BEAT" else "TEST AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Background Mode & Notification Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmoledBlack)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBackgroundServiceEnabled) {
                            "BACKGROUND DSP & NOTIFICATION: ACTIVE"
                        } else {
                            "BACKGROUND DSP & NOTIFICATION: OFF"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBackgroundServiceEnabled) SignalEmerald else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keeps equalizer running across Spotify, YouTube & other apps",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isBackgroundServiceEnabled,
                    onCheckedChange = onToggleBackgroundService,
                    modifier = Modifier.testTag("background_service_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmoledBlack,
                        checkedTrackColor = SignalEmerald
                    )
                )
            }
        }
    }
}

@Composable
private fun TenBandEqualizerTab(
    uiState: EqUiState,
    presets: List<EqPresetEntity>,
    spectrumBins: List<Float>,
    peakHoldBins: List<Float>,
    dominantFreqHz: Int,
    isAudioRecordActive: Boolean,
    peakDb: Float,
    onToggleAudioRecord: () -> Unit,
    onBandGainChanged: (Int, Float) -> Unit,
    onSelectPreset: (EqPresetEntity) -> Unit,
    onResetFlat: () -> Unit,
    onOpenSavePreset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Real-time AudioRecord 1024-Pt FFT Spectrum Visualizer + 10-Band Spline Response
        FrequencyResponseAndSpectrumCanvas(
            bandGainsDb = uiState.bandGainsDb,
            spectrumBins = spectrumBins,
            peakHoldBins = peakHoldBins,
            dominantFreqHz = dominantFreqHz,
            isAudioRecordActive = isAudioRecordActive,
            isEnabled = uiState.isMasterEnabled,
            peakDb = peakDb,
            onToggleAudioRecord = onToggleAudioRecord
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Selector Strip (All 9 built-in + custom presets)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets, key = { "${it.id}_${it.name}" }) { preset ->
                val isSelected = uiState.activePresetName.equals(preset.name, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPreset(preset) },
                    modifier = Modifier.testTag("preset_chip_${preset.name}"),
                    label = {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = AmoledBlack,
                        containerColor = ObsidianCard,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = ObsidianBorder,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Bar above 10-Band Fader Bank
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-BAND GRAPHIC EQ (-15dB TO +15dB)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onResetFlat,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("reset_flat_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Flat",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FLAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onOpenSavePreset,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("save_preset_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, ElectricCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Custom Preset",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SAVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 10-Band Horizontal Scrollable Studio Fader Rack
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqPresetEntity.FREQUENCY_LABELS.forEachIndexed { index, freqLabel ->
                VerticalEqBandFader(
                    bandIndex = index,
                    frequencyLabel = freqLabel,
                    gainDb = uiState.bandGainsDb.getOrElse(index) { 0f },
                    isEnabled = uiState.isMasterEnabled,
                    onGainChanged = { newGain -> onBandGainChanged(index, newGain) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick DSP Summary Strip at bottom of EQ screen
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DspMiniStat("BASS BOOST", "${uiState.bassBoostStrength}%", NeonMagenta)
                DspMiniStat("SPATIAL 3D", "${uiState.spatialLevel}%", ElectricCyan)
                DspMiniStat("REVERB (${uiState.reverbRoom.uppercase(Locale.US)})", "${uiState.reverbLevel}%", HyperViolet)
                DspMiniStat("PREAMP", String.format(Locale.US, "%+.1fdB", uiState.preampDb), SignalEmerald)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DspMiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BassAndSpatialDspTab(
    uiState: EqUiState,
    spectrumBins: List<Float>,
    peakDb: Float,
    onBassChanged: (Int) -> Unit,
    onSpatialChanged: (Int) -> Unit,
    onReverbChanged: (Int) -> Unit,
    onReverbRoomChanged: (String) -> Unit,
    onPreampChanged: (Float) -> Unit,
    onToggleLimiter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Two Rotary Studio Knobs Side-by-Side (Bass Boost + Spatial 3D)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudioRotaryKnobCard(
                title = "Bass Boost",
                subtitle = "55Hz Sub-Harmonic Punch",
                valuePercent = uiState.bassBoostStrength,
                accentColor = NeonMagenta,
                testTag = "bass_boost_slider",
                onValueChange = onBassChanged,
                modifier = Modifier.weight(1f)
            )
            StudioRotaryKnobCard(
                title = "Spatial 3D",
                subtitle = "Binaural Soundstage Width",
                valuePercent = uiState.spatialLevel,
                accentColor = ElectricCyan,
                testTag = "spatial_3d_slider",
                onValueChange = onSpatialChanged,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Acoustic Reverb Module Card
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Acoustic Reverb Engine",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Schroeder reflections & room decay simulation",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${uiState.reverbLevel}% WET",
                        style = MaterialTheme.typography.labelLarge,
                        color = HyperViolet
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = uiState.reverbLevel.toFloat(),
                    onValueChange = { onReverbChanged(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reverb_level_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = HyperViolet,
                        activeTrackColor = HyperViolet,
                        inactiveTrackColor = AmoledBlack
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val rooms = listOf("None", "Small Room", "Medium Room", "Large Hall", "Plate")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rooms) { room ->
                        val selected = uiState.reverbRoom == room
                        FilterChip(
                            selected = selected,
                            onClick = { onReverbRoomChanged(room) },
                            modifier = Modifier.testTag("reverb_room_$room"),
                            label = {
                                Text(
                                    text = room,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HyperViolet,
                                selectedLabelColor = Color.White,
                                containerColor = AmoledBlack,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preamp Gain + Soft-Clip Mastering Limiter Card
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Preamp & Loudness Gain",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Headroom compensation (-12.0 dB to +12.0 dB)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%+.1f dB", uiState.preampDb),
                        style = MaterialTheme.typography.labelLarge,
                        color = SignalEmerald
                    )
                }

                Slider(
                    value = uiState.preampDb,
                    onValueChange = onPreampChanged,
                    valueRange = -12f..12f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preamp_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SignalEmerald,
                        activeTrackColor = SignalEmerald,
                        inactiveTrackColor = AmoledBlack
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "64-Bit Soft-Clip Brickwall Limiter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Prevents digital clipping when Bass Boost & EQ bands are pushed high",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.softLimiterEnabled,
                        onCheckedChange = { onToggleLimiter() },
                        modifier = Modifier.testTag("soft_limiter_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmoledBlack,
                            checkedTrackColor = SignalEmerald
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PresetsLibraryTab(
    presets: List<EqPresetEntity>,
    activePresetName: String,
    onSelectPreset: (EqPresetEntity) -> Unit,
    onDeleteCustomPreset: (EqPresetEntity) -> Unit,
    onOpenSaveDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Studio & Custom Presets (${presets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "9 Mastering Profiles + Local Custom Storage",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
            Button(
                onClick = onOpenSaveDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("new_custom_preset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Custom Preset",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SAVE CURRENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(presets, key = { "${it.id}_${it.name}" }) { preset ->
                val isSelected = activePresetName.equals(preset.name, ignoreCase = true)
                Surface(
                    color = if (isSelected) Color(0xFF102232) else ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPreset(preset) }
                        .testTag("preset_card_${preset.name}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Tune,
                                    contentDescription = preset.name,
                                    tint = if (isSelected) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (!preset.isBuiltIn) {
                                IconButton(
                                    onClick = { onDeleteCustomPreset(preset) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete ${preset.name}",
                                        tint = NeonMagenta
                                    )
                                }
                            } else {
                                Text(
                                    text = preset.category.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier
                                        .background(AmoledBlack, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini 10-band bar preview + DSP badges
                        val gains = preset.getBandGains()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                gains.forEachIndexed { idx, db ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = String.format(Locale.US, "%+.0f", db),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = if (db > 0f) ElectricCyan else TextSecondary
                                        )
                                        Text(
                                            text = EqPresetEntity.FREQUENCY_LABELS[idx].replace("Hz", ""),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "BASS ${preset.bassBoostStrength}% • 3D ${preset.spatialLevel}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = NeonMagenta
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkAndGithubGuideTab() {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val exportApkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument(
            "application/vnd.android.package-archive"
        )
    ) { destUri ->
        if (destUri != null) {
            runCatching {
                val sourceApk = java.io.File(context.applicationInfo.sourceDir)
                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    sourceApk.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Toast.makeText(context, "Saved Equltra-v1.0.apk successfully!", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, ElectricCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download APK",
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Direct APK Download & Export",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { exportApkLauncher.launch("Equltra-v1.0.apk") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_apk_direct_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save Equltra-v1.0.apk",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPORT EQULTRA-V1.0.APK TO FILES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        runCatching {
                            uriHandler.openUri("https://github.com/hemantkumar30590-tech/Equltra/releases")
                        }
                    },
                    border = BorderStroke(1.dp, NeonMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_github_releases_btn")
                ) {
                    Text(
                        text = "OPEN GITHUB RELEASES PAGE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Option A — Direct from AI Studio Toolbar (Phone):",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Tap the Settings (Gear) icon at the top-right of AI Studio.\n" +
                        "2. Tap 'Download APK' / 'Generate APK' to download the signed APK directly to your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Option B — Direct from GitHub Releases:",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonMagenta
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Push this project to GitHub (hemantkumar30590-tech/Equltra) using the GitHub icon in AI Studio.\n" +
                        "2. Open https://github.com/hemantkumar30590-tech/Equltra/releases -> Tap 'Equltra-v1.0.apk'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // GitHub Actions Workflow Verification Card
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "GitHub Actions Workflow",
                            tint = SignalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ".github/workflows/build-apk.yml",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "GitHub Releases URL",
                                    "https://github.com/hemantkumar30590-tech/Equltra/releases"
                                )
                            )
                            Toast.makeText(context, "Copied Releases URL!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Releases Link",
                            tint = ElectricCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "• Pre-configured with permissions: contents: write (uses automatic GITHUB_TOKEN, zero manual secrets required).\n" +
                        "• Sets up Java 17 + Flutter 3.22 + Android SDK.\n" +
                        "• Builds split-per-abi release APKs + universal Equltra-v1.0.apk.\n" +
                        "• Uploads Equltra-v1.0.apk to both GitHub Actions Artifacts and GitHub Releases automatically on every push to main.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SaveCustomPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Save Custom Studio Preset",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name (e.g., My AMOLED Bass)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier.testTag("confirm_save_preset_btn")
            ) {
                Text("Save Preset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
