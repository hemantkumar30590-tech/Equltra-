package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import java.io.File
import java.util.Locale

@Composable
fun DevicePresetsTab(
    devicePresets: List<EqPresetEntity>,
    searchQuery: String,
    activePresetName: String,
    isMasterEnabled: Boolean = true,
    isHapticsEnabled: Boolean = true,
    onSearchQueryChanged: (String) -> Unit,
    onApplyDevicePreset: (EqPresetEntity) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var selectedBrandFilter by remember { mutableStateOf("All") }
    val brandFilters = listOf("All", "Apple", "Sony", "Samsung", "Bose", "Sennheiser", "Google", "JBL", "OnePlus", "Nothing", "Boat")

    val filteredDevices = remember(devicePresets, searchQuery, selectedBrandFilter) {
        devicePresets.filter { preset ->
            val matchesQuery = searchQuery.isBlank() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.description.contains(searchQuery, ignoreCase = true)
            val matchesBrand = selectedBrandFilter == "All" ||
                preset.name.contains(selectedBrandFilter, ignoreCase = true)
            matchesQuery && matchesBrand
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Device Presets (Oct 2026 Latest)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "15 AutoEQ & RTINGS Tuned Earbuds / Headphones Profiles",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
            Text(
                text = "${filteredDevices.size}/15",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(ObsidianCard, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar: "Search your earbuds..."
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Search your earbuds...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search your earbuds",
                    tint = ElectricCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_search_bar")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Brand Quick Filter Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(brandFilters) { brand ->
                val selected = selectedBrandFilter == brand
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (isHapticsEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        selectedBrandFilter = brand
                    },
                    modifier = Modifier.testTag("brand_chip_$brand"),
                    label = {
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = AmoledBlack,
                        containerColor = ObsidianCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = ObsidianBorder,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device Preset Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredDevices, key = { it.id }) { device ->
                val isApplied = activePresetName.equals(device.name, ignoreCase = true) && isMasterEnabled
                DevicePresetCard(
                    device = device,
                    isApplied = isApplied,
                    onApply = { onApplyDevicePreset(device) }
                )
            }
        }
    }
}

@Composable
private fun DevicePresetCard(
    device: EqPresetEntity,
    isApplied: Boolean,
    onApply: () -> Unit
) {
    val isOverEar = device.name.contains("WH-", ignoreCase = true) ||
        device.name.contains("Momentum", ignoreCase = true) ||
        device.name.contains("QuietComfort", ignoreCase = true) ||
        device.name.contains("Over-ear", ignoreCase = true)

    val brandAccent = when {
        device.name.contains("Apple", ignoreCase = true) -> ElectricCyan
        device.name.contains("Sony", ignoreCase = true) -> StudioAmber
        device.name.contains("Samsung", ignoreCase = true) -> HyperViolet
        device.name.contains("Bose", ignoreCase = true) -> ElectricCyan
        device.name.contains("JBL", ignoreCase = true) || device.name.contains("OnePlus", ignoreCase = true) -> NeonMagenta
        device.name.contains("Google", ignoreCase = true) -> SignalEmerald
        else -> ElectricCyan
    }

    Surface(
        color = if (isApplied) Color(0xFF0E2030) else ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isApplied) 1.5.dp else 1.dp,
            color = if (isApplied) ElectricCyan else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() }
            .testTag("device_card_${device.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Device Icon + Name + Description + Apply Button
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        brandAccent.copy(alpha = 0.25f),
                                        AmoledBlack
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverEar) Icons.Default.Headphones else Icons.Default.Hearing,
                            contentDescription = device.name,
                            tint = brandAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = device.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApplied) SignalEmerald else ElectricCyan,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("apply_device_${device.name}")
                ) {
                    Text(
                        text = if (isApplied) "APPLIED ✓" else "APPLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency Curve Preview + Bass / Mid / Treble Values Row
            val gains = device.getBandGains()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Frequency Response Curve Canvas Preview
                MiniFrequencyCurvePreview(
                    bandGains = gains,
                    accentColor = brandAccent,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledBlack)
                )

                // Bass / Mid / Treble Summary Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToneSummaryPill("BASS", device.bassSummaryDb, NeonMagenta)
                    ToneSummaryPill("MID", device.midSummaryDb, ElectricCyan)
                    ToneSummaryPill("TREBLE", device.trebleSummaryDb, SignalEmerald)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 10-band +0.0dB values + BASS % • 3D % badges (matching current UI)
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
                                text = String.format(Locale.US, "%+.1f", db),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                color = if (db > 0f) ElectricCyan else if (db < 0f) NeonMagenta else TextSecondary
                            )
                            Text(
                                text = EqPresetEntity.FREQUENCY_LABELS[idx].replace("Hz", ""),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                color = TextMuted
                            )
                        }
                    }
                }

                Text(
                    text = "BASS ${device.bassBoostStrength}% • 3D ${device.spatialLevel}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ToneSummaryPill(label: String, dbValue: Float, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledBlack)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = TextSecondary
        )
        Text(
            text = String.format(Locale.US, "%+.1fdB", dbValue),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MiniFrequencyCurvePreview(
    bandGains: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // 0 dB center line
        drawLine(
            color = Color(0xFF1E273C),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        if (bandGains.isEmpty()) return@Canvas

        val points = bandGains.mapIndexed { idx, gain ->
            val x = (idx.toFloat() / (bandGains.size - 1).coerceAtLeast(1)) * w
            val normalized = (gain.coerceIn(-15f, 15f) / 15f)
            val y = midY - normalized * (h * 0.42f)
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
                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        drawPath(
            path = curvePath,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun PresetsAndImportExportTab(
    presets: List<EqPresetEntity>,
    importedPresets: List<EqPresetEntity>,
    activePresetName: String,
    isMasterEnabled: Boolean = true,
    isHapticsEnabled: Boolean = true,
    onSelectPreset: (EqPresetEntity) -> Unit,
    onDeleteCustomPreset: (EqPresetEntity) -> Unit,
    onOpenSaveDialog: () -> Unit,
    onOpenPasteImportDialog: () -> Unit,
    isAiProUnlocked: Boolean = false,
    onOpenAiAutoEqDialog: () -> Unit = {},
    onImportFromContent: (String, String) -> Unit,
    onBuildExportJson: () -> String
) {
    val context = LocalContext.current

    // File Picker Launcher supporting .json, .txt AutoEQ, and .equltra files
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Imported_Preset.json"
                if (!content.isNullOrBlank()) {
                    onImportFromContent(content, fileName)
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Could not read file: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save .json File to storage launcher
    val saveJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val json = onBuildExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Exported preset .json file!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Action Bar: Import, Export (Share via WhatsApp/Telegram), Save Current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Presets & Custom Import/Export",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supports AI Auto-EQ (PRO), .json, AutoEQ .txt & .equltra",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
            Button(
                onClick = onOpenAiAutoEqDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAiProUnlocked) NeonMagenta else StudioAmber,
                    contentColor = if (isAiProUnlocked) Color.White else AmoledBlack
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("presets_tab_ai_autoeq_btn")
            ) {
                Icon(
                    imageVector = if (isAiProUnlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                    contentDescription = "AI Auto-EQ PRO",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAiProUnlocked) "AI AUTO-EQ PRO" else "AI PRO LOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Import / Export / Paste / Save Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // IMPORT Button with file icon
            Button(
                onClick = {
                    importFileLauncher.launch(arrayOf("*/*"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("import_preset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Import Preset File",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "IMPORT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // EXPORT Button (Share .json via WhatsApp / Telegram / System Share)
            OutlinedButton(
                onClick = {
                    val jsonContent = onBuildExportJson()
                    val safeName = activePresetName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "equltra_preset" }
                    runCatching {
                        val exportFile = File(context.cacheDir, "$safeName.json")
                        exportFile.writeText(jsonContent)
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Equltra Preset - $activePresetName ($safeName.json)")
                        putExtra(Intent.EXTRA_TEXT, jsonContent)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Export & Share $safeName.json via WhatsApp / Telegram")
                    )
                },
                border = BorderStroke(1.dp, NeonMagenta),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("export_preset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = "Export Preset JSON",
                    tint = NeonMagenta,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EXPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
            }

            // PASTE / AUTOEQ TXT Button
            OutlinedButton(
                onClick = onOpenPasteImportDialog,
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("paste_autoeq_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste AutoEQ or JSON",
                    tint = SignalEmerald,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AUTOEQ/JSON",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalEmerald,
                    fontWeight = FontWeight.Bold
                )
            }

            // SAVE CURRENT Button
            OutlinedButton(
                onClick = onOpenSaveDialog,
                border = BorderStroke(1.dp, ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("new_custom_preset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Preset",
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SAVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Save .JSON to device storage link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    val safeName = activePresetName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "equltra_preset" }
                    saveJsonFileLauncher.launch("$safeName.json")
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "Save current EQ as .json file to storage →",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // SECTION 1: MY IMPORTS
            item(key = "header_my_imports") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY IMPORTS (${importedPresets.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ".json • .txt AutoEQ • .equltra",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            if (importedPresets.isEmpty()) {
                item(key = "empty_my_imports") {
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "No imported presets yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap IMPORT to pick a .json, AutoEQ .txt, or .equltra file, share a file from WhatsApp/Telegram to Equltra, or tap AUTOEQ/JSON to import instantly.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(importedPresets, key = { "import_${it.id}_${it.name}" }) { preset ->
                    PresetItemCard(
                        preset = preset,
                        isSelected = activePresetName.equals(preset.name, ignoreCase = true) && isMasterEnabled,
                        badgeText = "IMPORTED",
                        badgeColor = NeonMagenta,
                        onSelect = { onSelectPreset(preset) },
                        onDelete = { onDeleteCustomPreset(preset) }
                    )
                }
            }

            // SECTION 2: STUDIO & CUSTOM PRESETS
            item(key = "header_studio_presets") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "STUDIO & SAVED PRESETS (${presets.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            items(presets, key = { "preset_${it.id}_${it.name}" }) { preset ->
                PresetItemCard(
                    preset = preset,
                    isSelected = activePresetName.equals(preset.name, ignoreCase = true) && isMasterEnabled,
                    badgeText = preset.category.uppercase(Locale.US),
                    badgeColor = ElectricCyan,
                    onSelect = { onSelectPreset(preset) },
                    onDelete = if (!preset.isBuiltIn) {
                        { onDeleteCustomPreset(preset) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetItemCard(
    preset: EqPresetEntity,
    isSelected: Boolean,
    badgeText: String,
    badgeColor: Color,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        color = if (isSelected) Color(0xFF102232) else ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ElectricCyan else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("preset_card_${preset.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Tune,
                        contentDescription = preset.name,
                        tint = if (isSelected) ElectricCyan else badgeColor,
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier
                            .background(AmoledBlack, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Switch(
                        checked = isSelected,
                        onCheckedChange = { onSelect() },
                        modifier = Modifier.testTag("toggle_preset_${preset.name}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmoledBlack,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = AmoledBlack
                        )
                    )
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_preset_${preset.name}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${preset.name}",
                                tint = NeonMagenta
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                                text = String.format(Locale.US, "%+.1f", db),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                color = if (db > 0f) ElectricCyan else TextSecondary
                            )
                            Text(
                                text = EqPresetEntity.FREQUENCY_LABELS[idx].replace("Hz", ""),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                color = TextMuted
                            )
                        }
                    }
                }

                Text(
                    text = "BASS ${preset.bassBoostStrength}% • 3D ${preset.spatialLevel}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PasteAutoEqOrJsonImportDialog(
    onDismiss: () -> Unit,
    onImportContent: (String, String) -> Unit
) {
    var presetName by remember { mutableStateOf("My AutoEQ Import") }
    var rawText by remember {
        mutableStateOf(
            """{"name": "Harman Sub-Bass Custom", "bands": [4.5, 3.5, 1.5, -1.0, 0.0, 1.5, 3.0, 3.5, 2.5, 2.0], "bass": 64, "spatial": 82}"""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Import .json / AutoEQ .txt / .equltra",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            presetName = "Equltra JSON Custom"
                            rawText = """{"name": "Equltra JSON Custom", "bands": [5.0, 4.0, 2.0, 0.0, -0.5, 1.0, 2.5, 3.5, 3.0, 2.5], "bass": 64, "spatial": 82}"""
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sample .json", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    }
                    OutlinedButton(
                        onClick = {
                            presetName = "AutoEQ Parametric"
                            rawText = """
                                Preamp: -4.2 dB
                                Filter 1: ON PK Fc 31 Hz Gain 4.5 dB Q 1.41
                                Filter 2: ON PK Fc 62 Hz Gain 3.2 dB Q 1.41
                                Filter 3: ON PK Fc 125 Hz Gain -1.5 dB Q 1.41
                                Filter 4: ON PK Fc 250 Hz Gain -2.0 dB Q 1.41
                                Filter 5: ON PK Fc 1000 Hz Gain 1.8 dB Q 1.41
                                Filter 6: ON PK Fc 2000 Hz Gain 3.6 dB Q 1.41
                                Filter 7: ON PK Fc 4000 Hz Gain 4.0 dB Q 1.41
                                Filter 8: ON PK Fc 8000 Hz Gain 2.5 dB Q 1.41
                            """.trimIndent()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sample AutoEQ .txt", style = MaterialTheme.typography.labelSmall, color = NeonMagenta)
                    }
                }

                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Fallback Name (for .txt files)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste .json, .equltra, or AutoEQ .txt content") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_raw_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImportContent(rawText, presetName)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier.testTag("confirm_import_btn")
            ) {
                Text("Import to My Imports", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
