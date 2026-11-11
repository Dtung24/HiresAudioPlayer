package com.hiresaudioplayer.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiresaudioplayer.app.audio.HiResAudioPlayer
import com.hiresaudioplayer.app.audio.PlaybackState
import com.hiresaudioplayer.app.dsp.DspManager
import com.hiresaudioplayer.app.ui.components.SpectrumVisualizer
import com.hiresaudioplayer.app.ui.theme.*

@Composable
fun NowPlayingScreen(
    player: HiResAudioPlayer,
    dspManager: DspManager,
    onNavigateToEq: () -> Unit,
    onNavigateToDsp: () -> Unit
) {
    val track by player.currentTrack.collectAsState()
    val playbackState by player.playbackState.collectAsState()
    val currentPos by player.currentPositionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val outputRate by player.activeOutputSampleRate.collectAsState()
    val visualizerBins by player.visualizerBins.collectAsState()

    val isPlaying = playbackState == PlaybackState.PLAYING

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Bar & Hi-Res Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDarkVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CyanAccent else GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HI-RES AUDIO  •  ${outputRate / 1000} kHz / 32-bit Float",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Codec tag
            val codecInfo = track?.let {
                "${it.codec} ${it.formattedSampleRate}"
            } ?: "FLAC 192kHz Ready"

            Text(
                text = codecInfo,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // 2. Vinyl Center Artwork
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(2.dp, BorderDark, CircleShape)
                .rotate(if (isPlaying) angle else 0f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                // Vinyl grooves
                for (r in listOf(0.85f, 0.70f, 0.55f)) {
                    drawCircle(
                        color = Color(0xFF232630),
                        radius = radius * r,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }

            // Center gold label
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(GoldAccent, GoldDark)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = BgDark,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 3. Track Info & Visualizer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = track?.title ?: "Select a Track from Library",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track?.artist ?: "Aether Audiophile Engine",
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Spectrum Visualizer
            SpectrumVisualizer(
                magnitudes = visualizerBins,
                height = 54.dp,
                barCount = 36
            )
        }

        // 4. Progress Scrubber
        Column(modifier = Modifier.fillMaxWidth()) {
            val progress = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    player.seekTo((frac * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = AudioTrackInactive
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPos),
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // 5. Playback Controls & Action Shortcuts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigateToEq() }) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = { player.seekTo((currentPos - 10000).coerceAtLeast(0)) }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Play / Pause FAB
                FilledIconButton(
                    onClick = { player.togglePlayPause() },
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GoldAccent
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = BgDark,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { player.seekTo((currentPos + 10000).coerceAtMost(duration)) }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(onClick = { onNavigateToDsp() }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "DSP Settings",
                        tint = CyanAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

