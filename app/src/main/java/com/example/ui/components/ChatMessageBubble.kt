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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import com.example.ui.theme.AccentRose
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.collection.LruCache
import com.example.model.ChatMessage
import com.example.model.DELIVERY_DELIVERED
import com.example.model.DELIVERY_READ
import com.example.model.MessageType
import com.example.network.NetworkUtils
import com.example.ui.theme.PrimaryPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Shared in-memory bitmap cache for chat images. Prevents repeated Base64 decoding
 * while scrolling through long message lists (decoding previously ran on the UI
 * thread once per bubble instance and was discarded on every scroll pass).
 */
object ChatImageCache {
    private const val MAX_POOL_BYTES = 32 * 1024 * 1024 // 32 MB

    private val pool = object : LruCache<String, Bitmap>(MAX_POOL_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = pool.get(key)
    fun put(key: String, value: Bitmap) {
        pool.put(key, value)
    }

    /** Decodes bytes with a sub-sampling factor so huge photos stay memory-safe. */
    fun decodeScaled(bytes: ByteArray, maxDim: Int = 1024): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

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
    playbackSpeed: Float = 1f,
    onTogglePlayVoiceNote: (ChatMessage) -> Unit = {},
    onSeekVoiceNote: (Float) -> Unit = {},
    onCyclePlaybackSpeed: () -> Unit = {},
    onOpenImage: (ChatMessage) -> Unit = {},
    onReplyQuoteClick: ((String) -> Unit)? = null,
    onReplyClick: ((ChatMessage) -> Unit)? = null,
    onEditClick: ((ChatMessage) -> Unit)? = null,
    onDeleteClick: ((ChatMessage) -> Unit)? = null,
    onForwardClick: ((ChatMessage) -> Unit)? = null,
    onCancelDownloadClick: (ChatMessage) -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val isMine = message.isMine
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    var showMenu by remember { mutableStateOf(false) }

    var decodedBitmap by remember(message.imageBase64, message.localFilePath) {
        mutableStateOf<Bitmap?>(null)
    }

    // Cached, sub-sampled, off-main-thread decode (single decode per unique source)
    val imageCacheKey = when {
        message.imageBase64 != null -> "b64_${message.imageBase64.hashCode()}"
        message.localFilePath != null -> "file_${message.localFilePath}"
        else -> null
    }

    LaunchedEffect(imageCacheKey) {
        if (imageCacheKey == null) return@LaunchedEffect
        val cached = ChatImageCache.get(imageCacheKey)
        if (cached != null && !cached.isRecycled) {
            decodedBitmap = cached
            return@LaunchedEffect
        }
        val decoded = withContext(Dispatchers.Default) {
            try {
                when {
                    message.imageBase64 != null ->
                        ChatImageCache.decodeScaled(Base64.decode(message.imageBase64, Base64.NO_WRAP))
                    message.localFilePath != null &&
                            (message.mimeType?.startsWith("image/") == true || message.messageType == MessageType.IMAGE ||
                                    File(message.localFilePath).name.substringAfterLast('.', "").lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")) -> {
                        val f = File(message.localFilePath)
                        if (f.exists()) {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(f.absolutePath, options)
                            var sample = 1
                            while (options.outWidth / (sample * 2) >= 1024 || options.outHeight / (sample * 2) >= 1024) {
                                sample *= 2
                            }
                            options.inSampleSize = sample
                            options.inJustDecodeBounds = false
                            BitmapFactory.decodeFile(f.absolutePath, options)
                        } else null
                    }
                    else -> null
                }
            } catch (e: Throwable) {
                null
            }
        }
        if (decoded != null) {
            ChatImageCache.put(imageCacheKey, decoded)
            decodedBitmap = decoded
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

        // Bubble Container with Context Menu Box
        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                ),
                color = if (isMine) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onTap = {
                                showMenu = true
                            },
                            onLongPress = {
                                // Subtle tactile confirmation when opening the context menu
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        )
                    }
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

                // Quoted Reply Card (U-05)
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMine) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .then(
                                if (!message.replyToId.isNullOrBlank() && onReplyQuoteClick != null) {
                                    Modifier.clickable { onReplyQuoteClick(message.replyToId!!) }
                                } else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isMine) Color.White else PrimaryPurple)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.replyToSender ?: "رسالة",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMine) Color.White else PrimaryPurple,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = message.replyToText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // If missed call message (U-09)
                if (message.messageType == MessageType.MISSED_CALL) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneMissed,
                                contentDescription = "مكالمة فائتة",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مكالمة فائتة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (message.content.isNotBlank()) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // If image preview is available — tap opens the in-app zoom viewer
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap!!.asImageBitmap(),
                        contentDescription = "Chat Image Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (message.imageBase64 != null ||
                                    (message.localFilePath != null && File(message.localFilePath).exists())
                                ) {
                                    onOpenImage(message)
                                } else if (message.localFilePath != null) {
                                    onOpenFileClick(message)
                                }
                            },
                        contentScale = ContentScale.Fit
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
                        playbackSpeed = playbackSpeed,
                        onTogglePlay = { onTogglePlayVoiceNote(message) },
                        onSeek = onSeekVoiceNote,
                        onCycleSpeed = onCyclePlaybackSpeed,
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
                        onOpenFileClick = { onOpenFileClick(message) },
                        onCancelDownloadClick = { onCancelDownloadClick(message) },
                        onOpenImage = { onOpenImage(message) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text Content (if not standard default voice note content or if text message)
                if (message.content.isNotBlank() && 
                    message.content != message.fileName && 
                    message.messageType != MessageType.MISSED_CALL &&
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "(معَدلة)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isMine) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // Delivery ticks: single = sent, double gray = delivered, double blue = read
                        when {
                            message.deliveryStatus >= DELIVERY_READ -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "تمت القراءة",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            message.deliveryStatus == DELIVERY_DELIVERED -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "تم التسليم",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "تم الإرسال",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Context Menu Dropdown
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 0. Reply (U-05)
            if (onReplyClick != null) {
                DropdownMenuItem(
                    text = { Text("رد") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = PrimaryPurple)
                    },
                    onClick = {
                        showMenu = false
                        onReplyClick(message)
                    }
                )
            }
            // 1. Copy
            if (message.content.isNotBlank() && message.messageType == MessageType.TEXT) {
                DropdownMenuItem(
                    text = { Text("نسخ النص") },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryPurple)
                    },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        showMenu = false
                        Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. Forward
            if (onForwardClick != null) {
                DropdownMenuItem(
                    text = { Text("تحويل الرسالة") },
                    leadingIcon = {
                        Icon(Icons.Default.Forward, contentDescription = null, tint = PrimaryPurple)
                    },
                    onClick = {
                        showMenu = false
                        onForwardClick(message)
                    }
                )
            }

            // 3. Edit (if own text message)
            if (isMine && message.messageType == MessageType.TEXT && onEditClick != null) {
                DropdownMenuItem(
                    text = { Text("تعديل الرسالة") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryPurple)
                    },
                    onClick = {
                        showMenu = false
                        onEditClick(message)
                    }
                )
            }

            // 4. Delete
            if (onDeleteClick != null) {
                DropdownMenuItem(
                    text = { Text("حذف الرسالة", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        showMenu = false
                        onDeleteClick(message)
                        Toast.makeText(context, "تم حذف الرسالة", Toast.LENGTH_SHORT).show()
                    }
                )
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
    playbackSpeed: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val durationSeconds = message.durationSeconds ?: 0
    var isDownloaded by remember(message.isDownloaded, message.isMine, message.localFilePath) {
        mutableStateOf(message.isDownloaded || message.isMine)
    }
    LaunchedEffect(message.localFilePath) {
        if (!isDownloaded && message.localFilePath != null) {
            withContext(Dispatchers.IO) {
                if (File(message.localFilePath).exists()) {
                    isDownloaded = true
                }
            }
        }
    }

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

                // Speed + Duration badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Playback speed pill: 1.0x -> 1.5x -> 2.0x
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMine) Color.White.copy(alpha = 0.22f) else PrimaryPurple.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onCycleSpeed)
                            .testTag("voice_speed_button")
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", playbackSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) Color.White else PrimaryPurple,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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
    onOpenFileClick: () -> Unit,
    onCancelDownloadClick: () -> Unit = {},
    onOpenImage: () -> Unit = {}
) {
    val fileName = message.fileName ?: "ملف مرفق"
    val fileSizeFormatted = NetworkUtils.formatFileSize(message.fileSize)
    val mimeType = message.mimeType ?: ""
    val isDownloaded = message.isDownloaded || message.isMine || message.localFilePath != null

    val ext = fileName.substringAfterLast('.', "").lowercase()
    val isImg = message.messageType == MessageType.IMAGE ||
            mimeType.startsWith("image/") ||
            ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isVid = mimeType.startsWith("video/") ||
            ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جاري التنزيل...",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = AccentRose.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onCancelDownloadClick)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إيقاف مؤقت / إلغاء",
                                        tint = AccentRose,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
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
                    onClick = {
                        if (isImg) onOpenImage() else onOpenFileClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isImg -> Icons.Default.Image
                            isVid -> Icons.Default.Videocam
                            else -> Icons.Default.OpenInNew
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isImg -> "عرض الصورة"
                            isVid -> "تشغيل الفيديو"
                            isMine -> "عرض / فتح الملف"
                            else -> "فتح الملف المحفوظ"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val isResume = downloadProgress > 0.01f && downloadProgress < 0.99f
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
                        imageVector = if (isResume) Icons.Default.PlayArrow else Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isResume) "استئناف التحميل (${(downloadProgress * 100).toInt()}%)" else "تحميل عبر Wi-Fi ($fileSizeFormatted)",
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

