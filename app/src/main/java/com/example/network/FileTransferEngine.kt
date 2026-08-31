package com.example.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.utils.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class SharedFileInfo(
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val localFilePath: String
)

class FileTransferEngine(private val context: Context) {

    private val TAG = "FileTransferEngine"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // High-performance chunk buffer: 512 KB for ultra-fast local Wi-Fi transfer (up to 100+ MB/s)
    private val BUFFER_SIZE = 512 * 1024
    private val SOCKET_BUFFER_SIZE = 4 * 1024 * 1024 // 4 MB socket buffer for max throughput

    // Map of fileId -> File on local storage
    private val sharedFilesMap = ConcurrentHashMap<String, File>()

    // Map of messageId -> download progress (0.0 to 1.0)
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap = _downloadProgressMap.asStateFlow()

    // Map of messageId -> isDownloading
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    init {
        startServer()
    }

    /**
     * Starts the TCP file server for P2P direct transfers over Wi-Fi.
     */
    fun startServer() {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    receiveBufferSize = SOCKET_BUFFER_SIZE
                    setPerformancePreferences(0, 1, 2)
                    bind(InetSocketAddress(NetworkUtils.FILE_PORT))
                }
                Log.d(TAG, "P2P Ultra-Fast File Server listening on port ${NetworkUtils.FILE_PORT}")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: continue
                        scope.launch {
                            handleClientDownloadRequest(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in File Server socket", e)
            }
        }
    }

    /**
     * Handles an incoming TCP request from a peer wanting to download or resume a file.
     * Protocol: DOWNLOAD <fileId> [offset]
     */
    private fun handleClientDownloadRequest(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.trafficClass = 0x08 // IPTOS_THROUGHPUT (Maximize Network Throughput)
            socket.setPerformancePreferences(0, 1, 2)
            socket.sendBufferSize = SOCKET_BUFFER_SIZE
            socket.receiveBufferSize = SOCKET_BUFFER_SIZE
            socket.soTimeout = 45000

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return

            val parts = requestLine.trim().split(" ")
            if (parts.size >= 2 && parts[0] == "DOWNLOAD") {
                val fileId = parts[1]
                val offset = if (parts.size >= 3) parts[2].toLongOrNull() ?: 0L else 0L
                val file = sharedFilesMap[fileId]

                val outStream = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)

                if (file != null && file.exists()) {
                    val totalSize = file.length()
                    // Send header: OK <totalSize> <fileName> <offset>
                    val header = "OK $totalSize ${file.name} $offset\n"
                    outStream.write(header.toByteArray(Charsets.UTF_8))
                    outStream.flush()

                    val fileIn = BufferedInputStream(FileInputStream(file), BUFFER_SIZE)
                    if (offset > 0) {
                        var skipped = 0L
                        while (skipped < offset) {
                            val count = fileIn.skip(offset - skipped)
                            if (count <= 0) break
                            skipped += count
                        }
                    }

                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                    outStream.flush()
                    fileIn.close()
                    Log.d(TAG, "Successfully served file: ${file.name} (from offset $offset/$totalSize) to ${socket.inetAddress.hostAddress}")
                } else {
                    outStream.write("ERROR FILE_NOT_FOUND\n".toByteArray(Charsets.UTF_8))
                    outStream.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client download request", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Prepares and stages a local file for P2P sharing.
     */
    suspend fun stageFileForSharing(uri: Uri): SharedFileInfo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)

            val sharedDir = File(context.filesDir, "shared_p2p_files").apply { mkdirs() }
            val fileId = UUID.randomUUID().toString().substring(0, 8)
            val stagedFile = File(sharedDir, "${fileId}_$fileName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }

            if (fileSize <= 0) {
                fileSize = stagedFile.length()
            }

            // Register in memory map for serving
            sharedFilesMap[fileId] = stagedFile
            Log.d(TAG, "Staged file for P2P sharing: $fileName ($fileSize bytes) id=$fileId")

            SharedFileInfo(
                fileId = fileId,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                localFilePath = stagedFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage file for sharing", e)
            null
        }
    }

    /**
     * Prepares and stages an existing local file (e.g. recorded voice note) for P2P sharing without re-copying.
     */
    fun stageExistingFile(file: File, mimeType: String = "audio/m4a"): SharedFileInfo {
        val fileId = UUID.randomUUID().toString().substring(0, 8)
        sharedFilesMap[fileId] = file
        Log.d(TAG, "Staged existing local file: ${file.name} ($fileId)")
        return SharedFileInfo(
            fileId = fileId,
            fileName = file.name,
            fileSize = file.length(),
            mimeType = mimeType,
            localFilePath = file.absolutePath
        )
    }

    /**
     * Downloads a file directly from peer's IP via ultra-fast TCP socket with auto-resumption support.
     */
    suspend fun downloadFileFromPeer(
        messageId: String,
        fileId: String,
        fileName: String,
        expectedSize: Long,
        senderIp: String
    ): File? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            _downloadingIds.value = _downloadingIds.value + messageId

            val categoryDir = StorageUtils.getCategoryDir(context, fileName)
            val destinationFile = File(categoryDir, fileName)

            // If file is already fully downloaded and matches size, return it directly
            if (destinationFile.exists() && expectedSize > 0 && destinationFile.length() == expectedSize) {
                _downloadProgressMap.value = _downloadProgressMap.value + (messageId to 1.0f)
                return@withContext destinationFile
            }

            // Check if there is a partial download in progress for resumption
            val tempDir = File(context.cacheDir, "p2p_downloads_temp").apply { mkdirs() }
            val partFile = File(tempDir, "${fileId}_${fileName}.part")
            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            val initialProgress = if (expectedSize > 0 && existingBytes > 0) {
                (existingBytes.toFloat() / expectedSize).coerceIn(0.01f, 0.99f)
            } else 0.05f
            _downloadProgressMap.value = _downloadProgressMap.value + (messageId to initialProgress)

            socket = Socket().apply {
                tcpNoDelay = true
                trafficClass = 0x08 // IPTOS_THROUGHPUT
                setPerformancePreferences(0, 1, 2)
                sendBufferSize = SOCKET_BUFFER_SIZE
                receiveBufferSize = SOCKET_BUFFER_SIZE
                soTimeout = 45000
            }
            socket.connect(InetSocketAddress(senderIp, NetworkUtils.FILE_PORT), 8000)

            val outStream = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)
            // Send request with existing offset for resumption
            val request = "DOWNLOAD $fileId $existingBytes\n"
            outStream.write(request.toByteArray(Charsets.UTF_8))
            outStream.flush()

            val inStream = BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)
            val reader = BufferedReader(InputStreamReader(inStream))
            val responseHeader = reader.readLine() ?: throw Exception("Empty server response")

            val headerParts = responseHeader.trim().split(" ")
            if (headerParts.isEmpty() || headerParts[0] != "OK") {
                throw Exception("Server rejected download: $responseHeader")
            }

            val remoteTotalSize = headerParts.getOrNull(1)?.toLongOrNull() ?: expectedSize
            val startOffset = if (headerParts.size >= 4) headerParts[3].toLongOrNull() ?: 0L else existingBytes

            // Open part file in append mode if starting from offset > 0
            val appendMode = startOffset > 0 && partFile.exists() && partFile.length() == startOffset
            val fileOut = FileOutputStream(partFile, appendMode)

            val buffer = ByteArray(BUFFER_SIZE)
            var totalDownloaded = if (appendMode) startOffset else 0L
            var lastReportTime = System.currentTimeMillis()

            var bytesRead: Int
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                fileOut.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportTime > 80 || (remoteTotalSize > 0 && totalDownloaded >= remoteTotalSize)) {
                    lastReportTime = now
                    val progress = if (remoteTotalSize > 0) (totalDownloaded.toFloat() / remoteTotalSize).coerceIn(0f, 1f) else 0.5f
                    _downloadProgressMap.value = _downloadProgressMap.value + (messageId to progress)
                }

                if (remoteTotalSize > 0 && totalDownloaded >= remoteTotalSize) {
                    break
                }
            }

            fileOut.flush()
            fileOut.close()

            // When completely downloaded, rename from .part to final destination in LocalConnect directory
            if (partFile.exists()) {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                val moved = partFile.renameTo(destinationFile)
                if (!moved) {
                    partFile.copyTo(destinationFile, overwrite = true)
                    partFile.delete()
                }
            }

            // Scan file with MediaScanner to immediately show in Gallery / Files
            val mime = getMimeTypeFromFileName(destinationFile.name)
            StorageUtils.scanFile(context, destinationFile, mime)

            _downloadProgressMap.value = _downloadProgressMap.value + (messageId to 1.0f)
            Log.d(TAG, "Downloaded file successfully: ${destinationFile.absolutePath} ($totalDownloaded bytes)")

            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading file from $senderIp", e)
            _downloadProgressMap.value = _downloadProgressMap.value - messageId
            null
        } finally {
            _downloadingIds.value = _downloadingIds.value - messageId
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Opens a local downloaded or staged file using an external viewer.
     */
    fun openFile(filePath: String, mimeType: String?) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val resolvedMimeType = mimeType ?: getMimeTypeFromFileName(file.name)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "فتح الملف باستخدام").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not open with specific MIME type, trying generic */*", e)
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(genericIntent, "فتح الملف").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open file", ex)
            }
        }
    }

    fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return "*/*"

        // Explicit lookup for common specialized Android and multimedia formats
        return when (extension) {
            "apk" -> "application/vnd.android.package-archive"
            "xapk", "apks" -> "application/octet-stream"
            "obb" -> "application/octet-stream"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz", "gzip" -> "application/gzip"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt", "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "iso", "bin", "img", "dat" -> "application/octet-stream"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }
    }

    fun stop() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
