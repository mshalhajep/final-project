package com.example.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File

object StorageUtils {
    private const val TAG = "StorageUtils"
    private const val APP_DIR_NAME = "LocalConnect"

    /**
     * Gets the main dedicated storage root directory for LocalConnect files.
     * Uses Public Downloads/LocalConnect or App External Files directory if unavailable.
     */
    fun getAppStorageDir(context: Context): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(publicDownloads, APP_DIR_NAME)
        if (!appFolder.exists()) {
            val created = appFolder.mkdirs()
            if (!created && !appFolder.exists()) {
                // Fallback to external files directory
                val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APP_DIR_NAME)
                fallback.mkdirs()
                return fallback
            }
        }
        return appFolder
    }

    /**
     * Returns the appropriate subfolder based on MIME type or file extension.
     * (Images, Videos, Audio, Documents)
     */
    fun getCategoryDir(context: Context, fileName: String, mimeType: String? = null): File {
        val baseDir = getAppStorageDir(context)
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.lowercase() ?: ""

        val folderName = when {
            mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp") -> "Images"
            mime.startsWith("video/") || ext in listOf("mp4", "mkv", "avi", "webm", "mov", "3gp", "flv") -> "Videos"
            mime.startsWith("audio/") || ext in listOf("mp3", "m4a", "wav", "ogg", "aac", "flac") -> "Audio"
            else -> "Documents"
        }

        val categoryFolder = File(baseDir, folderName)
        if (!categoryFolder.exists()) {
            categoryFolder.mkdirs()
        }
        return categoryFolder
    }

    /**
     * Scans newly saved or downloaded file with Android MediaScanner so it instantly appears
     * in the device's Gallery, Music, and File Manager apps.
     */
    fun scanFile(context: Context, file: File, mimeType: String? = null) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                if (mimeType != null) arrayOf(mimeType) else null
            ) { path, uri ->
                Log.d(TAG, "Scanned $path -> URI: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan file with MediaScanner", e)
        }
    }
}
