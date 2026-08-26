package com.example.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
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
                    bind(InetSocketAddress(NetworkUtils.FILE_PORT))
                }
                Log.d(TAG, "P2P File Server listening on port ${NetworkUtils.FILE_PORT}")

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
     * Handles an incoming TCP request from a peer wanting to download a file.
     */
    private fun handleClientDownloadRequest(socket: Socket) {
        try {
            socket.soTimeout = 30000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return

            // Protocol: DOWNLOAD <fileId>
            val parts = requestLine.trim().split(" ")
            if (parts.size >= 2 && parts[0] == "DOWNLOAD") {
                val fileId = parts[1]
                val file = sharedFilesMap[fileId]

                val outStream = BufferedOutputStream(socket.getOutputStream())

                if (file != null && file.exists()) {
                    // Send header: OK <length> <fileName>
                    val header = "OK ${file.length()} ${file.name}\n"
                    outStream.write(header.toByteArray(Charsets.UTF_8))
                    outStream.flush()

                    // Stream file bytes
                    val fileIn = BufferedInputStream(FileInputStream(file))
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                    outStream.flush()
                    fileIn.close()
                    Log.d(TAG, "Successfully transferred file: ${file.name} to ${socket.inetAddress.hostAddress}")
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
                    input.copyTo(output)
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
     * Downloads a file directly from peer's IP via high-speed TCP socket.
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
            // Update downloading state
            _downloadingIds.value = _downloadingIds.value + messageId
            _downloadProgressMap.value = _downloadProgressMap.value + (messageId to 0.05f)

            socket = Socket()
            socket.connect(InetSocketAddress(senderIp, NetworkUtils.FILE_PORT), 8000)
            socket.soTimeout = 60000

            val outStream = socket.getOutputStream()
            val request = "DOWNLOAD $fileId\n"
            outStream.write(request.toByteArray(Charsets.UTF_8))
            outStream.flush()

            val inStream = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(inStream))
            val responseHeader = reader.readLine() ?: throw Exception("Empty server response")

            val headerParts = responseHeader.trim().split(" ")
            if (headerParts.isEmpty() || headerParts[0] != "OK") {
                throw Exception("Server rejected download: $responseHeader")
            }

            val remoteSize = headerParts.getOrNull(1)?.toLongOrNull() ?: expectedSize
            val cleanName = if (headerParts.size >= 3) headerParts.subList(2, headerParts.size).joinToString(" ") else fileName

            val downloadsDir = File(context.filesDir, "downloaded_files").apply { mkdirs() }
            val destinationFile = File(downloadsDir, cleanName)

            val fileOut = FileOutputStream(destinationFile)
            val buffer = ByteArray(32768)
            var totalRead = 0L
            var lastReportTime = System.currentTimeMillis()

            // Note: Use inStream directly (or wrap appropriately)
            var bytesRead: Int
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                fileOut.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportTime > 100 || totalRead >= remoteSize) {
                    lastReportTime = now
                    val progress = if (remoteSize > 0) (totalRead.toFloat() / remoteSize).coerceIn(0f, 1f) else 0.5f
                    _downloadProgressMap.value = _downloadProgressMap.value + (messageId to progress)
                }

                if (remoteSize > 0 && totalRead >= remoteSize) {
                    break
                }
            }

            fileOut.flush()
            fileOut.close()

            _downloadProgressMap.value = _downloadProgressMap.value + (messageId to 1.0f)
            Log.d(TAG, "Downloaded file successfully: ${destinationFile.absolutePath} ($totalRead bytes)")

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

    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        } else {
            "*/*"
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
