package com.hiresaudioplayer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiresaudioplayer.app.dsp.DspManager
import com.hiresaudioplayer.app.ui.theme.*

@Composable
fun DspSettingsScreen(dspManager: DspManager) {
    val upsampleRate by dspManager.targetUpsampleRate.collectAsState()
    val filterMode by dspManager.resamplerFilterMode.collectAsState()
    val flacProfile by dspManager.flacProfile.collectAsState()
    val aacProfile by dspManager.aacProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Hi-Res & Codec DSP Engine",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure upsampling up to 192 kHz and codec-specific sound enhancement",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: 192 kHz SINC RESAMPLER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Upsampling Upper Limit",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Polyphase Windowed-Sinc Interpolation",
                            color = GoldAccent,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Target Upper Limit Sampling Rate", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Rate Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rates = DspManager.SAMPLING_RATES
                    rates.take(3).forEach { rate ->
                        val isSelected = upsampleRate == rate
                        FilterChip(
                            selected = isSelected,
                            onClick = { dspManager.setTargetUpsampleRate(rate) },
                            label = { Text("${rate / 1000}k", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = BgDark,
                                containerColor = SurfaceDarkVariant,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rates = DspManager.SAMPLING_RATES
                    rates.drop(3).forEach { rate ->
                        val isSelected = upsampleRate == rate
                        FilterChip(
                            selected = isSelected,
                            onClick = { dspManager.setTargetUpsampleRate(rate) },
                            label = {
                                Text(
                                    if (rate == 192000) "192k (Hi-Res)" else "${rate / 1000}k",
                                    fontSize = 11.sp,
                                    fontWeight = if (rate == 192000) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = BgDark,
                                containerColor = SurfaceDarkVariant,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Resampling Filter Impulse Response", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == 0,
                        onClick = { dspManager.setResamplerFilterMode(0) },
                        label = { Text("Linear Phase (Transparent)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = BgDark,
                            containerColor = SurfaceDarkVariant,
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = filterMode == 1,
                        onClick = { dspManager.setResamplerFilterMode(1) },
                        label = { Text("Minimum Phase (No Pre-ring)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = BgDark,
                            containerColor = SurfaceDarkVariant,
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: FLAC & LOSSLESS DEEP SETTINGS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FLAC & Lossless Tuning",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audiophile Soundstage & Pure Bit-Stream",
                            color = CyanAccent,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bit-Perfect Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bit-Perfect Bypass", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Bypasses all DSP / Resampling directly to DAC", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = flacProfile.bitPerfectMode,
                        onCheckedChange = { dspManager.setFlacBitPerfect(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDark, checkedTrackColor = CyanAccent)
                    )
                }

                HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                // Meier Binaural Crossfeed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Meier Binaural Crossfeed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Simulates natural speaker crosstalk for fatigue-free headphone listening", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = flacProfile.crossfeedEnabled && !flacProfile.bitPerfectMode,
                        onCheckedChange = { dspManager.setFlacCrossfeed(it, flacProfile.crossfeedStrength) },
                        enabled = !flacProfile.bitPerfectMode,
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDark, checkedTrackColor = CyanAccent)
                    )
                }

                if (flacProfile.crossfeedEnabled && !flacProfile.bitPerfectMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strength", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(55.dp))
                        Slider(
                            value = flacProfile.crossfeedStrength,
                            onValueChange = { dspManager.setFlacCrossfeed(true, it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = AudioTrackInactive
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(flacProfile.crossfeedStrength * 100).toInt()}%",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                // Smooth Roll-Off Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smooth Roll-Off Anti-Aliasing", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Gentle 22kHz Butterworth curve to soften ultrasonic DAC hash", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = flacProfile.rollOffFilterEnabled && !flacProfile.bitPerfectMode,
                        onCheckedChange = { dspManager.setFlacRollOff(it) },
                        enabled = !flacProfile.bitPerfectMode,
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDark, checkedTrackColor = CyanAccent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: AAC & LOSSY DEEP SETTINGS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AAC / Lossy Audio Tuning",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "DSEE Spectral Harmonics Reconstruction",
                            color = GoldAccent,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DSEE Recovery Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High-Frequency Harmonics Recovery", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Synthesizes subtle 16kHz - 24kHz harmonics lost in AAC compression", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = aacProfile.dseeHighFrequencyRecovery,
                        onCheckedChange = { dspManager.setAacDseeRecovery(it, aacProfile.harmonicIntensity) },
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDark, checkedTrackColor = GoldAccent)
                    )
                }

                if (aacProfile.dseeHighFrequencyRecovery) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Air / Sheen", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                        Slider(
                            value = aacProfile.harmonicIntensity,
                            onValueChange = { dspManager.setAacDseeRecovery(true, it) },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldAccent,
                                activeTrackColor = GoldAccent,
                                inactiveTrackColor = AudioTrackInactive
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(aacProfile.harmonicIntensity * 100).toInt()}%",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }
    }
}

