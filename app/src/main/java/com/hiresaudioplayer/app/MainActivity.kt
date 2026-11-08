package com.hiresaudioplayer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.hiresaudioplayer.app.audio.AudioPlaybackService
import com.hiresaudioplayer.app.audio.HiResAudioPlayer
import com.hiresaudioplayer.app.data.AudioSource
import com.hiresaudioplayer.app.data.MusicTrack
import com.hiresaudioplayer.app.data.TrackRepository
import com.hiresaudioplayer.app.dsp.DspManager
import com.hiresaudioplayer.app.gdrive.GoogleDriveManager
import com.hiresaudioplayer.app.ui.AppBottomNavigation
import com.hiresaudioplayer.app.ui.AppDestination
import com.hiresaudioplayer.app.ui.screens.*
import com.hiresaudioplayer.app.ui.theme.BgDark
import com.hiresaudioplayer.app.ui.theme.HiresAudioPlayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TrackRepository
    private lateinit var dspManager: DspManager
    private lateinit var driveManager: GoogleDriveManager
    private lateinit var player: HiResAudioPlayer

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions granted, reload tracks
        loadTracks()
    }

    private var tracksState = mutableStateOf<List<MusicTrack>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = TrackRepository(applicationContext)
        dspManager = DspManager()
        driveManager = GoogleDriveManager(applicationContext, repository)
        player = HiResAudioPlayer(applicationContext, dspManager)

        startForegroundPlaybackService()
        checkAndRequestPermissions()
        loadTracks()

        // Handle external Intent (e.g. opened from Google Drive app)
        handleIntent(intent)

        setContent {
            HiresAudioPlayerTheme {
                var currentDestination by remember { mutableStateOf(AppDestination.NOW_PLAYING) }
                val tracks by tracksState

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgDark,
                    bottomBar = {
                        AppBottomNavigation(
                            currentDestination = currentDestination,
                            onDestinationSelected = { currentDestination = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            AppDestination.NOW_PLAYING -> {
                                NowPlayingScreen(
                                    player = player,
                                    dspManager = dspManager,
                                    onNavigateToEq = { currentDestination = AppDestination.EQUALIZER },
                                    onNavigateToDsp = { currentDestination = AppDestination.DSP_SETTINGS }
                                )
                            }
                            AppDestination.LIBRARY -> {
                                LibraryScreen(
                                    tracks = tracks,
                                    player = player,
                                    onNavigateToDrive = { currentDestination = AppDestination.GOOGLE_DRIVE },
                                    onTrackSelected = { track ->
                                        player.playTrack(track)
                                        currentDestination = AppDestination.NOW_PLAYING
                                    }
                                )
                            }
                            AppDestination.EQUALIZER -> {
                                EqualizerScreen(dspManager = dspManager)
                            }
                            AppDestination.DSP_SETTINGS -> {
                                DspSettingsScreen(dspManager = dspManager)
                            }
                            AppDestination.GOOGLE_DRIVE -> {
                                GoogleDriveScreen(
                                    driveManager = driveManager,
                                    onTrackImported = { newTrack ->
                                        loadTracks()
                                        player.playTrack(newTrack)
                                        currentDestination = AppDestination.NOW_PLAYING
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadTracks() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            tracksState.value = repository.loadAllTracks()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    val track = repository.inspectAndAddTrack(uri, AudioSource.LOCAL)
                    loadTracks()
                    player.playTrack(track)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun startForegroundPlaybackService() {
        val serviceIntent = Intent(this, AudioPlaybackService::class.java).apply {
            putExtra("EXTRA_TITLE", "Aether HiFi Player")
            putExtra("EXTRA_SUBTITLE", "192 kHz Hi-Res Audio Engine Ready")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
    }
}

