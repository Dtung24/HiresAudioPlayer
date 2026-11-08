package com.hiresaudioplayer.app.gdrive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.hiresaudioplayer.app.data.AudioSource
import com.hiresaudioplayer.app.data.MusicTrack
import com.hiresaudioplayer.app.data.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class DownloadProgress(
    val fileId: String,
    val fileName: String,
    val bytesRead: Long,
    val totalBytes: Long,
    val percent: Int,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class GoogleDriveManager(
    private val context: Context,
    private val repository: TrackRepository
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val hiresDir = File(context.filesDir, "hires_library").apply {
        if (!exists()) mkdirs()
    }

    private val _downloadStatus = MutableStateFlow<DownloadProgress?>(null)
    val downloadStatus: StateFlow<DownloadProgress?> = _downloadStatus.asStateFlow()

    /**
     * Port a file picked via Android System SAF (supports Google Drive, OneDrive, Device)
     */
    suspend fun portFromSafUri(uri: Uri): MusicTrack = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(uri) ?: "HiresAudio_${System.currentTimeMillis()}.flac"
        val destinationFile = File(hiresDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val localUri = Uri.fromFile(destinationFile)
        repository.inspectAndAddTrack(localUri, AudioSource.GOOGLE_DRIVE, fileName)
    }

    /**
     * Port all audio files from a Google Drive folder picked via SAF
     */
    suspend fun portFromFolderTree(treeUri: Uri, onProgress: (Int, Int) -> Unit): List<MusicTrack> = withContext(Dispatchers.IO) {
        val importedTracks = mutableListOf<MusicTrack>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val audioFiles = rootDoc.listFiles().filter {
            val name = it.name?.lowercase() ?: ""
            name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".aac") ||
                    name.endsWith(".m4a") || name.endsWith(".alac") || name.endsWith(".mp3")
        }

        for (i in audioFiles.indices) {
            val doc = audioFiles[i]
            val fileName = doc.name ?: "track_$i.flac"
            val target = File(hiresDir, fileName)

            context.contentResolver.openInputStream(doc.uri)?.use { inStream ->
                FileOutputStream(target).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            val track = repository.inspectAndAddTrack(Uri.fromFile(target), AudioSource.GOOGLE_DRIVE, fileName)
            importedTracks.add(track)
            onProgress(i + 1, audioFiles.size)
        }

        importedTracks
    }

    /**
     * Parse Google Drive URL and download file directly
     */
    suspend fun downloadFromShareUrl(url: String): Result<MusicTrack> = withContext(Dispatchers.IO) {
        val fileId = extractGoogleDriveId(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Google Drive Link or File ID"))

        val directUrl = "https://docs.google.com/uc?export=download&id=$fileId&confirm=t"
        _downloadStatus.value = DownloadProgress(fileId, "Connecting to Drive...", 0, -1, 0)

        try {
            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Download failed: HTTP ${response.code}"
                _downloadStatus.value = DownloadProgress(fileId, "", 0, -1, 0, false, err)
                return@withContext Result.failure(Exception(err))
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()

            // Try to extract filename from header
            var resolvedName = "DriveAudio_${fileId.take(6)}.flac"
            val contentDisposition = response.header("Content-Disposition")
            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                val match = Pattern.compile("filename=\"?([^\";]+)\"?").matcher(contentDisposition)
                if (match.find()) {
                    resolvedName = match.group(1) ?: resolvedName
                }
            }

            val targetFile = File(hiresDir, resolvedName)
            val buffer = ByteArray(8192)
            var bytesReadTotal = 0L

            body.byteStream().use { inStream ->
                FileOutputStream(targetFile).use { outStream ->
                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        bytesReadTotal += read

                        val percent = if (totalBytes > 0) ((bytesReadTotal * 100) / totalBytes).toInt() else 0
                        _downloadStatus.value = DownloadProgress(
                            fileId = fileId,
                            fileName = resolvedName,
                            bytesRead = bytesReadTotal,
                            totalBytes = totalBytes,
                            percent = percent
                        )
                    }
                }
            }

            _downloadStatus.value = DownloadProgress(
                fileId = fileId,
                fileName = resolvedName,
                bytesRead = bytesReadTotal,
                totalBytes = totalBytes,
                percent = 100,
                isCompleted = true
            )

            val track = repository.inspectAndAddTrack(Uri.fromFile(targetFile), AudioSource.GOOGLE_DRIVE, resolvedName)
            Result.success(track)
        } catch (e: Exception) {
            _downloadStatus.value = DownloadProgress(fileId, "", 0, -1, 0, false, e.message)
            Result.failure(e)
        }
    }

    private fun extractGoogleDriveId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("[a-zA-Z0-9_-]{25,}"))) {
            return trimmed
        }
        val pattern = Pattern.compile("/d/([a-zA-Z0-9_-]+)|id=([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1) ?: matcher.group(2)
        }
        return null
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }
}

