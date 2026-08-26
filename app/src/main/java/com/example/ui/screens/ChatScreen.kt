package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.Peer
import com.example.model.RoomInfo
import com.example.model.TypingPeer
import com.example.network.NetworkUtils
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ChatTypingIndicator
import com.example.ui.components.TypingDotsAnimation
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryTeal

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentTarget: String,
    isDirectChat: Boolean,
    currentPeer: Peer?,
    currentRoomInfo: RoomInfo?,
    typingPeers: List<TypingPeer> = emptyList(),
    downloadProgressMap: Map<String, Float> = emptyMap(),
    downloadingIds: Set<String> = emptySet(),
    isVoiceRecording: Boolean = false,
    voiceRecordingDuration: Int = 0,
    voiceRecordingAmplitudes: List<Float> = emptyList(),
    voiceRecordingCurrentAmp: Float = 0f,
    isPlayingVoiceNote: Boolean = false,
    playingVoiceMessageId: String? = null,
    voiceNoteProgress: Float = 0f,
    voiceNoteCurrentMs: Int = 0,
    onUserTyping: (Boolean) -> Unit = {},
    onStartVoiceRecording: () -> Unit = {},
    onStopAndSendVoiceNote: () -> Unit = {},
    onCancelVoiceRecording: () -> Unit = {},
    onTogglePlayVoiceNote: (ChatMessage) -> Unit = {},
    onSeekVoiceNote: (Float) -> Unit = {},
    onSendMessage: (text: String, bitmap: Bitmap?) -> Unit,
    onSendFile: (Uri, String) -> Unit = { _, _ -> },
    onDownloadFile: (ChatMessage) -> Unit = {},
    onOpenFile: (ChatMessage) -> Unit = {},
    onCallPeer: (Peer, Boolean) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

    // Active typing peers in the current chat target
    val activeTypingPeers = remember(typingPeers, currentTarget, isDirectChat, currentPeer) {
        if (isDirectChat && currentPeer != null) {
            typingPeers.filter { it.isDirect && (it.peerId == currentPeer.id || it.targetPeerId == currentPeer.id) }
        } else {
            typingPeers.filter { !it.isDirect && (it.roomId == currentTarget || currentTarget.isEmpty()) }
        }
    }

    // Camera launcher for taking offline snapshot to share
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImageBitmap = bitmap
            selectedFileUri = null
            selectedFileName = null
        }
    }

    // Gallery image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                selectedImageBitmap = BitmapFactory.decodeStream(stream)
                selectedFileUri = null
                selectedFileName = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedImageBitmap = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) selectedFileName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) selectedFileSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            if (selectedFileName == null) {
                selectedFileName = "file_${System.currentTimeMillis()}"
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Chat Header Banner
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDirectChat && currentPeer != null) Color(currentPeer.avatarColor) else PrimaryCyan
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDirectChat) Icons.Default.Person else Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (isDirectChat && currentPeer != null) {
                            UserStatusDot(
                                status = currentPeer.userStatus,
                                size = 11.dp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isDirectChat && currentPeer != null) currentPeer.name else (currentRoomInfo?.name ?: "المحادثة العامة"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDirectChat && currentPeer != null) {
                                UserStatusBadge(status = currentPeer.userStatus, compact = true)
                            }
                        }
                        if (activeTypingPeers.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isDirectChat) "يكتب الآن..." else "${activeTypingPeers.first().peerName} يكتب الآن...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                TypingDotsAnimation(
                                    dotSize = 3.5.dp,
                                    dotColor = PrimaryPurple,
                                    spacing = 2.dp
                                )
                            }
                        } else {
                            Text(
                                text = if (isDirectChat && currentPeer != null && currentPeer.statusMessage.isNotBlank())
                                    "\"${currentPeer.statusMessage}\""
                                else if (isDirectChat)
                                    "محادثة مشفرة مباشرة بدون إنترنت"
                                else
                                    "محادثة جماعية مفتوحة لأعضاء الغرفة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (isDirectChat && currentPeer != null) {
                    Row {
                        IconButton(onClick = { onCallPeer(currentPeer, false) }) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Audio call",
                                tint = PrimaryCyan
                            )
                        }
                        IconButton(onClick = { onCallPeer(currentPeer, true) }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video call",
                                tint = SecondaryTeal
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد رسائل بعد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ابدأ المحادثة الآن، يتم إرسال الرسائل والصور والملفات محلياً فوراً عبر Wi-Fi دون الحاجة للإنترنت!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(
                            message = message,
                            downloadProgress = downloadProgressMap[message.id] ?: 0f,
                            isDownloading = downloadingIds.contains(message.id),
                            onDownloadClick = onDownloadFile,
                            onOpenFileClick = onOpenFile,
                            isPlayingVoiceNote = isPlayingVoiceNote,
                            playingVoiceMessageId = playingVoiceMessageId,
                            voiceNoteProgress = voiceNoteProgress,
                            voiceNoteCurrentMs = voiceNoteCurrentMs,
                            onTogglePlayVoiceNote = onTogglePlayVoiceNote,
                            onSeekVoiceNote = onSeekVoiceNote
                        )
                    }
                }
            }

            // Real-time Chat Typing Indicator Floating Pill
            ChatTypingIndicator(
                typingPeers = activeTypingPeers,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp)
            )
        }

        // Staged File Preview (if file selected)
        if (selectedFileUri != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedFileName ?: "ملف مرفق",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${NetworkUtils.formatFileSize(selectedFileSize)} • جاهز للإرسال عبر P2P",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            selectedFileUri = null
                            selectedFileName = null
                            selectedFileSize = 0L
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove file",
                            tint = AccentRose
                        )
                    }
                }
            }
        }

        // Selected Image Preview (if taking photo or selecting image)
        if (selectedImageBitmap != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = selectedImageBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "صورة مرفقة جاهزة للإرسال",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(onClick = { selectedImageBitmap = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image"
                        )
                    }
                }
            }
        }

        // Quick Emojis Row (only if not recording)
        if (!isVoiceRecording) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                val quickEmojis = listOf("👋", "👍", "📁", "🎙️", "📶", "🔥", "🤝", "✅", "⚡")
                items(quickEmojis) { emoji ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { inputText += emoji }
                    ) {
                        Text(
                            text = emoji,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Input Field & Action Buttons OR Voice Recording Bar
        if (isVoiceRecording) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AccentRose.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentRose.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 88.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Red blinking record dot + Duration timer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(AccentRose)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val minutes = voiceRecordingDuration / 60
                        val seconds = voiceRecordingDuration % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentRose
                        )
                    }

                    // Amplitude live visualizer
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val recentAmps = voiceRecordingAmplitudes.takeLast(16)
                        if (recentAmps.isEmpty()) {
                            for (i in 0 until 12) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.5.dp)
                                        .width(3.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(AccentRose.copy(alpha = 0.5f))
                                )
                            }
                        } else {
                            recentAmps.forEach { amp ->
                                val barH = (6 + (amp * 22)).dp.coerceIn(4.dp, 28.dp)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.5.dp)
                                        .width(3.dp)
                                        .height(barH)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(AccentRose)
                                )
                            }
                        }
                    }

                    // Actions: Cancel & Send
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onCancelVoiceRecording,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("cancel_chat_voicenote_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Recording",
                                tint = AccentRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onStopAndSendVoiceNote,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                                .testTag("send_chat_voicenote_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Note",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File Attachment Button
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = PrimaryPurple
                        )
                    }

                    // Image Picker Button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Attach Gallery Photo",
                            tint = PrimaryCyan
                        )
                    }

                    // Camera Button
                    IconButton(
                        onClick = { cameraLauncher.launch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = SecondaryTeal
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            onUserTyping(it.isNotBlank())
                        },
                        placeholder = { Text("اكتب رسالة محلياً...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    // Send or Record Voice Note Button
                    if (inputText.isBlank() && selectedImageBitmap == null && selectedFileUri == null) {
                        FloatingActionButton(
                            onClick = {
                                onUserTyping(false)
                                onStartVoiceRecording()
                            },
                            containerColor = PrimaryPurple,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("record_chat_voicenote_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        FloatingActionButton(
                            onClick = {
                                onUserTyping(false)
                                if (selectedFileUri != null) {
                                    onSendFile(selectedFileUri!!, inputText.trim())
                                    selectedFileUri = null
                                    selectedFileName = null
                                    selectedFileSize = 0L
                                    inputText = ""
                                } else if (inputText.isNotBlank() || selectedImageBitmap != null) {
                                    onSendMessage(inputText.trim(), selectedImageBitmap)
                                    inputText = ""
                                    selectedImageBitmap = null
                                }
                            },
                            containerColor = PrimaryPurple,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("send_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
