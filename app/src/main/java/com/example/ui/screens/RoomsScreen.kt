package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.Peer
import com.example.model.PeerVideoFrame
import com.example.model.RoomInfo
import com.example.model.TypingPeer
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ChatTypingIndicator
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleDark
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomsScreen(
    rooms: List<RoomInfo>,
    currentRoomId: String,
    peers: List<Peer>,
    userProfile: UserProfile,
    inRoomMessages: List<ChatMessage>,
    downloadProgressMap: Map<String, Float> = emptyMap(),
    downloadingIds: Set<String> = emptySet(),
    isGroupCallActive: Boolean = false,
    micLevel: Float = 0f,
    isMuted: Boolean = false,
    isSpeakerOn: Boolean = false,
    isOpenMic: Boolean = false,
    isPushToTalkActive: Boolean = false,
    isVideoStreaming: Boolean = false,
    remoteVideoFrames: Map<String, PeerVideoFrame> = emptyMap(),
    videoEngine: VideoEngine? = null,
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
    onStartGroupVideoCall: (roomId: String, roomName: String) -> Unit = { _, _ -> },
    onToggleGroupVoiceCall: () -> Unit = {},
    onToggleOpenMic: () -> Unit = {},
    onPushToTalkChange: (Boolean) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onToggleCameraStream: () -> Unit = {},
    onToggleCameraLens: () -> Unit = {},
    onSendInRoomMessage: (String, Bitmap?) -> Unit,
    onSendInRoomFile: (Uri, String) -> Unit = { _, _ -> },
    onDownloadFile: (ChatMessage) -> Unit = {},
    onOpenFile: (ChatMessage) -> Unit = {},
    onDirectCallPeer: (Peer, Boolean) -> Unit = { _, _ -> },
    onClearRoomJoinError: () -> Unit,
    onEditMessage: (String, String) -> Unit = { _, _ -> },
    onDeleteMessage: (String) -> Unit = {},
    onForwardMessage: (ChatMessage, String, Boolean) -> Unit = { _, _, _ -> }
) {
    val currentRoomInfo = rooms.find { it.id == currentRoomId } ?: rooms.firstOrNull()
    val roomPeers = peers.filter { it.currentRoom == currentRoomId }
    val totalInRoom = roomPeers.size + 1 // +1 for self
    val maxCapacity = currentRoomInfo?.maxCapacity ?: 10

    var showMembersSheet by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf(0L) }

    // Active typing peers in current room
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
    val isImeVisible = WindowInsets.isImeVisible

    // Auto-scroll chat to bottom on new messages or keyboard open
    LaunchedEffect(inRoomMessages.size, isImeVisible) {
        if (inRoomMessages.isNotEmpty()) {
            listState.animateScrollToItem(inRoomMessages.size - 1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Room Error Banner
        AnimatedVisibility(visible = roomJoinError != null) {
            if (roomJoinError != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
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
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = AccentRose)
                        }
                    }
                }
            }
        }

        // Available Rooms Header & Carousel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "غرف المراسلة والمشاركة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryPurple.copy(alpha = 0.12f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onCreateRoomClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("add_room_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add room",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "إنشاء غرفة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Room Chips Carousel
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
                            imageVector = if (isSelected) Icons.Default.Forum else Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("room_chip_${room.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Room Banner Header (Visual Card with Members Toggle)
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
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
                            text = currentRoomInfo?.description ?: "مراسلات ومشاركة ملفات فائقة السرعة داخل الغرفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action buttons: Member Counter Toggle + Invite Peers button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMembersSheet = !showMembersSheet }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showMembersSheet) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Members",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (currentRoomInfo != null) {
                            // Group Video Call Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusGreen,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onStartGroupVideoCall(currentRoomInfo.id, currentRoomInfo.name) }
                                    .testTag("group_video_call_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "اتصال فيديو جماعي",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "فيديو جماعي",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

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

                // Collapsible Room Members List Panel
                AnimatedVisibility(visible = showMembersSheet) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "الأعضاء المتواجدون في الغرفة ($totalInRoom)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Self Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(userProfile.avatarColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!userProfile.avatarUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = userProfile.avatarUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = userProfile.username.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${userProfile.displayName.ifBlank { userProfile.username }} (أنت)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "@${userProfile.username}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StatusGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "متصل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Other Room Peers
                                if (roomPeers.isEmpty()) {
                                    Text(
                                        text = "لا يوجد أعضاء آخرون في الغرفة. اضغط على زر (+) بالأعلى لدعوة أجهزة متصلة!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                } else {
                                    roomPeers.forEach { peer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(peer.avatarColor)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!peer.avatarUri.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = peer.avatarUri,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = peer.name.take(1).uppercase(),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = peer.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = peer.ip,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Quick direct call shortcuts
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { onDirectCallPeer(peer, false) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = "Call",
                                                        tint = StatusGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDirectCallPeer(peer, true) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Videocam,
                                                        contentDescription = "Video Call",
                                                        tint = PrimaryPurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- IN-ROOM TEXT & FILE EXCHANGE CHAT STREAM ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryPurple.copy(alpha = 0.12f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = "غرفة (${currentRoomInfo?.name ?: ""}) جاهزة للمراسلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تبادل الرسائل والصور والملفات بجميع أنواعها والتسجيلات الصوتية بأقصى سرعة Wi-Fi محلية بدون إنترنت!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(inRoomMessages, key = { it.id }) { message ->
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
                                onSeekVoiceNote = onSeekVoiceNote,
                                onEditClick = { msg ->
                                    editingMessage = msg
                                    messageText = msg.content
                                },
                                onDeleteClick = { msg ->
                                    onDeleteMessage(msg.id)
                                },
                                onForwardClick = { msg ->
                                    forwardingMessage = msg
                                }
                            )
                        }
                    }
                }

                // In-Room Typing Indicator Bar
                if (activeRoomTypingPeers.isNotEmpty()) {
                    ChatTypingIndicator(
                        typingPeers = activeRoomTypingPeers,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Edit Message Banner if editing
                if (editingMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPurple.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تعديل الرسالة: ${editingMessage?.content?.take(30)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    messageText = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = PrimaryPurple)
                            }
                        }
                    }
                }

                // File Attachment Preview Banner if selected
                if (selectedFileUri != null && selectedFileName != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryPurple.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFileName ?: "ملف",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

                            // Amplitude visualizer
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

                            // Actions: Cancel & Send Voice Note
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
                            .padding(top = 4.dp, bottom = 4.dp),
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

                        if (editingMessage != null) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        onEditMessage(editingMessage!!.id, messageText.trim())
                                        editingMessage = null
                                        messageText = ""
                                        onUserTyping(false)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (messageText.isBlank() && selectedImageBitmap == null && selectedFileUri == null) {
                            // Voice Note Record Button
                            IconButton(
                                onClick = onStartVoiceRecording,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple)
                                    .testTag("inroom_record_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record voice note",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
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
    }
}

    // Forward Message Dialog
    if (forwardingMessage != null) {
        val msgToForward = forwardingMessage!!
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("تحويل الرسالة إلى...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "اختر الغرفة أو الجهاز المراد تحويل الرسالة إليه:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Rooms list for forwarding
                    if (rooms.isNotEmpty()) {
                        Text(
                            text = "الغرف:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        rooms.forEach { r ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onForwardMessage(msgToForward, r.id, false)
                                        forwardingMessage = null
                                        Toast.makeText(context, "تم تحويل الرسالة إلى غرفة ${r.name}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = PrimaryPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = r.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Direct peers for forwarding
                    if (peers.isNotEmpty()) {
                        Text(
                            text = "الأجهزة المتصلة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                        peers.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onForwardMessage(msgToForward, p.id, true)
                                        forwardingMessage = null
                                        Toast.makeText(context, "تم تحويل الرسالة إلى ${p.name}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = StatusGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = p.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { forwardingMessage = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
