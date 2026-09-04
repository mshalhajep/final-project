package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.Peer
import com.example.model.RoomInfo
import com.example.model.TypingPeer
import com.example.network.NetworkUtils
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ChatListItem
import com.example.ui.components.ChatTypingIndicator
import com.example.ui.components.DateHeaderChip
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.TypingDotsAnimation
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.components.buildChatListItems
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
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
    playbackSpeed: Float = 1f,
    onCyclePlaybackSpeed: () -> Unit = {},
    onUserTyping: (Boolean) -> Unit = {},
    onStartVoiceRecording: () -> Unit = {},
    onStopAndSendVoiceNote: () -> Unit = {},
    onCancelVoiceRecording: () -> Unit = {},
    onTogglePlayVoiceNote: (ChatMessage) -> Unit = {},
    onSeekVoiceNote: (Float) -> Unit = {},
    onSendMessage: (text: String, bitmap: Bitmap?) -> Unit,
    onSendFile: (Uri, String) -> Unit = { _, _ -> },
    onDownloadFile: (ChatMessage) -> Unit = {},
    onCancelDownloadFile: (ChatMessage) -> Unit = {},
    onOpenFile: (ChatMessage) -> Unit = {},
    onCallPeer: (Peer, Boolean) -> Unit,
    onEditMessage: (String, String) -> Unit = { _, _ -> },
    onDeleteMessage: (String) -> Unit = {},
    onForwardMessage: (ChatMessage, String, Boolean) -> Unit = { _, _, _ -> },
    initialDraft: String = "",
    onDraftChange: (String) -> Unit = {},
    replyingToMessage: ChatMessage? = null,
    onReplyToMessage: (ChatMessage) -> Unit = {},
    onCancelReply: () -> Unit = {},
    voiceDraft: com.example.audio.VoiceNoteRecordResult? = null,
    isVoiceDraftPlaying: Boolean = false,
    onPauseAndReviewVoiceNote: () -> Unit = {},
    onToggleVoiceDraftPlayback: () -> Unit = {},
    onCancelVoiceDraft: () -> Unit = {},
    onSendVoiceDraft: () -> Unit = {},
    peers: List<Peer> = emptyList(),
    rooms: List<RoomInfo> = emptyList(),
    isPeerBlocked: Boolean = false,
    onBlockPeer: (String, String) -> Unit = { _, _ -> },
    onUnblockPeer: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember(currentTarget) { mutableStateOf(initialDraft) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingImage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var showPeerMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Stop the live typing indicator the moment this chat screen leaves composition
    DisposableEffect(Unit) {
        onDispose { onUserTyping(false) }
    }

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
            // Sub-sampled decode (max 1280px) — raw 4K decodes allocate 50MB+ and OOM
            scope.launch(Dispatchers.IO) {
                val bmp = com.example.utils.StorageUtils.decodeSampledBitmap(context, uri)
                withContext(Dispatchers.Main) {
                    selectedImageBitmap = bmp
                    selectedFileUri = null
                    selectedFileName = null
                    selectedFileSize = 0L
                }
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

    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= messages.size - 3) {
                listState.animateScrollToItem(messages.size - 1)
            }
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
            // Sleek, Compact Chat Header Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDirectChat && currentPeer != null) Color(currentPeer.avatarColor) else PrimaryCyan
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDirectChat && currentPeer != null && !currentPeer.avatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentPeer.avatarUri,
                                        contentDescription = currentPeer.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isDirectChat) Icons.Default.Person else Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (isDirectChat && currentPeer != null) {
                                UserStatusDot(
                                    status = currentPeer.userStatus,
                                    size = 9.dp,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!isDirectChat && !currentRoomInfo?.passwordHash.isNullOrBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "محمية",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = if (isDirectChat && currentPeer != null) currentPeer.name else (currentRoomInfo?.name ?: "المحادثة العامة"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
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
                                        text = if (isDirectChat) "يكتب..." else "${activeTypingPeers.first().peerName} يكتب...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    TypingDotsAnimation(
                                        dotSize = 3.dp,
                                        dotColor = PrimaryPurple,
                                        spacing = 1.5.dp
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isDirectChat && currentPeer != null && currentPeer.statusMessage.isNotBlank())
                                        currentPeer.statusMessage
                                    else if (isDirectChat)
                                        "محادثة مباشرة مشفرة"
                                    else
                                        "محادثة الغرفة الجماعية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSearchOpen = !isSearchOpen
                                if (!isSearchOpen) searchQuery = ""
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "بحث في الرسائل",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (isDirectChat && currentPeer != null) {
                            IconButton(
                                onClick = { onCallPeer(currentPeer, false) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Audio call",
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onCallPeer(currentPeer, true) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video call",
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showPeerMenu = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "خيارات المستخدم",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPeerMenu,
                                    onDismissRequest = { showPeerMenu = false }
                                ) {
                                    if (isPeerBlocked) {
                                        DropdownMenuItem(
                                            text = { Text("إلغاء حظر المستخدم") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen)
                                            },
                                            onClick = {
                                                showPeerMenu = false
                                                onUnblockPeer(currentPeer.id)
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("حظر هذا المستخدم", color = AccentRose) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Block, contentDescription = null, tint = AccentRose)
                                            },
                                            onClick = {
                                                showPeerMenu = false
                                                onBlockPeer(currentPeer.id, currentPeer.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // In-Chat Search Bar (U-04)
            AnimatedVisibility(visible = isSearchOpen) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في الرسائل...", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        if (searchQuery.isNotBlank()) {
                            val matchCount = messages.count { msg ->
                                msg.content.contains(searchQuery, ignoreCase = true) ||
                                (msg.fileName?.contains(searchQuery, ignoreCase = true) == true) ||
                                msg.senderName.contains(searchQuery, ignoreCase = true)
                            }
                            Surface(
                                color = PrimaryPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "$matchCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
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
            val displayedMessages = remember(messages, searchQuery) {
                if (searchQuery.isBlank()) {
                    messages
                } else {
                    messages.filter { msg ->
                        msg.content.contains(searchQuery, ignoreCase = true) ||
                        (msg.fileName?.contains(searchQuery, ignoreCase = true) == true) ||
                        msg.senderName.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            if (displayedMessages.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة" else "لا توجد رسائل بعد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "جرب البحث بكلمات أخرى" else "ابدأ المحادثة الآن، يتم إرسال الرسائل والصور والملفات محلياً فوراً عبر Wi-Fi دون الحاجة للإنترنت!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // WhatsApp-style day dividers ("اليوم"/"أمس"/date) + Telegram-style
                // scroll-to-bottom FAB with a missed-message badge.
                val chatListItems = remember(displayedMessages) { buildChatListItems(displayedMessages) }
                val isNearBottom by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val lastVisible = info.visibleItemsInfo.lastOrNull()
                        lastVisible == null || info.totalItemsCount == 0 ||
                                lastVisible.index >= info.totalItemsCount - 2
                    }
                }
                var missedCount by remember { mutableIntStateOf(0) }
                var lastSeenCount by remember { mutableIntStateOf(0) }
                LaunchedEffect(chatListItems.size) {
                    if (chatListItems.size > lastSeenCount) {
                        if (!isNearBottom) missedCount += chatListItems.size - lastSeenCount
                        if (isNearBottom) {
                            listState.animateScrollToItem(chatListItems.size - 1)
                        }
                    }
                    lastSeenCount = chatListItems.size
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatListItems, key = { it.key }) { item ->
                        when (item) {
                            is ChatListItem.DateHeader -> DateHeaderChip(item.text)
                            is ChatListItem.Message -> ChatMessageBubble(
                                message = item.message,
                                downloadProgress = downloadProgressMap[item.message.id] ?: 0f,
                                isDownloading = downloadingIds.contains(item.message.id),
                                onDownloadClick = onDownloadFile,
                                onOpenFileClick = onOpenFile,
                                isPlayingVoiceNote = isPlayingVoiceNote,
                                playingVoiceMessageId = playingVoiceMessageId,
                                voiceNoteProgress = voiceNoteProgress,
                                voiceNoteCurrentMs = voiceNoteCurrentMs,
                                playbackSpeed = playbackSpeed,
                                onTogglePlayVoiceNote = onTogglePlayVoiceNote,
                                onSeekVoiceNote = onSeekVoiceNote,
                                onCyclePlaybackSpeed = onCyclePlaybackSpeed,
                                onOpenImage = { msg -> viewingImage = msg },
                                onReplyQuoteClick = { quoteId ->
                                    val targetIdx = chatListItems.indexOfFirst {
                                        it is ChatListItem.Message && it.message.id == quoteId
                                    }
                                    if (targetIdx >= 0) {
                                        scope.launch {
                                            listState.animateScrollToItem(targetIdx)
                                        }
                                    }
                                },
                                onReplyClick = { msg -> onReplyToMessage(msg) },
                                onEditClick = { msg -> editingMessage = msg },
                                onDeleteClick = { msg -> onDeleteMessage(msg.id) },
                                onForwardClick = { msg -> forwardingMessage = msg },
                                onCancelDownloadClick = onCancelDownloadFile
                            )
                        }
                    }
                }

                if (!isNearBottom) {
                    val scrollScope = rememberCoroutineScope()
                    FloatingActionButton(
                        onClick = {
                            scrollScope.launch {
                                if (chatListItems.isNotEmpty()) {
                                    listState.animateScrollToItem(chatListItems.size - 1)
                                }
                            }
                            missedCount = 0
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("scroll_to_bottom_fab")
                    ) {
                        BadgedBox(
                            badge = {
                                if (missedCount > 0) {
                                    Badge(containerColor = com.example.ui.theme.StatusGreen) {
                                        Text("$missedCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "الانتقال لآخر الرسائل"
                            )
                        }
                    }
                }
            }

            // Fullscreen in-app image viewer with pinch zoom
            viewingImage?.let { imageMessage ->
                ImageViewerDialog(
                    message = imageMessage,
                    onDismiss = { viewingImage = null },
                    onDownloadClick = {
                        viewingImage = null
                        onDownloadFile(imageMessage)
                    },
                    onOpenFile = { onOpenFile(imageMessage) }
                )
            }

            if (forwardingMessage != null) {
                val msgToForward = forwardingMessage!!
                AlertDialog(
                    onDismissRequest = { forwardingMessage = null },
                    title = { Text("تحويل الرسالة إلى") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            Text(
                                text = "اختر الغرفة أو الجهاز المراد التحويل إليه:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onForwardMessage(msgToForward, "general", false)
                                                forwardingMessage = null
                                                Toast.makeText(context, "تم تحويل الرسالة للغرفة العامة", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Forum, contentDescription = null, tint = PrimaryPurple)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("الغرفة العامة (General)", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                items(rooms.filter { it.id != "general" }) { room ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onForwardMessage(msgToForward, room.id, false)
                                                forwardingMessage = null
                                                Toast.makeText(context, "تم تحويل الرسالة إلى ${room.name}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Forum, contentDescription = null, tint = PrimaryPurple)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(room.name, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                items(peers) { peer ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onForwardMessage(msgToForward, peer.id, true)
                                                forwardingMessage = null
                                                Toast.makeText(context, "تم تحويل الرسالة إلى ${peer.name}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(peer.avatarColor)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = peer.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(peer.name, fontWeight = FontWeight.SemiBold)
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

            if (editingMessage != null) {
                var editContent by remember(editingMessage) { mutableStateOf(editingMessage?.content ?: "") }
                AlertDialog(
                    onDismissRequest = { editingMessage = null },
                    title = { Text("تعديل الرسالة") },
                    text = {
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("الرسالة الجديدة") }
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (editContent.isNotBlank() && editingMessage != null) {
                                    onEditMessage(editingMessage!!.id, editContent)
                                }
                                editingMessage = null
                            }
                        ) {
                            Text("حفظ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingMessage = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Real-time Chat Typing Indicator Floating Pill
            ChatTypingIndicator(
                typingPeers = activeTypingPeers,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp)
            )
        }

        // Quoted Reply Preview Bar (U-05)
        if (replyingToMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f)),
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PrimaryPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "الرد على ${replyingToMessage.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }
                            Text(
                                text = replyingToMessage.content.ifBlank { replyingToMessage.fileName ?: "رسالة" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = AccentRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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
        if (!isVoiceRecording && voiceDraft == null) {
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
                            .clickable {
                                inputText += emoji
                                onDraftChange(inputText)
                            }
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

        // Input Field & Action Buttons OR Blocked Notice OR Voice Recording Bar OR Voice Draft Review
        if (isDirectChat && isPeerBlocked && currentPeer != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AccentRose.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = AccentRose,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لقد قمت بحظر هذا المستخدم. لن تتلقى منه رسائل أو اتصالات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { onUnblockPeer(currentPeer.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("إلغاء الحظر", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        } else if (isVoiceRecording) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AccentRose.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentRose.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
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

                    // Actions: Cancel, Review, & Send
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
                            onClick = onPauseAndReviewVoiceNote,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("review_chat_voicenote_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "معاينة التسجيل الصوتي",
                                tint = PrimaryPurple,
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
        } else if (voiceDraft != null) {
            // Voice Note Preview Bar (U-07)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PrimaryPurple.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onToggleVoiceDraftPlayback,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple)
                    ) {
                        Icon(
                            imageVector = if (isVoiceDraftPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "معاينة التسجيل الصوتي",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "معاينة التسجيل الصوتي",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold
                        )
                        val minutes = voiceDraft.durationSeconds / 60
                        val seconds = voiceDraft.durationSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onCancelVoiceDraft,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف التسجيل",
                                tint = AccentRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onSendVoiceDraft,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال التسجيل الصوتي",
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
                    .padding(vertical = 4.dp)
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
                            onDraftChange(it)
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
                                    onDraftChange("")
                                    onCancelReply()
                                } else if (inputText.isNotBlank() || selectedImageBitmap != null) {
                                    onSendMessage(inputText.trim(), selectedImageBitmap)
                                    inputText = ""
                                    selectedImageBitmap = null
                                    onDraftChange("")
                                    onCancelReply()
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
}
