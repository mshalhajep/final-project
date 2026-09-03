package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

object StorageUtils {
    private const val TAG = "StorageUtils"
    private const val APP_DIR_NAME = "LocalConnect"

    fun getAppStorageDir(context: Context): File {
        val publicDownloads = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "downloads")
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
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
     * Registers a downloaded file with the system media index so it instantly shows
     * up in the Files app, Downloads and Gallery.
     *
     * Android 10+ (Scoped Storage): the file bytes are copied into MediaStore under
     * "Download/LocalConnect/<category>" via ContentResolver — direct public-folder
     * writes are restricted, and MediaScanner cannot index app-external files without
     * legacy storage flags.
     * Android 9 and below: classic MediaScanner path scan.
     */
    fun registerDownloadedFile(context: Context, file: File, mimeType: String?): Uri? {
        val resolvedMime = mimeType ?: "application/octet-stream"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                val resolver = context.contentResolver
                val downloads = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val relativeFolder = "LocalConnect/${subfolderFor(file.name, resolvedMime)}"

                // Reuse an existing entry if the same file was registered before
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
                resolver.query(downloads, projection, selection, arrayOf("$relativeFolder/", file.name), null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        Log.d(TAG, "MediaStore entry already present: $relativeFolder/${file.name}")
                        return Uri.withAppendedPath(downloads, id.toString())
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, resolvedMime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$relativeFolder/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collectionUri = resolver.insert(downloads, values) ?: return null
                resolver.openOutputStream(collectionUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output, 512 * 1024)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(collectionUri, values, null, null)
                Log.d(TAG, "Registered $relativeFolder/${file.name} into MediaStore Downloads")
                collectionUri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register file into MediaStore — falling back to scan", e)
                scanFile(context, file, resolvedMime)
                null
            }
        } else {
            scanFile(context, file, resolvedMime)
            return null
        }
    }

    private fun subfolderFor(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mime = mimeType.lowercase()
        return when {
            mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp") -> "Images"
            mime.startsWith("video/") || ext in listOf("mp4", "mkv", "avi", "webm", "mov", "3gp", "flv") -> "Videos"
            mime.startsWith("audio/") || ext in listOf("mp3", "m4a", "wav", "ogg", "aac", "flac") -> "Audio"
            else -> "Documents"
        }
    }

    /**
     * Saves a decoded bitmap into the public gallery (MediaStore.Images on Android
     * 10+, MediaScanner on older releases). Returns true on success.
     */
    fun saveBitmapToGallery(context: Context, bitmap: android.graphics.Bitmap, displayName: String): Boolean {
        return try {
            val resolvedName = if (displayName.contains('.')) displayName else "$displayName.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, resolvedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$APP_DIR_NAME")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    APP_DIR_NAME
                ).apply { mkdirs() }
                val file = File(dir, resolvedName)
                file.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
                }
                scanFile(context, file, "image/jpeg")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to gallery", e)
            false
        }
    }

    /**
     * Decodes a content-uri bitmap with a sub-sampling factor capped to [maxDim]
     * on the long edge. Decoding raw 4K camera photos directly allocates 50MB+
     * per pick and can OOM low-memory devices.
     */
    fun decodeSampledBitmap(context: Context, uri: android.net.Uri, maxDim: Int = 1280): android.graphics.Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) {
                sample *= 2
            }
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode sampled bitmap", e)
            null
        }
    }

    /**
     * Scans newly saved or downloaded file with Android MediaScanner so it instantly appears
     * in the device's Gallery, Music, and File Manager apps (Android 9 and below).
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
