package com.hiresaudioplayer.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiresaudioplayer.app.dsp.DspManager
import com.hiresaudioplayer.app.dsp.EqualizerPreset
import com.hiresaudioplayer.app.dsp.PeqBand
import com.hiresaudioplayer.app.ui.theme.*
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun EqualizerScreen(dspManager: DspManager) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Graphic EQ (10-Band)", "Parametric EQ (PEQ)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        Text(
            text = "Audiophile Equalizer",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = GoldAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) GoldAccent else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            GraphicEqTab(dspManager)
        } else {
            ParametricEqTab(dspManager)
        }
    }
}

@Composable
fun GraphicEqTab(dspManager: DspManager) {
    val enabled by dspManager.graphicEqEnabled.collectAsState()
    val gains by dspManager.graphicEqGains.collectAsState()
    val currentPreset by dspManager.currentPresetName.collectAsState()
    val frequencies = DspManager.EQ_FREQUENCIES

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle Switch & Preamp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Equalizer State", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (enabled) "Active • $currentPreset Preset" else "Bypassed",
                    color = if (enabled) GoldAccent else TextMuted,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { dspManager.setGraphicEqEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BgDark,
                    checkedTrackColor = GoldAccent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Presets Chips Carousel
        Text("Sound Presets", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqualizerPreset.PRESETS.forEach { preset ->
                val isSelected = currentPreset == preset.name
                FilterChip(
                    selected = isSelected,
                    onClick = { dspManager.applyPreset(preset) },
                    label = { Text(preset.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent,
                        selectedLabelColor = BgDark,
                        containerColor = SurfaceDark,
                        labelColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 10 Sliders Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+12 dB", color = TextMuted, fontSize = 10.sp)
                    Text("0 dB (Flat)", color = TextMuted, fontSize = 10.sp)
                    Text("-12 dB", color = TextMuted, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable 10 Band Vertical-like sliders
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(frequencies.indices.toList()) { i ->
                        val gain = gains.getOrElse(i) { 0f }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = frequencies[i],
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(36.dp)
                            )

                            Slider(
                                value = gain,
                                onValueChange = { newVal ->
                                    dspManager.setGraphicEqBandGain(i, newVal)
                                },
                                valueRange = -12.0f..12.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = AudioTrackInactive
                                ),
                                enabled = enabled,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = String.format("%+.1f dB", gain),
                                color = if (gain > 0) GoldAccent else if (gain < 0) CyanAccent else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParametricEqTab(dspManager: DspManager) {
    val enabled by dspManager.peqEnabled.collectAsState()
    val preamp by dspManager.peqPreampDb.collectAsState()
    val bands by dspManager.peqBands.collectAsState()
    var showAutoEqDialog by remember { mutableStateOf(false) }
    var autoEqText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }

    if (showAutoEqDialog) {
        AlertDialog(
            onDismissRequest = { showAutoEqDialog = false },
            title = { Text("Import AutoEQ Profile", color = CyanAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste parametric EQ text from AutoEQ (e.g. jaakkopasanen/AutoEq for your headphone model):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = autoEqText,
                        onValueChange = { autoEqText = it },
                        placeholder = { Text("Preamp: -4.5 dB\nFilter 1: ON PK Fc 32 Hz Gain 4.2 dB Q 1.41...", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceDarkVariant,
                            unfocusedContainerColor = SurfaceDarkVariant
                        )
                    )
                    importStatus?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = GoldAccent, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = dspManager.importAutoEqText(autoEqText)
                        if (ok) {
                            importStatus = "Profile applied successfully!"
                            showAutoEqDialog = false
                        } else {
                            importStatus = "Could not parse filters. Check format."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = BgDark)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoEqDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Parametric EQ (PEQ)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (enabled) "Filter Engine Active" else "Bypassed",
                    color = if (enabled) CyanAccent else TextMuted,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showAutoEqDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Import AutoEQ",
                        tint = CyanAccent
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { dspManager.setPeqEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BgDark,
                        checkedTrackColor = CyanAccent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preamp Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preamp:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = preamp,
                onValueChange = { dspManager.setPeqPreamp(it) },
                valueRange = -12.0f..6.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = AudioTrackInactive
                ),
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%+.1f dB", preamp),
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(55.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Response Curve Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            PeqCurveCanvas(bands = bands, preamp = preamp, enabled = enabled)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("PEQ Filter Bands (7 Bands)", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bands) { band ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Band ${band.id + 1} • ${formatFreq(band.frequency)} (${getFilterTypeName(band.type)})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Checkbox(
                                checked = band.enabled,
                                onCheckedChange = { check ->
                                    dspManager.updatePeqBand(band.id, band.frequency, band.gainDb, band.q, band.type, check)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                            )
                        }

                        // Gain Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gain", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = band.gainDb,
                                onValueChange = { newGain ->
                                    dspManager.updatePeqBand(band.id, band.frequency, newGain, band.q, band.type, band.enabled)
                                },
                                valueRange = -15.0f..15.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanAccent,
                                    activeTrackColor = CyanAccent,
                                    inactiveTrackColor = AudioTrackInactive
                                ),
                                enabled = enabled && band.enabled,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("%+.1f dB", band.gainDb),
                                color = CyanAccent,
                                fontSize = 11.sp,
                                modifier = Modifier.width(48.dp)
                            )
                        }

                        // Q Factor Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Q", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = band.q,
                                onValueChange = { newQ ->
                                    dspManager.updatePeqBand(band.id, band.frequency, band.gainDb, newQ, band.type, band.enabled)
                                },
                                valueRange = 0.2f..8.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = AudioTrackInactive
                                ),
                                enabled = enabled && band.enabled,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("Q %.2f", band.q),
                                color = GoldAccent,
                                fontSize = 11.sp,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeqCurveCanvas(bands: List<PeqBand>, preamp: Float, enabled: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Draw 0dB Grid Line
        drawLine(
            color = BorderDark,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1f
        )

        if (!enabled) return@Canvas

        val path = Path()
        val numPoints = 80
        val minFreq = 20.0
        val maxFreq = 20000.0

        for (i in 0 until numPoints) {
            val frac = i.toDouble() / (numPoints - 1)
            // Logarithmic frequency scale
            val f = minFreq * (maxFreq / minFreq).pow(frac)

            var totalDb = preamp.toDouble()

            for (b in bands) {
                if (!b.enabled) continue
                val f0 = b.frequency.toDouble()
                val gain = b.gainDb.toDouble()
                val q = b.q.toDouble()

                // Approximate Gaussian peaking response for visual curve
                val logDist = log10(f / f0)
                val width = 1.0 / (q * 2.0)
                val response = gain * kotlin.math.exp(-(logDist * logDist) / (2.0 * width * width))
                totalDb += response
            }

            // Map totalDb (-18dB .. +18dB) to canvas height
            val y = midY - (totalDb.toFloat() / 18f) * (h / 2f)
            val x = (frac.toFloat()) * w

            if (i == 0) path.moveTo(x, y.coerceIn(0f, h))
            else path.lineTo(x, y.coerceIn(0f, h))
        }

        drawPath(
            path = path,
            color = CyanAccent,
            style = Stroke(width = 2.5f)
        )
    }
}

private fun formatFreq(freq: Float): String {
    return if (freq >= 1000f) String.format("%.1f kHz", freq / 1000f)
    else String.format("%.0f Hz", freq)
}

private fun getFilterTypeName(type: Int): String {
    return when (type) {
        0 -> "Peak"
        1 -> "Low Shelf"
        2 -> "High Shelf"
        3 -> "Low Pass"
        4 -> "High Pass"
        5 -> "Band Pass"
        6 -> "Notch"
        else -> "Filter"
    }
}

