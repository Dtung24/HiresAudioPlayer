package com.hiresaudioplayer.app.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TrackRepository(private val context: Context) {

    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "imported_tracks.json")

    suspend fun loadAllTracks(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MusicTrack>()

        // 1. Load imported Google Drive & manual files from cache
        val imported = loadImportedTracks()
        tracks.addAll(imported)

        // 2. Query device MediaStore for audio
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    // Avoid duplicate if already imported
                    if (tracks.none { t -> t.uriString == contentUri.toString() }) {
                        val title = it.getString(titleCol) ?: "Unknown Track"
                        val artist = it.getString(artistCol) ?: "Unknown Artist"
                        val album = it.getString(albumCol) ?: "Unknown Album"
                        val duration = it.getLong(durCol)
                        val size = it.getLong(sizeCol)
                        val mime = it.getString(mimeCol) ?: ""

                        val codec = when {
                            mime.contains("flac", ignoreCase = true) -> "FLAC"
                            mime.contains("aac", ignoreCase = true) || mime.contains("mp4", ignoreCase = true) -> "AAC"
                            mime.contains("wav", ignoreCase = true) -> "WAV"
                            mime.contains("alac", ignoreCase = true) -> "ALAC"
                            else -> "MP3"
                        }

                        tracks.add(
                            MusicTrack(
                                id = "media_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                uriString = contentUri.toString(),
                                sampleRate = if (codec == "FLAC") 96000 else 44100,
                                bitDepth = if (codec == "FLAC") 24 else 16,
                                codec = codec,
                                fileSize = size,
                                source = AudioSource.LOCAL
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    suspend fun inspectAndAddTrack(uri: Uri, source: AudioSource, suggestedName: String? = null): MusicTrack = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title = suggestedName ?: "Audio File"
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var durationMs = 0L
        var sampleRate = 44100
        var bitDepth = 16
        var bitRate = 0
        var codec = "FLAC"

        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { bitRate = it / 1000 }

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            val uriStr = uri.toString().lowercase()
            codec = when {
                mime.contains("flac") || uriStr.endsWith(".flac") -> "FLAC"
                mime.contains("aac") || uriStr.endsWith(".aac") || uriStr.endsWith(".m4a") -> "AAC"
                mime.contains("wav") || uriStr.endsWith(".wav") -> "WAV"
                mime.contains("alac") -> "ALAC"
                else -> "MP3"
            }

            if (codec == "FLAC") {
                sampleRate = 96000
                bitDepth = 24
            } else if (codec == "AAC") {
                sampleRate = 48000
                bitDepth = 16
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        val track = MusicTrack(
            id = "track_${System.currentTimeMillis()}_${(0..9999).random()}",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uriString = uri.toString(),
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            bitRateKbps = bitRate,
            codec = codec,
            fileSize = 0L,
            source = source
        )

        saveTrackToCache(track)
        track
    }

    private fun loadImportedTracks(): List<MusicTrack> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            val json = cacheFile.readText()
            val type = object : TypeToken<List<MusicTrack>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun saveTrackToCache(track: MusicTrack) {
        val current = loadImportedTracks().toMutableList()
        current.removeAll { it.uriString == track.uriString }
        current.add(0, track)
        try {
            val json = gson.toJson(current)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeTrack(trackId: String) = withContext(Dispatchers.IO) {
        val current = loadImportedTracks().toMutableList()
        current.removeAll { it.id == trackId }
        cacheFile.writeText(gson.toJson(current))
    }
}
