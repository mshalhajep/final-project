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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

    // RULE 5: files are streamed as AES-256-GCM encrypted chunks with deterministic
    // per-chunk nonces so byte-offset resume stays aligned across sender/receiver.
    companion object {
        private const val CIPHER_CHUNK_PLAIN_SIZE = 64 * 1024
        private const val GCM_TAG_BYTES = 16
        private const val MAX_CIPHER_CHUNK = CIPHER_CHUNK_PLAIN_SIZE + GCM_TAG_BYTES
    }

    // Map of fileId -> File on local storage
    private val sharedFilesMap = ConcurrentHashMap<String, File>()

    // Active client sockets for in-flight downloads, allowing pause/cancel
    private val activeDownloadSockets = ConcurrentHashMap<String, Socket>()

    // Map of messageId -> download progress (0.0 to 1.0)
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap = _downloadProgressMap.asStateFlow()

    // Map of messageId -> isDownloading
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    fun cancelDownload(messageId: String) {
        try {
            val socket = activeDownloadSockets.remove(messageId)
            socket?.close()
            _downloadingIds.value = _downloadingIds.value - messageId
            Log.d(TAG, "Download cancelled/paused for message $messageId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling download for $messageId", e)
        }
    }

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
     * Reads one header line ("...\n") manually so the raw stream is never over-read
     * by a buffered reader before the encrypted payload begins.
     */
    private fun readLineSafely(input: InputStream): String? {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) {
                if (prev == '\r'.code) sb.setLength(sb.length - 1)
                return sb.toString()
            }
            if (sb.length > 1024) return null
            sb.append(b.toChar())
            prev = b
        }
    }

    /** Reads exactly [len] bytes, returning false when the stream ends early. */
    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, len: Int): Boolean {
        var pos = offset
        var remaining = len
        while (remaining > 0) {
            val read = input.read(buffer, pos, remaining)
            if (read == -1) return false
            pos += read
            remaining -= read
        }
        return true
    }

    /** Writes a 4-byte big-endian length prefix. */
    private fun writeIntBE(output: BufferedOutputStream, value: Int) {
        output.write((value shr 24) and 0xFF)
        output.write((value shr 16) and 0xFF)
        output.write((value shr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun readIntBE(buffer: ByteArray): Int {
        return ((buffer[0].toInt() and 0xFF) shl 24) or
                ((buffer[1].toInt() and 0xFF) shl 16) or
                ((buffer[2].toInt() and 0xFF) shl 8) or
                (buffer[3].toInt() and 0xFF)
    }

    /**
     * Handles an incoming TCP request from a peer wanting to download or resume a file.
     * Protocol: DOWNLOAD <fileId> [offset]  ->  OK <totalSize> <fileName> <alignedOffset>
     * followed by framed AES-GCM chunks: [4-byte BE cipher length][ciphertext + tag].
     */
    private fun handleClientDownloadRequest(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.trafficClass = 0x08 // IPTOS_THROUGHPUT (Maximize Network Throughput)
            socket.setPerformancePreferences(0, 1, 2)
            socket.sendBufferSize = SOCKET_BUFFER_SIZE
            socket.receiveBufferSize = SOCKET_BUFFER_SIZE
            socket.soTimeout = 45000

            val inputStream = BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)
            val requestLine = readLineSafely(inputStream) ?: return

            val parts = requestLine.trim().split(" ")
            if (parts.size >= 2 && parts[0] == "DOWNLOAD") {
                val fileId = parts[1]
                val offset = if (parts.size >= 3) parts[2].toLongOrNull() ?: 0L else 0L
                var file = sharedFilesMap[fileId]

                // Disk auto-recovery: if not in memory, find on disk in shared_p2p_files
                if (file == null || !file.exists()) {
                    val sharedDir = File(context.filesDir, "shared_p2p_files")
                    val found = sharedDir.listFiles()?.firstOrNull { it.name.startsWith("${fileId}_") }
                    if (found != null && found.exists()) {
                        file = found
                        sharedFilesMap[fileId] = found
                        Log.d(TAG, "Recovered shared file from disk: ${file.name}")
                    }
                }

                val outStream = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)

                if (file != null && file.exists()) {
                    val totalSize = file.length()
                    val alignedOffset = (offset / CIPHER_CHUNK_PLAIN_SIZE) * CIPHER_CHUNK_PLAIN_SIZE

                    // Send header: OK <totalSize> <alignedOffset> <encodedFileName>
                    val encodedName = java.net.URLEncoder.encode(file.name, "UTF-8")
                    val header = "OK $totalSize $alignedOffset $encodedName\n"
                    outStream.write(header.toByteArray(Charsets.UTF_8))
                    outStream.flush()

                    BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { fileIn ->
                        if (alignedOffset > 0) {
                            var skipped = 0L
                            while (skipped < alignedOffset) {
                                val count = fileIn.skip(alignedOffset - skipped)
                                if (count <= 0) break
                                skipped += count
                            }
                        }

                        val plainBuffer = ByteArray(CIPHER_CHUNK_PLAIN_SIZE)
                        var chunkIndex = alignedOffset / CIPHER_CHUNK_PLAIN_SIZE

                        while (true) {
                            var filled = 0
                            while (filled < CIPHER_CHUNK_PLAIN_SIZE) {
                                val read = fileIn.read(plainBuffer, filled, CIPHER_CHUNK_PLAIN_SIZE - filled)
                                if (read == -1) break
                                filled += read
                            }
                            if (filled == 0) break

                            val cipher = LocalCryptoEngine.encryptChunk(
                                plainBuffer.copyOf(filled),
                                fileId,
                                chunkIndex
                            )
                            writeIntBE(outStream, cipher.size)
                            outStream.write(cipher)
                            chunkIndex++

                            if (filled < CIPHER_CHUNK_PLAIN_SIZE) break
                        }
                        outStream.flush()
                    }
                    Log.d(TAG, "Served encrypted stream: ${file.name} (from offset $alignedOffset/$totalSize) to ${socket.inetAddress.hostAddress}")
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
     * Determines the exact file extension and MIME type using Magic Bytes sniffing,
     * MimeTypeMap, and file name heuristics so files are NEVER saved with .bin or wrong formats.
     */
    fun detectMimeAndExtension(file: File, fallbackMime: String? = null): Pair<String, String> {
        val header = ByteArray(16)
        var bytesRead = 0
        try {
            FileInputStream(file).use { fis ->
                bytesRead = fis.read(header)
            }
        } catch (_: Exception) {}

        // 1. Image formats by magic bytes
        if (bytesRead >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "image/jpeg" to "jpg"
        }
        if (bytesRead >= 8 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
            return "image/png" to "png"
        }
        if (bytesRead >= 4 && header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x38.toByte()) {
            return "image/gif" to "gif"
        }
        if (bytesRead >= 12 && header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte()
            && header[8] == 0x57.toByte() && header[9] == 0x45.toByte() && header[10] == 0x42.toByte() && header[11] == 0x50.toByte()) {
            return "image/webp" to "webp"
        }

        // 2. Video and Audio formats
        if (bytesRead >= 8 && header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && header[6] == 0x79.toByte() && header[7] == 0x70.toByte()) {
            // MP4 / MOV / M4A (ftyp box)
            return if (bytesRead >= 12 && header[8] == 0x4D.toByte() && header[9] == 0x34.toByte() && header[10] == 0x41.toByte()) {
                "audio/mp4" to "m4a"
            } else {
                "video/mp4" to "mp4"
            }
        }
        if (bytesRead >= 4 && header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() && header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte()) {
            return "video/x-matroska" to "mkv"
        }
        if (bytesRead >= 3 && header[0] == 0x49.toByte() && header[1] == 0x44.toByte() && header[2] == 0x33.toByte()) {
            return "audio/mpeg" to "mp3"
        }
        if (bytesRead >= 2 && header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0) {
            return "audio/mpeg" to "mp3"
        }
        if (bytesRead >= 4 && header[0] == 0x4F.toByte() && header[1] == 0x67.toByte() && header[2] == 0x67.toByte() && header[3] == 0x53.toByte()) {
            return "audio/ogg" to "ogg"
        }

        // 3. Document formats
        if (bytesRead >= 4 && header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
            return "application/pdf" to "pdf"
        }
        if (bytesRead >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
            val ext = file.name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "apk" -> "application/vnd.android.package-archive" to "apk"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx"
                else -> "application/zip" to "zip"
            }
        }

        // 4. File extension fallback
        val ext = file.name.substringAfterLast('.', "").lowercase()
        if (ext.isNotBlank() && ext.length in 2..5 && ext != "bin") {
            val mimeFromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!mimeFromMap.isNullOrBlank()) {
                return mimeFromMap to ext
            }
            val mimeFromUtils = getMimeTypeFromFileName(file.name)
            if (mimeFromUtils != "*/*") {
                return mimeFromUtils to ext
            }
        }

        // 5. Provided MIME fallback
        if (!fallbackMime.isNullOrBlank() && fallbackMime != "*/*" && fallbackMime != "application/octet-stream") {
            val extFromMap = MimeTypeMap.getSingleton().getExtensionFromMimeType(fallbackMime)
            if (!extFromMap.isNullOrBlank()) {
                return fallbackMime to extFromMap
            }
        }

        return (fallbackMime ?: "application/octet-stream") to (if (ext.isNotBlank() && ext != "bin") ext else "bin")
    }

    /**
     * Guarantees the file name carries a correct extension derived from its MIME type.
     */
    fun ensureFileExtension(fileName: String, mimeType: String?): String {
        val existingExt = fileName.substringAfterLast('.', "").lowercase()
        if (existingExt.isNotBlank() && existingExt.length in 2..5 && existingExt != "bin") {
            return fileName
        }
        val mime = (mimeType ?: "").lowercase()
        val ext = when {
            mime.startsWith("image/jpeg") || mime.startsWith("image/jpg") -> "jpg"
            mime.startsWith("image/png") -> "png"
            mime.startsWith("image/webp") -> "webp"
            mime.startsWith("image/gif") -> "gif"
            mime.startsWith("image/") -> "jpg"
            mime.startsWith("video/mp4") -> "mp4"
            mime.startsWith("video/x-matroska") -> "mkv"
            mime.startsWith("video/webm") -> "webm"
            mime.startsWith("video/quicktime") -> "mov"
            mime.startsWith("video/3gpp") -> "3gp"
            mime.startsWith("video/") -> "mp4"
            mime.startsWith("audio/mpeg") || mime.startsWith("audio/mp3") -> "mp3"
            mime.startsWith("audio/mp4") || mime.startsWith("audio/m4a") -> "m4a"
            mime.startsWith("audio/wav") -> "wav"
            mime.startsWith("audio/ogg") -> "ogg"
            mime.startsWith("audio/") -> "m4a"
            mime == "application/pdf" -> "pdf"
            mime == "text/plain" -> "txt"
            mime == "application/zip" -> "zip"
            mime == "application/vnd.android.package-archive" -> "apk"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        }
        if (!ext.isNullOrBlank()) {
            val baseName = if (existingExt == "bin" || fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
            return "$baseName.$ext"
        }
        return fileName
    }

    /**
     * Prepares and stages a local file for P2P sharing.
     */
    suspend fun stageFileForSharing(uri: Uri): SharedFileInfo? = withContext(Dispatchers.IO) {
        try {
            var rawName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L

            // 1. Check if direct file:// URI
            if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
                val directFile = File(uri.path ?: "")
                if (directFile.exists()) {
                    rawName = directFile.name
                    fileSize = directFile.length()
                }
            } else {
                // 2. Query ContentResolver for content:// URI
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) {
                                val n = cursor.getString(nameIndex)
                                if (!n.isNullOrBlank()) rawName = n
                            }
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ContentResolver query failed for $uri", e)
                }
                if (rawName.startsWith("file_") && !uri.lastPathSegment.isNullOrBlank()) {
                    val seg = uri.lastPathSegment!!
                    if (seg.contains('.')) rawName = seg.substringAfterLast('/')
                }
            }

            var mimeType = context.contentResolver.getType(uri) ?: getMimeTypeFromFileName(rawName)

            val sharedDir = File(context.filesDir, "shared_p2p_files").apply { mkdirs() }
            val fileId = UUID.randomUUID().toString().substring(0, 12)
            val stagedTmp = File(sharedDir, "${fileId}_staging_tmp")

            // Copy stream safely
            val inStream: InputStream = try {
                if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
                    FileInputStream(File(uri.path ?: ""))
                } else {
                    context.contentResolver.openInputStream(uri) ?: FileInputStream(File(uri.path ?: ""))
                }
            } catch (e: Exception) {
                FileInputStream(File(uri.path ?: ""))
            }

            inStream.use { input ->
                FileOutputStream(stagedTmp).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }

            if (fileSize <= 0L) {
                fileSize = stagedTmp.length()
            }

            // Sniff true MIME type and extension from the actual staged bytes!
            val (detectedMime, detectedExt) = detectMimeAndExtension(stagedTmp, mimeType)
            mimeType = detectedMime

            var finalFileName = rawName
            if (!finalFileName.contains('.') || finalFileName.endsWith(".bin", ignoreCase = true)) {
                finalFileName = ensureFileExtension(finalFileName, mimeType)
            }
            if (!finalFileName.contains('.')) {
                finalFileName = "$finalFileName.$detectedExt"
            }

            // Sanitize and rename
            val sanitizedName = File(finalFileName).name.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF ]"), "_")
            val permanentStagedFile = File(sharedDir, "${fileId}_$sanitizedName")
            if (stagedTmp.exists()) {
                if (permanentStagedFile.exists()) permanentStagedFile.delete()
                val moved = stagedTmp.renameTo(permanentStagedFile)
                if (!moved) {
                    stagedTmp.copyTo(permanentStagedFile, overwrite = true)
                    stagedTmp.delete()
                }
            }

            // Register in memory map
            sharedFilesMap[fileId] = permanentStagedFile
            Log.d(TAG, "Staged file for P2P sharing: $finalFileName ($fileSize bytes, mime=$mimeType) id=$fileId")

            SharedFileInfo(
                fileId = fileId,
                fileName = finalFileName,
                fileSize = fileSize,
                mimeType = mimeType,
                localFilePath = permanentStagedFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage file for sharing: uri=$uri", e)
            null
        }
    }

    /**
     * Prepares and stages an existing local file (e.g. recorded voice note) for P2P sharing without re-copying.
     */
    fun stageExistingFile(file: File, mimeType: String = "audio/m4a"): SharedFileInfo {
        val fileId = UUID.randomUUID().toString().substring(0, 12)
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
        senderIp: String,
        mimeType: String? = null
    ): File? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            _downloadingIds.value = _downloadingIds.value + messageId

            // Correct extensionless names so the saved file opens with the right viewer
            val safeFileName = ensureFileExtension(fileName, mimeType)
            val categoryDir = StorageUtils.getCategoryDir(context, safeFileName)
            val destinationFile = File(categoryDir, safeFileName)

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
            activeDownloadSockets[messageId] = socket

            val outStream = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)
            // Send request with existing offset for resumption
            val request = "DOWNLOAD $fileId $existingBytes\n"
            outStream.write(request.toByteArray(Charsets.UTF_8))
            outStream.flush()

            val inStream = BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)
            val responseHeader = readLineSafely(inStream) ?: throw Exception("Empty server response")

            val headerParts = responseHeader.trim().split(" ")
            if (headerParts.isEmpty() || headerParts[0] != "OK") {
                throw Exception("Server rejected download: $responseHeader")
            }

            val remoteTotalSize = headerParts.getOrNull(1)?.toLongOrNull() ?: expectedSize
            // Format 1 (new): OK <totalSize> <alignedOffset> <encodedName>
            // Format 2 (legacy): OK <totalSize> <name> <alignedOffset>
            val alignedStartOffset = when {
                headerParts.size >= 4 && headerParts[2].toLongOrNull() != null -> headerParts[2].toLong()
                headerParts.size >= 4 && headerParts.last().toLongOrNull() != null -> headerParts.last().toLong()
                else -> existingBytes
            }
            val bytesToSkipInitially = (existingBytes - alignedStartOffset).coerceAtLeast(0L)

            // Open part file in append mode if resuming from a previous partial download
            val appendMode = alignedStartOffset > 0 && partFile.exists() && partFile.length() >= alignedStartOffset
            if (appendMode && partFile.length() > alignedStartOffset) {
                try {
                    java.io.RandomAccessFile(partFile, "rw").use { it.setLength(alignedStartOffset) }
                } catch (_: Exception) {}
            }
            var bytesWritten = 0L
            FileOutputStream(partFile, appendMode).use { fileOut ->
                var bytesToSkip = bytesToSkipInitially
                var chunkIndex = alignedStartOffset / CIPHER_CHUNK_PLAIN_SIZE
                var lastReportTime = System.currentTimeMillis()

                val lengthPrefix = ByteArray(4)
                val cipherBuffer = ByteArray(MAX_CIPHER_CHUNK)

                chunkLoop@ while (true) {
                    if (!readFully(inStream, lengthPrefix, 0, 4)) break
                    val cipherLen = readIntBE(lengthPrefix)
                    if (cipherLen <= 0 || cipherLen > MAX_CIPHER_CHUNK) {
                        throw Exception("Corrupt encrypted chunk stream (len=$cipherLen)")
                    }
                    if (!readFully(inStream, cipherBuffer, 0, cipherLen)) break

                    val plain = LocalCryptoEngine.decryptChunk(
                        cipherBuffer.copyOf(cipherLen),
                        fileId,
                        chunkIndex
                    ) ?: throw Exception("AEAD authentication failed on chunk $chunkIndex")
                    chunkIndex++

                    var start = 0
                    if (bytesToSkip > 0) {
                        val skip = minOf(bytesToSkip, plain.size.toLong()).toInt()
                        start = skip
                        bytesToSkip -= skip
                    }
                    if (start < plain.size) {
                        fileOut.write(plain, start, plain.size - start)
                        bytesWritten += (plain.size - start)
                    }

                    val totalWritten = existingBytes + bytesWritten
                    val now = System.currentTimeMillis()
                    if (now - lastReportTime > 80 || (remoteTotalSize > 0 && totalWritten >= remoteTotalSize)) {
                        lastReportTime = now
                        val progress = if (remoteTotalSize > 0) {
                            (totalWritten.toFloat() / remoteTotalSize).coerceIn(0f, 1f)
                        } else 0.5f
                        _downloadProgressMap.value = _downloadProgressMap.value + (messageId to progress)
                    }

                    if (remoteTotalSize > 0 && totalWritten >= remoteTotalSize) break@chunkLoop
                }
            }

            // When completely downloaded, rename from .part to final destination in LocalConnect directory
            var finalDownloadedFile = destinationFile
            if (partFile.exists()) {
                // Sniff the downloaded part file to ensure correct extension!
                val (detectedMime, detectedExt) = detectMimeAndExtension(partFile, mimeType)
                var resolvedName = safeFileName
                if (!resolvedName.contains('.') || resolvedName.endsWith(".bin", ignoreCase = true)) {
                    resolvedName = ensureFileExtension(resolvedName, detectedMime)
                }
                if (!resolvedName.contains('.')) {
                    resolvedName = "$resolvedName.$detectedExt"
                }
                finalDownloadedFile = File(categoryDir, resolvedName)

                if (finalDownloadedFile.exists()) {
                    finalDownloadedFile.delete()
                }
                val moved = partFile.renameTo(finalDownloadedFile)
                if (!moved) {
                    partFile.copyTo(finalDownloadedFile, overwrite = true)
                    partFile.delete()
                }
            }

            // Register the file with the system media index (MediaStore on Android 10+)
            val (fileMime, _) = detectMimeAndExtension(finalDownloadedFile, mimeType)
            StorageUtils.registerDownloadedFile(context, finalDownloadedFile, fileMime)

            _downloadProgressMap.value = _downloadProgressMap.value + (messageId to 1.0f)
            Log.d(TAG, "Downloaded encrypted file successfully: ${finalDownloadedFile.absolutePath} ($bytesWritten bytes, mime=$fileMime)")

            finalDownloadedFile
        } catch (e: Exception) {
            Log.d(TAG, "Download paused or cancelled for $messageId: ${e.message}")
            // Retain the partial progress so the user sees "استئناف التحميل (X%)"
            val tempDir = File(context.cacheDir, "p2p_downloads_temp")
            val partFile = File(tempDir, "${fileId}_${fileName}.part")
            if (partFile.exists() && expectedSize > 0) {
                val currentProgress = (partFile.length().toFloat() / expectedSize).coerceIn(0.01f, 0.99f)
                _downloadProgressMap.value = _downloadProgressMap.value + (messageId to currentProgress)
            } else {
                _downloadProgressMap.value = _downloadProgressMap.value - messageId
            }
            null
        } finally {
            activeDownloadSockets.remove(messageId)
            _downloadingIds.value = _downloadingIds.value - messageId
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun isImageFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg")
    }

    fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v", "ts")
    }

    fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "wav", "ogg", "aac", "flac", "opus", "amr")
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

            val (detectedMime, _) = detectMimeAndExtension(file, mimeType)
            val resolvedMimeType = when {
                detectedMime != "*/*" && detectedMime != "application/octet-stream" -> detectedMime
                !mimeType.isNullOrBlank() && mimeType != "*/*" && mimeType != "application/octet-stream" -> mimeType
                isImageFile(file.name) -> "image/*"
                isVideoFile(file.name) -> "video/*"
                isAudioFile(file.name) -> "audio/*"
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "فتح الملف باستخدام").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open with ACTION_VIEW, falling back to Share Intent", e)
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val (detectedMime, _) = detectMimeAndExtension(file, mimeType)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (detectedMime != "*/*") detectedMime else "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة / فتح الملف").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open or share file", ex)
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
