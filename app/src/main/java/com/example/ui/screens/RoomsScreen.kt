package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.Peer
import com.example.model.PeerVideoFrame
import com.example.model.RoomInfo
import com.example.model.TypingPeer
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ChatTypingIndicator
import com.example.ui.components.RemotePeerVideoView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleDark
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RoomsScreen(
    rooms: List<RoomInfo>,
    currentRoomId: String,
    peers: List<Peer>,
    userProfile: UserProfile,
    inRoomMessages: List<ChatMessage>,
    downloadProgressMap: Map<String, Float> = emptyMap(),
    downloadingIds: Set<String> = emptySet(),
    isGroupCallActive: Boolean,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isOpenMic: Boolean,
    isPushToTalkActive: Boolean,
    isVideoStreaming: Boolean,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    videoEngine: VideoEngine,
    roomJoinError: String?,
    typingPeers: List<TypingPeer> = emptyList(),
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
    onSelectRoom: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
    onInvitePeersClick: (RoomInfo) -> Unit,
    onToggleGroupVoiceCall: () -> Unit,
    onToggleOpenMic: () -> Unit,
    onPushToTalkChange: (Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCameraStream: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onSendInRoomMessage: (String, Bitmap?) -> Unit,
    onSendInRoomFile: (Uri, String) -> Unit = { _, _ -> },
    onDownloadFile: (ChatMessage) -> Unit = {},
    onOpenFile: (ChatMessage) -> Unit = {},
    onDirectCallPeer: (Peer, Boolean) -> Unit,
    onClearRoomJoinError: () -> Unit
) {
    val currentRoomInfo = rooms.find { it.id == currentRoomId } ?: rooms.firstOrNull()
    val roomPeers = peers.filter { it.currentRoom == currentRoomId }
    val totalInRoom = roomPeers.size + 1 // +1 for self
    val maxCapacity = currentRoomInfo?.maxCapacity ?: 10

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = In-room Chat, 1 = Group Voice & Video
    var messageText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf(0L) }

    // Active typing peers in the current room
    val activeRoomTypingPeers = remember(typingPeers, currentRoomId) {
        typingPeers.filter { !it.isDirect && (it.roomId == currentRoomId || it.roomId == null) }
    }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                selectedImageBitmap = bitmap
                selectedFileUri = null
                selectedFileName = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                // Fallback
            }
            if (selectedFileName == null) {
                selectedFileName = "file_${System.currentTimeMillis()}"
            }
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll chat to bottom when new messages arrive
    LaunchedEffect(inRoomMessages.size) {
        if (inRoomMessages.isNotEmpty()) {
            listState.animateScrollToItem(inRoomMessages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Room Error Snackbar / Alert if capacity reached
        AnimatedVisibility(visible = roomJoinError != null) {
            if (roomJoinError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = roomJoinError,
                            color = AccentRose,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onClearRoomJoinError, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Close", tint = AccentRose)
                        }
                    }
                }
            }
        }

        // Room Selection Horizontal Carousel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الغرف المتاحة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onCreateRoomClick,
                modifier = Modifier.testTag("add_room_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add room",
                    tint = PrimaryPurple
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(rooms) { room ->
                val isSelected = room.id == currentRoomId
                val count = if (room.id == currentRoomId) totalInRoom else peers.count { it.currentRoom == room.id }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRoom(room.id) },
                    label = { Text("${room.name} ($count/${room.maxCapacity})") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RecordVoiceOver else Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("room_chip_${room.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Room Banner Header
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentRoomInfo?.name ?: "غرفة الاتصال",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentRoomInfo?.creatorName != null && currentRoomInfo.creatorName != "النظام") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryPurple.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "المنشئ: ${currentRoomInfo.creatorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = currentRoomInfo?.description ?: "تواصل صوتي ومراسلات فورية داخل الغرفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action: Invite Peers button + Capacity Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreen)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$totalInRoom / $maxCapacity عضو",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }

                        if (currentRoomInfo != null) {
                            IconButton(
                                onClick = { onInvitePeersClick(currentRoomInfo) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.15f))
                                    .testTag("invite_peers_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = "Invite Peers",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-tabs: In-Room Live Chat vs. Group Voice Call
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = PrimaryPurple
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("المحادثة النصية للغرفة", fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                modifier = Modifier.testTag("tab_inroom_chat")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isGroupCallActive) "مكالمة صوتية نشطة ●" else "المكالمة الصوتية الجماعية",
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (isGroupCallActive) StatusGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                modifier = Modifier.testTag("tab_inroom_voice")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content: In-Room Chat OR Group Voice Controls
        if (selectedSubTab == 0) {
            // --- IN-ROOM TEXT CHAT ---
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (inRoomMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "لا توجد رسائل سابقة في هذه الغرفة.\nابدأ بإرسال رسالة وستصل لجميع أعضاء الغرفة مباشرة!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(inRoomMessages) { msg ->
                                ChatMessageBubble(
                                    message = msg,
                                    downloadProgress = downloadProgressMap[msg.id] ?: 0f,
                                    isDownloading = downloadingIds.contains(msg.id),
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

                    // Real-time In-Room Typing Indicator Floating Pill
                    ChatTypingIndicator(
                        typingPeers = activeRoomTypingPeers,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )

                    // Staged File Preview
                    if (selectedFileUri != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
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
                                            text = "${com.example.network.NetworkUtils.formatFileSize(selectedFileSize)} • جاهز للإرسال للغرفة عبر Wi-Fi",
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

                    // Image preview if selected
                    if (selectedImageBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = PrimaryPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تم إرفاق صورة جاهزة للإرسال", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { selectedImageBitmap = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = AccentRose)
                                }
                            }
                        }
                    }

                    // Active Voice Recording Bar OR Input Row
                    if (isVoiceRecording) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = AccentRose.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentRose.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 12.dp)
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
                                            .testTag("inroom_cancel_voicenote_button")
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
                                            .testTag("inroom_send_voicenote_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send Voice Note",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Input Row for in-room messaging
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = {
                                    messageText = it
                                    onUserTyping(it.isNotBlank())
                                },
                                placeholder = { Text("اكتب رسالة للغرفة...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inroom_message_input"),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { filePickerLauncher.launch("*/*") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "Attach local file",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Attach image",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            )

                            // Send or Record Voice Note button
                            if (messageText.isBlank() && selectedImageBitmap == null && selectedFileUri == null) {
                                IconButton(
                                    onClick = {
                                        onUserTyping(false)
                                        onStartVoiceRecording()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple)
                                        .testTag("inroom_record_voicenote_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice Note",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        onUserTyping(false)
                                        if (selectedFileUri != null) {
                                            onSendInRoomFile(selectedFileUri!!, messageText.trim())
                                            selectedFileUri = null
                                            selectedFileName = null
                                            selectedFileSize = 0L
                                            messageText = ""
                                        } else if (selectedImageBitmap != null || messageText.isNotBlank()) {
                                            onSendInRoomMessage(messageText.trim(), selectedImageBitmap)
                                            messageText = ""
                                            selectedImageBitmap = null
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple)
                                        .testTag("inroom_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send message",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- GROUP VOICE & VIDEO CALL PANEL ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Group Voice Call Master Control Card
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isGroupCallActive) PrimaryPurple.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "المكالمة الصوتية الجماعية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isGroupCallActive) "أنت مشارك في البث الصوتي المباشر للغرفة" else "انضم للمكالمة الصوتية للتحدث والاستماع للجميع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = onToggleGroupVoiceCall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isGroupCallActive) AccentRose else StatusGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("group_call_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGroupCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isGroupCallActive) "مغادرة" else "بدء / انضمام")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Audio visualizer bar when active
                            AudioWaveformVisualizer(
                                isSpeaking = (isOpenMic || isPushToTalkActive || isGroupCallActive) && micLevel > 0.05f && !isMuted,
                                audioLevel = micLevel,
                                barCount = 20,
                                maxHeight = 24.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Push To Talk & Voice Controls
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val pttActive = isPushToTalkActive
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .scale(if (pttActive) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (pttActive) Brush.radialGradient(listOf(StatusGreen, PrimaryPurpleDark))
                                        else Brush.radialGradient(listOf(PrimaryPurple, PrimaryPurpleDark))
                                    )
                                    .pointerInteropFilter { motionEvent ->
                                        when (motionEvent.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                onPushToTalkChange(true)
                                                true
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                onPushToTalkChange(false)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    .testTag("push_to_talk_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (pttActive) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                                        contentDescription = "Push to talk",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = if (pttActive) "تحدث الآن" else "اضغط للتحدث",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Continuous mic switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MicNone,
                                        contentDescription = null,
                                        tint = PrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ميكروفون مفتوح مستمر",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Switch(
                                    checked = isOpenMic,
                                    onCheckedChange = { onToggleOpenMic() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Controls Row: Camera, Flip, Mute, Speaker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onToggleCameraStream,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isVideoStreaming) StatusGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isVideoStreaming) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                        contentDescription = "Camera stream",
                                        tint = if (isVideoStreaming) StatusGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isVideoStreaming) {
                                    IconButton(
                                        onClick = onToggleCameraLens,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cameraswitch,
                                            contentDescription = "Switch Camera",
                                            tint = PrimaryPurple
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onToggleMute,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isMuted) AccentRose.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute mic",
                                        tint = if (isMuted) AccentRose else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = onToggleSpeaker,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSpeakerOn) PrimaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerOn) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // In-Room Members List
                item {
                    Text(
                        text = "المتواجدون في الغرفة ($totalInRoom)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Current User Card
                        ParticipantCard(
                            name = "${userProfile.displayName.ifBlank { userProfile.username }} (أنت)",
                            avatarColor = userProfile.avatarColor,
                            isSpeaking = (isOpenMic || isPushToTalkActive || isGroupCallActive) && micLevel > 0.05f && !isMuted,
                            isVideoActive = isVideoStreaming,
                            isMuted = isMuted,
                            deviceModel = "@${userProfile.username}",
                            pulseScale = pulseScale,
                            videoContent = {
                                if (isVideoStreaming) {
                                    CameraPreviewSurface(
                                        videoEngine = videoEngine,
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 85.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }
                            }
                        )

                        // Remote peers in this room
                        if (roomPeers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا يوجد أعضاء آخرون في الغرفة حالياً. يمكنك استخدام زر (دعوة أعضاء) لدعوة الأجهزة المتصلة!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            roomPeers.forEach { peer ->
                                val peerVideo = remoteVideoFrames[peer.id]
                                ParticipantCard(
                                    name = peer.name,
                                    avatarColor = peer.avatarColor,
                                    isSpeaking = peer.isSpeaking,
                                    isVideoActive = peer.isVideoActive || peerVideo?.bitmap != null,
                                    isMuted = peer.isMuted,
                                    deviceModel = peer.ip,
                                    pulseScale = pulseScale,
                                    onAudioCall = { onDirectCallPeer(peer, false) },
                                    onVideoCall = { onDirectCallPeer(peer, true) },
                                    videoContent = {
                                        if (peerVideo?.bitmap != null) {
                                            RemotePeerVideoView(
                                                bitmap = peerVideo.bitmap,
                                                peerName = peer.name,
                                                modifier = Modifier
                                                    .size(width = 110.dp, height = 85.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantCard(
    name: String,
    avatarColor: Long,
    isSpeaking: Boolean,
    isVideoActive: Boolean,
    isMuted: Boolean,
    deviceModel: String,
    pulseScale: Float,
    onAudioCall: (() -> Unit)? = null,
    onVideoCall: (() -> Unit)? = null,
    videoContent: @Composable () -> Unit = {}
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(if (isSpeaking) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(Color(avatarColor))
                    .then(
                        if (isSpeaking) {
                            Modifier.border(2.5.dp, StatusGreen, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isSpeaking) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "يتحدث...",
                            color = StatusGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = deviceModel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Attached video tile if active
            videoContent()

            // 1-to-1 direct call shortcuts for remote peers
            if (onAudioCall != null && onVideoCall != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onAudioCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = StatusGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onVideoCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
