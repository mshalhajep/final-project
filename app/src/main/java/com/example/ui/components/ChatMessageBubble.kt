package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.network.NetworkUtils
import com.example.ui.theme.PrimaryPurple
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    downloadProgress: Float = 0f,
    isDownloading: Boolean = false,
    onDownloadClick: (ChatMessage) -> Unit = {},
    onOpenFileClick: (ChatMessage) -> Unit = {},
    isPlayingVoiceNote: Boolean = false,
    playingVoiceMessageId: String? = null,
    voiceNoteProgress: Float = 0f,
    voiceNoteCurrentMs: Int = 0,
    onTogglePlayVoiceNote: (ChatMessage) -> Unit = {},
    onSeekVoiceNote: (Float) -> Unit = {}
) {
    val isMine = message.isMine
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    var decodedBitmap by remember(message.imageBase64, message.localFilePath) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(message.imageBase64, message.localFilePath) {
        if (message.imageBase64 != null) {
            try {
                val bytes = Base64.decode(message.imageBase64, Base64.NO_WRAP)
                decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                // Ignore
            }
        } else if (message.localFilePath != null && (message.mimeType?.startsWith("image/") == true || message.messageType == MessageType.IMAGE)) {
            try {
                val f = File(message.localFilePath)
                if (f.exists()) {
                    decodedBitmap = BitmapFactory.decodeFile(f.absolutePath)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar on the left if not mine
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(message.senderColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bubble Container
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(message.senderColor)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // If image preview is available
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap!!.asImageBitmap(),
                        contentDescription = "Chat Image Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (message.localFilePath != null) {
                                    onOpenFileClick(message)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // If voice note message
                if (message.messageType == MessageType.VOICE_NOTE) {
                    VoiceNoteCard(
                        message = message,
                        isMine = isMine,
                        isCurrentlyPlaying = isPlayingVoiceNote && (playingVoiceMessageId == message.id),
                        progress = if (playingVoiceMessageId == message.id) voiceNoteProgress else 0f,
                        currentMs = if (playingVoiceMessageId == message.id) voiceNoteCurrentMs else 0,
                        downloadProgress = downloadProgress,
                        isDownloading = isDownloading,
                        onTogglePlay = { onTogglePlayVoiceNote(message) },
                        onSeek = onSeekVoiceNote,
                        onDownloadClick = { onDownloadClick(message) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // If file attachment
                if (message.messageType == MessageType.FILE || (message.fileId != null && decodedBitmap == null && message.messageType != MessageType.VOICE_NOTE)) {
                    FileAttachmentCard(
                        message = message,
                        isMine = isMine,
                        downloadProgress = downloadProgress,
                        isDownloading = isDownloading,
                        onDownloadClick = { onDownloadClick(message) },
                        onOpenFileClick = { onOpenFileClick(message) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text Content (if not standard default voice note content or if text message)
                if (message.content.isNotBlank() && 
                    message.content != message.fileName && 
                    !(message.messageType == MessageType.VOICE_NOTE && message.content.startsWith("تسجيل صوتي"))) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteCard(
    message: ChatMessage,
    isMine: Boolean,
    isCurrentlyPlaying: Boolean,
    progress: Float,
    currentMs: Int,
    downloadProgress: Float,
    isDownloading: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onDownloadClick: () -> Unit
) {
    val durationSeconds = message.durationSeconds ?: 0
    val isDownloaded = message.isDownloaded || message.isMine || (message.localFilePath != null && File(message.localFilePath).exists())

    // Generate waveform bar heights deterministically based on message ID hash
    val barCount = 22
    val heights = remember(message.id) {
        val hash = message.id.hashCode().absoluteValue
        val list = mutableListOf<Float>()
        for (i in 0 until barCount) {
            val pseudoRandom = ((hash + i * 37 + i * i * 13) % 100) / 100f
            list.add(0.25f + pseudoRandom * 0.75f)
        }
        list
    }

    val cardBg = if (isMine) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header: Voice Note label & Mic badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Note",
                        tint = if (isMine) Color.White else PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تسجيل صوتي محلي",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                    )
                }

                // Duration badge
                Text(
                    text = if (isCurrentlyPlaying) {
                        val currentSec = currentMs / 1000
                        val currentFraction = (currentMs % 1000) / 100
                        "$currentSec.$currentFraction / ${durationSeconds}s"
                    } else {
                        "${durationSeconds}s"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player control row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause / Download button
                if (isDownloading) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp),
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (!isDownloaded) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary)
                            .testTag("download_voicenote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download voice note",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary)
                            .testTag("play_voicenote_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Waveform Visualizer Bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek(fraction)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    heights.forEachIndexed { index, hFraction ->
                        val barProgress = (index.toFloat() / (barCount - 1)).coerceIn(0f, 1f)
                        val isFilled = barProgress <= progress

                        val activeColor = if (isMine) Color.White else PrimaryPurple
                        val inactiveColor = if (isMine) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(hFraction)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isFilled) activeColor else inactiveColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileAttachmentCard(
    message: ChatMessage,
    isMine: Boolean,
    downloadProgress: Float,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onOpenFileClick: () -> Unit
) {
    val fileName = message.fileName ?: "ملف مرفق"
    val fileSizeFormatted = NetworkUtils.formatFileSize(message.fileSize)
    val mimeType = message.mimeType ?: ""
    val isDownloaded = message.isDownloaded || message.isMine || message.localFilePath != null

    val fileIcon = getFileIcon(fileName, mimeType)
    val cardBg = if (isMine) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = "File Type",
                        tint = if (isMine) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جاري التنزيل...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                        trackColor = if (isMine) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else if (isDownloaded) {
                FilledTonalButton(
                    onClick = onOpenFileClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMine) "عرض / فتح الملف" else "فتح الملف المحفوظ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تحميل عبر Wi-Fi ($fileSizeFormatted)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getFileIcon(fileName: String, mimeType: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg") -> Icons.Default.Image
        mimeType.startsWith("audio/") || ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac") -> Icons.Default.Audiotrack
        mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "avi", "3gp") -> Icons.Default.Videocam
        mimeType.contains("pdf") || ext == "pdf" -> Icons.Default.PictureAsPdf
        ext in listOf("zip", "rar", "tar", "gz", "7z", "apk") -> Icons.Default.FolderZip
        ext in listOf("doc", "docx", "txt", "rtf", "odt") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

