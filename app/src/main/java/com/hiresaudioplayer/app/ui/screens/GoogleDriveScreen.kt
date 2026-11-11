package com.hiresaudioplayer.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.hiresaudioplayer.app.data.MusicTrack
import com.hiresaudioplayer.app.gdrive.GoogleDriveManager
import com.hiresaudioplayer.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GoogleDriveScreen(
    driveManager: GoogleDriveManager,
    onTrackImported: (MusicTrack) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var shareUrlInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val downloadProgress by driveManager.downloadStatus.collectAsState()

    // 1. Single Audio File Picker (SAF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Porting audio file from Google Drive..."
                try {
                    val track = driveManager.portFromSafUri(uri)
                    statusMessage = "Successfully imported: ${track.title} [${track.codec} ${track.formattedSampleRate}]"
                    onTrackImported(track)
                } catch (e: Exception) {
                    statusMessage = "Import failed: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // 2. Folder Tree Picker (SAF)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Scanning and importing folder from Google Drive..."
                try {
                    val tracks = driveManager.portFromFolderTree(treeUri) { current, total ->
                        statusMessage = "Importing file $current of $total from Drive..."
                    }
                    statusMessage = "Successfully imported ${tracks.size} tracks from Google Drive!"
                    if (tracks.isNotEmpty()) {
                        onTrackImported(tracks.first())
                    }
                } catch (e: Exception) {
                    statusMessage = "Folder import failed: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Google Drive Porting",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Directly import FLAC, AAC and Hi-Res audio from Google Drive storage",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Card 1: System Picker (Fast & Zero API Key Setup)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "System Drive Selector (SAF)",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select files or entire folders stored on your Google Drive account using the built-in Android file picker. Fast, secure, and no API key configuration needed.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf("audio/*", "application/octet-stream", "*/*")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BgDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Folder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Direct Share Link Downloader
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Download via Drive Share Link",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paste a Google Drive share link (e.g. https://drive.google.com/file/d/.../view) to directly stream and port high-resolution FLAC/AAC files to local storage.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = shareUrlInput,
                    onValueChange = { shareUrlInput = it },
                    placeholder = { Text("Paste Google Drive link or file ID here...", color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDarkVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (shareUrlInput.isNotBlank()) {
                            coroutineScope.launch {
                                isProcessing = true
                                val result = driveManager.downloadFromShareUrl(shareUrlInput)
                                result.onSuccess { track ->
                                    statusMessage = "Downloaded: ${track.title} [${track.codec}]"
                                    shareUrlInput = ""
                                    onTrackImported(track)
                                }.onFailure { e ->
                                    statusMessage = "Download failed: ${e.message}"
                                }
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing && shareUrlInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download & Port to Library", fontWeight = FontWeight.Bold)
                }

                // Download Progress
                downloadProgress?.let { prog ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = prog.fileName.ifBlank { "Connecting..." },
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${prog.percent}%",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (prog.percent / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = CyanAccent,
                            trackColor = AudioTrackInactive
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card
        if (statusMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoldAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = statusMessage ?: "",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Info / Audiophile Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Audiophile Storage Tips", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 24-bit / 192 kHz FLAC files are ported in pure bit-for-bit lossless format without compression.\n• Files ported from Google Drive are stored directly in your app's isolated high-speed storage for instantaneous low-latency playback.\n• Use the DSP Engine tab to enable Meier Binaural Crossfeed or Upsampling up to 192 kHz on playback.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

