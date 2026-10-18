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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
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
import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EqPresetEntity
import com.example.ui.components.FrequencyResponseAndSpectrumCanvas
import com.example.ui.components.StudioRotaryKnobCard
import com.example.ui.components.SystemAudioLevelVisualizerCard
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
    val importedPresets by viewModel.importedPresets.collectAsStateWithLifecycle()
    val isPlayingTest by viewModel.isPlayingTestLoop.collectAsStateWithLifecycle()
    val isAudioRecordActive by viewModel.isAudioRecordActive.collectAsStateWithLifecycle()
    val isSystemAudioActive by viewModel.isSystemAudioActive.collectAsStateWithLifecycle()
    val systemVolumePercent by viewModel.systemVolumePercent.collectAsStateWithLifecycle()
    val waveformSamples by viewModel.waveformSamples.collectAsStateWithLifecycle()
    val stereoVuLevels by viewModel.stereoVuLevels.collectAsStateWithLifecycle()
    val spectrumBins by viewModel.spectrumBins.collectAsStateWithLifecycle()
    val peakHoldBins by viewModel.peakHoldBins.collectAsStateWithLifecycle()
    val dominantFreqHz by viewModel.dominantFreqHz.collectAsStateWithLifecycle()
    val peakDb by viewModel.peakDb.collectAsStateWithLifecycle()
    val hardwareStatus by viewModel.hardwareStatus.collectAsStateWithLifecycle()
    val isVoiceListening by viewModel.isVoiceListening.collectAsStateWithLifecycle()
    val isVoiceHandsFree by viewModel.isVoiceHandsFree.collectAsStateWithLifecycle()
    val isVoiceTtsEnabled by viewModel.isVoiceTtsEnabled.collectAsStateWithLifecycle()
    val voiceRmsLevel by viewModel.voiceRmsLevel.collectAsStateWithLifecycle()
    val voiceLiveTranscript by viewModel.voiceLiveTranscript.collectAsStateWithLifecycle()
    val voiceStatusBadge by viewModel.voiceStatusBadge.collectAsStateWithLifecycle()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsStateWithLifecycle()
    val lastHapticEventLabel by viewModel.lastHapticEventLabel.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val localView = LocalView.current

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
                    Triple(2, "Device Presets", Icons.Default.Headphones),
                    Triple(3, "Presets", Icons.Default.LibraryMusic),
                    Triple(4, "Get APK", Icons.Default.Download)
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                val voiceMicPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        viewModel.startVoiceListening()
                    } else {
                        Toast.makeText(
                            context,
                            "Microphone permission needed for live Voice Control (or use Instant Voice Chips)",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.setVoiceControlDialogVisible(true)
                    }
                }

                val triggerLiveVoiceMic: () -> Unit = {
                    if (isVoiceListening) {
                        viewModel.stopVoiceListening()
                    } else {
                        val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasMicPerm) {
                            viewModel.startVoiceListening()
                        } else {
                            voiceMicPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }

                StudioTopHeader(
                    isEnabled = uiState.isMasterEnabled,
                    isBackgroundServiceEnabled = uiState.isBackgroundServiceEnabled,
                    activePresetName = uiState.activePresetName,
                    isPlayingTest = isPlayingTest,
                    isVoiceListening = isVoiceListening,
                    hardwareStatus = hardwareStatus,
                    onToggleMaster = {
                        if (isHapticsEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        viewModel.toggleMasterEnabled()
                    },
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
                    onOpenVoiceControl = { viewModel.setVoiceControlDialogVisible(true) },
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
                        waveformSamples = waveformSamples,
                        stereoVuLevels = stereoVuLevels,
                        systemVolumePercent = systemVolumePercent,
                        isSystemAudioActive = isSystemAudioActive,
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
                        onSelectPreset = { preset ->
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            viewModel.applyPreset(preset, toggleIfAlreadyActive = true)
                        },
                        onResetFlat = {
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            viewModel.resetToFlat()
                        },
                        onOpenSavePreset = { viewModel.setSavePresetDialogVisible(true) },
                        onQuickAiScenario = { scenarioPrompt ->
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.generateAndApplyAiAutoEq(
                                overridePrompt = scenarioPrompt,
                                saveToLibrary = true
                            ) { aiPreset ->
                                Toast.makeText(
                                    context,
                                    "AI Auto-EQ Applied: ${aiPreset.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onOpenAiDialog = { viewModel.setAiAutoEqDialogVisible(true) },
                        isVoiceListening = isVoiceListening,
                        voiceStatusBadge = voiceStatusBadge,
                        onOpenVoiceControl = { viewModel.setVoiceControlDialogVisible(true) },
                        onToggleLiveMic = triggerLiveVoiceMic,
                        onExecuteVoiceCommand = { cmd ->
                            viewModel.executeVoiceCommand(cmd) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLockProMode = {
                            viewModel.lockAiProMode()
                            Toast.makeText(
                                context,
                                "AI Auto-EQ PRO Mode Locked 🔒",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        isHapticsEnabled = isHapticsEnabled,
                        lastHapticEventLabel = lastHapticEventLabel,
                        onToggleHaptics = viewModel::toggleHapticsEnabled
                    )

                    1 -> BassAndSpatialDspTab(
                        uiState = uiState,
                        spectrumBins = spectrumBins,
                        waveformSamples = waveformSamples,
                        stereoVuLevels = stereoVuLevels,
                        systemVolumePercent = systemVolumePercent,
                        peakDb = peakDb,
                        onBassChanged = viewModel::updateBassBoost,
                        onSpatialChanged = viewModel::updateSpatialLevel,
                        onReverbChanged = viewModel::updateReverbLevel,
                        onReverbRoomChanged = { room ->
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            viewModel.updateReverbRoom(room)
                        },
                        onPreampChanged = viewModel::updatePreampDb,
                        onToggleLimiter = {
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.toggleSoftLimiter()
                        }
                    )

                    2 -> DevicePresetsTab(
                        devicePresets = viewModel.devicePresets,
                        searchQuery = uiState.deviceSearchQuery,
                        activePresetName = uiState.activePresetName,
                        isMasterEnabled = uiState.isMasterEnabled,
                        isHapticsEnabled = isHapticsEnabled,
                        onSearchQueryChanged = viewModel::updateDeviceSearchQuery,
                        onApplyDevicePreset = { device ->
                            val wasAlreadyApplied = uiState.activePresetName.equals(device.name, ignoreCase = true) && uiState.isMasterEnabled
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            viewModel.applyPreset(device, toggleIfAlreadyActive = true)
                            Toast.makeText(
                                context,
                                if (wasAlreadyApplied) {
                                    "Toggled OFF ${device.name} → Flat Bypass (Haptic Snap)"
                                } else {
                                    "Applied ${device.name} 10-Band EQ! (Tactile Haptics)"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    3 -> PresetsAndImportExportTab(
                        presets = presets,
                        importedPresets = importedPresets,
                        activePresetName = uiState.activePresetName,
                        isMasterEnabled = uiState.isMasterEnabled,
                        isHapticsEnabled = isHapticsEnabled,
                        onSelectPreset = { preset ->
                            if (isHapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            viewModel.applyPreset(preset, toggleIfAlreadyActive = true)
                        },
                        onDeleteCustomPreset = viewModel::deleteCustomPreset,
                        onOpenSaveDialog = { viewModel.setSavePresetDialogVisible(true) },
                        onOpenPasteImportDialog = { viewModel.setPasteImportDialogVisible(true) },
                        isAiProUnlocked = uiState.isAiProUnlocked,
                        onOpenAiAutoEqDialog = { viewModel.setAiAutoEqDialogVisible(true) },
                        onImportFromContent = { content, fileName ->
                            viewModel.importPresetFromContent(content, fileName) { saved ->
                                if (saved != null) {
                                    Toast.makeText(
                                        context,
                                        "Imported '${saved.name}' into My Imports!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Invalid preset format (.json, AutoEQ .txt, or .equltra required)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onBuildExportJson = viewModel::buildCurrentPresetExportJson
                    )

                    4 -> ApkAndGithubGuideTab()
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

    if (uiState.showPasteImportDialog) {
        val context = LocalContext.current
        PasteAutoEqOrJsonImportDialog(
            onDismiss = { viewModel.setPasteImportDialogVisible(false) },
            onImportContent = { rawContent, fallbackName ->
                viewModel.importPresetFromContent(rawContent, fallbackName) { saved ->
                    if (saved != null) {
                        Toast.makeText(
                            context,
                            "Imported '${saved.name}' into My Imports!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Could not parse preset content",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    if (uiState.showAiProUnlockDialog) {
        val context = LocalContext.current
        AiProOwnerUnlockDialog(
            onVerifyOwnerKey = { keyInput ->
                val ok = viewModel.tryUnlockAiProMode(keyInput)
                if (ok) {
                    Toast.makeText(
                        context,
                        "Owner Verified! AI Auto-EQ PRO Mode Unlocked 👑",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                ok
            },
            onDismiss = { viewModel.setAiProUnlockDialogVisible(false) }
        )
    }

    if (uiState.showAiAutoEqDialog) {
        val context = LocalContext.current
        AiAutoEqDialog(
            uiState = uiState,
            devicePresets = viewModel.devicePresets,
            dominantFreqHz = dominantFreqHz,
            peakDb = peakDb,
            isVoiceListening = isVoiceListening,
            onPromptChanged = viewModel::updateAiPromptInput,
            onDeviceSelected = viewModel::updateAiSelectedDevice,
            onGenerateAiPreset = { overridePrompt ->
                viewModel.generateAndApplyAiAutoEq(
                    overridePrompt = overridePrompt,
                    saveToLibrary = true
                ) { aiPreset ->
                    Toast.makeText(
                        context,
                        "AI Auto-EQ Applied & Saved: ${aiPreset.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onToggleVoiceMic = {
                viewModel.setAiAutoEqDialogVisible(false)
                viewModel.setVoiceControlDialogVisible(true)
            },
            onOpenVoiceCenter = {
                viewModel.setAiAutoEqDialogVisible(false)
                viewModel.setVoiceControlDialogVisible(true)
            },
            onLockProMode = {
                viewModel.lockAiProMode()
                Toast.makeText(
                    context,
                    "AI Auto-EQ PRO Mode Locked 🔒",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { viewModel.setAiAutoEqDialogVisible(false) }
        )
    }

    if (uiState.showVoiceControlDialog) {
        val context = LocalContext.current
        val voiceDialogMicLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.startVoiceListening()
            } else {
                Toast.makeText(
                    context,
                    "Use Instant Voice Chips or Google Mic button below",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        AiVoiceControlCenterDialog(
            uiState = uiState,
            isListening = isVoiceListening,
            isHandsFree = isVoiceHandsFree,
            isVoiceTtsEnabled = isVoiceTtsEnabled,
            voiceRmsLevel = voiceRmsLevel,
            liveTranscript = voiceLiveTranscript,
            voiceStatusBadge = voiceStatusBadge,
            onToggleListening = {
                if (isVoiceListening) {
                    viewModel.stopVoiceListening()
                } else {
                    val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasMicPerm) {
                        viewModel.startVoiceListening()
                    } else {
                        voiceDialogMicLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onToggleHandsFree = viewModel::toggleVoiceHandsFree,
            onToggleVoiceTts = viewModel::toggleVoiceTts,
            onExecuteVoiceCommand = { cmd ->
                viewModel.executeVoiceCommand(cmd) { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onOpenAiStudio = {
                viewModel.setAiAutoEqDialogVisible(true)
            },
            onDismiss = { viewModel.setVoiceControlDialogVisible(false) }
        )
    }
}

@Composable
private fun StudioTopHeader(
    isEnabled: Boolean,
    isBackgroundServiceEnabled: Boolean,
    activePresetName: String,
    isPlayingTest: Boolean,
    isVoiceListening: Boolean = false,
    hardwareStatus: String,
    onToggleMaster: () -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleTestLoop: () -> Unit,
    onOpenVoiceControl: () -> Unit = {},
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
                            contentDescription = "Equltra Logo",
                            tint = AmoledBlack,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Equltra",
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

            // Action Buttons: SYNC YOUTUBE + VOICE AI + TEST AUDIO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRehookYouTube,
                    modifier = Modifier
                        .weight(1.05f)
                        .height(38.dp)
                        .testTag("rehook_youtube_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, ElectricCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-Hook YouTube Audio",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SYNC YT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenVoiceControl,
                    modifier = Modifier
                        .weight(1.05f)
                        .height(38.dp)
                        .testTag("header_voice_control_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVoiceListening) SignalEmerald else NeonMagenta,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice AI Command Center",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVoiceListening) "LISTENING" else "VOICE AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleTestLoop,
                    modifier = Modifier
                        .weight(1.05f)
                        .height(38.dp)
                        .testTag("test_audio_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingTest) NeonMagenta else ElectricCyan,
                        contentColor = if (isPlayingTest) Color.White else AmoledBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingTest) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingTest) "Stop Synth Loop" else "Play Synth Loop",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlayingTest) "STOP TEST" else "TEST AUDIO",
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
    waveformSamples: List<Float>,
    stereoVuLevels: Pair<Float, Float>,
    systemVolumePercent: Int,
    isSystemAudioActive: Boolean,
    dominantFreqHz: Int,
    isAudioRecordActive: Boolean,
    peakDb: Float,
    onToggleAudioRecord: () -> Unit,
    onBandGainChanged: (Int, Float) -> Unit,
    onSelectPreset: (EqPresetEntity) -> Unit,
    onResetFlat: () -> Unit,
    onOpenSavePreset: () -> Unit,
    onQuickAiScenario: (String) -> Unit,
    onOpenAiDialog: () -> Unit,
    isVoiceListening: Boolean = false,
    voiceStatusBadge: String = "VOICE AI READY",
    onOpenVoiceControl: () -> Unit = {},
    onToggleLiveMic: () -> Unit = {},
    onExecuteVoiceCommand: (String) -> Unit = {},
    onLockProMode: () -> Unit = {},
    isHapticsEnabled: Boolean = true,
    lastHapticEventLabel: String = "TACTILE HAPTICS READY",
    onToggleHaptics: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Real-time System Audio + AudioRecord 1024-Pt FFT Visualizer + 10-Band Spline Response
        FrequencyResponseAndSpectrumCanvas(
            bandGainsDb = uiState.bandGainsDb,
            spectrumBins = spectrumBins,
            peakHoldBins = peakHoldBins,
            dominantFreqHz = dominantFreqHz,
            isAudioRecordActive = isAudioRecordActive,
            isEnabled = uiState.isMasterEnabled,
            peakDb = peakDb,
            onToggleAudioRecord = onToggleAudioRecord,
            waveformSamples = waveformSamples,
            stereoVuLevels = stereoVuLevels,
            systemVolumePercent = systemVolumePercent,
            isSystemAudioActive = isSystemAudioActive
        )

        Spacer(modifier = Modifier.height(10.dp))

        // AI-Based Auto-EQ Preset System + Voice Control Card (PRO Mode)
        AiAutoEqBannerCard(
            uiState = uiState,
            isVoiceListening = isVoiceListening,
            voiceStatusBadge = voiceStatusBadge,
            onQuickScenarioClick = onQuickAiScenario,
            onOpenAiDialog = onOpenAiDialog,
            onOpenVoiceControl = onOpenVoiceControl,
            onToggleLiveMic = onToggleLiveMic,
            onExecuteVoiceCommand = onExecuteVoiceCommand,
            onLockProMode = onLockProMode
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tactile Preset Haptics Header & Toggle Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EQUALIZER PRESETS (SELECT / TOGGLE)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = if (isHapticsEnabled) ElectricCyan.copy(alpha = 0.14f) else ObsidianCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isHapticsEnabled) ElectricCyan.copy(alpha = 0.7f) else ObsidianBorder
                ),
                modifier = Modifier
                    .clickable(onClick = onToggleHaptics)
                    .testTag("haptic_feedback_toggle_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Toggle Preset Haptic Feedback",
                        tint = if (isHapticsEnabled) ElectricCyan else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isHapticsEnabled) {
                            lastHapticEventLabel.take(30)
                        } else {
                            "HAPTICS OFF"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = if (isHapticsEnabled) ElectricCyan else TextMuted,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Preset Selector Strip (All 9 built-in + custom presets)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets, key = { "${it.id}_${it.name}" }) { preset ->
                val isSelected = uiState.activePresetName.equals(preset.name, ignoreCase = true) && uiState.isMasterEnabled
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPreset(preset) },
                    modifier = Modifier.testTag("preset_chip_${preset.name}"),
                    label = {
                        Text(
                            text = if (isSelected) "${preset.name} • ON" else preset.name,
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
    waveformSamples: List<Float>,
    stereoVuLevels: Pair<Float, Float>,
    systemVolumePercent: Int,
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
        // Real-Time System Audio Level & Sub-Bass Canvas Visualizer Card
        SystemAudioLevelVisualizerCard(
            isEnabled = uiState.isMasterEnabled,
            spectrumBins = spectrumBins,
            waveformSamples = waveformSamples,
            stereoVuLevels = stereoVuLevels,
            systemVolumePercent = systemVolumePercent,
            peakDb = peakDb,
            bassBoostPercent = uiState.bassBoostStrength,
            spatialPercent = uiState.spatialLevel
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                            uriHandler.openUri("https://github.com/hemantkumar30590-tech/Equltra-/releases")
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
                    text = "1. Push this project to GitHub (hemantkumar30590-tech/Equltra-) using the GitHub icon in AI Studio.\n" +
                        "2. Open https://github.com/hemantkumar30590-tech/Equltra-/releases -> Tap 'Equltra-v1.0.apk'.",
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
                                    "https://github.com/hemantkumar30590-tech/Equltra-/releases"
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
