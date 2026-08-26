package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.components.UserStatusQuickDropdown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PeerSignalInfo
import com.example.model.RoomInfo
import com.example.ui.AppTab
import com.example.ui.AuthState
import com.example.ui.MainViewModel
import com.example.ui.dialogs.AddRoomDialog
import com.example.ui.dialogs.CallScreenDialog
import com.example.ui.dialogs.IncomingRoomInviteDialog
import com.example.ui.dialogs.InvitePeersDialog
import com.example.ui.dialogs.ManualIpDialog
import com.example.ui.dialogs.UserProfileDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NetworkGuideScreen
import com.example.ui.screens.PeersScreen
import com.example.ui.screens.RoomsScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.StatusGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RootApp()
            }
        }
    }
}

@Composable
fun RootApp(viewModel: MainViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsState()
    val authError by viewModel.authError.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        }

        is AuthState.LoggedOut -> {
            AuthScreen(
                authError = authError,
                onLogin = { u, p -> viewModel.login(u, p) },
                onRegister = { u, p, d, c -> viewModel.register(u, p, d, c) },
                onClearError = { viewModel.clearAuthError() }
            )
        }

        is AuthState.LoggedIn -> {
            MainAppScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current

    // Request Audio & Camera Permissions
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    // State Collection
    val selectedTab by viewModel.selectedTab.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val currentRoomId by viewModel.currentRoom.collectAsState()
    val peers by viewModel.discoveredPeers.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val callSignalInfo by viewModel.callSignalInfo.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inRoomMessages by viewModel.inRoomMessages.collectAsState()
    val currentChatTarget by viewModel.currentChatTarget.collectAsState()
    val isDirectChat by viewModel.currentChatIsDirect.collectAsState()
    val currentChatPeer by viewModel.currentChatPeer.collectAsState()
    val typingPeers by viewModel.typingPeers.collectAsState()
    val activeRoomInvitation by viewModel.activeRoomInvitation.collectAsState()
    val roomJoinError by viewModel.roomJoinError.collectAsState()
    val isGroupCallActive by viewModel.isGroupCallActive.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()
    val downloadingIds by viewModel.downloadingIds.collectAsState()

    val isVoiceRecording by viewModel.isVoiceRecording.collectAsState()
    val voiceRecordingDuration by viewModel.voiceRecordingDuration.collectAsState()
    val voiceRecordingAmplitudes by viewModel.voiceRecordingAmplitudes.collectAsState()
    val voiceRecordingCurrentAmp by viewModel.voiceRecordingCurrentAmp.collectAsState()

    val isPlayingVoiceNote by viewModel.isPlayingVoiceNote.collectAsState()
    val playingVoiceMessageId by viewModel.playingVoiceMessageId.collectAsState()
    val voiceNoteProgress by viewModel.voiceNoteProgress.collectAsState()
    val voiceNoteCurrentMs by viewModel.voiceNoteCurrentMs.collectAsState()

    val micLevel by viewModel.micLevel.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isOpenMic by viewModel.isOpenMic.collectAsState()
    val isPushToTalkActive by viewModel.isPushToTalkActive.collectAsState()
    val isVideoStreaming by viewModel.isVideoStreaming.collectAsState()
    val remoteVideoFrames by viewModel.remoteVideoFrames.collectAsState()

    // Scanning states
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scannedIpCount by viewModel.scannedIpCount.collectAsState()
    val currentScanSubnet by viewModel.currentScanSubnet.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()

    // Dialogs state
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var roomToInvite by remember { mutableStateOf<RoomInfo?>(null) }

    val currentRoomInfo = rooms.find { it.id == currentRoomId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LocalConnect • اتصال محلي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserStatusDot(
                                    status = userProfile.userStatus,
                                    size = 7.dp,
                                    showBorder = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "@${userProfile.username} • $localIp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick User Presence Status Dropdown Pill
                    UserStatusQuickDropdown(
                        currentStatus = userProfile.userStatus,
                        onStatusSelected = { viewModel.setUserStatus(it) },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Quick Rescan Network button
                    IconButton(
                        onClick = { viewModel.rescanNetwork() },
                        modifier = Modifier.testTag("top_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Scan Network",
                            tint = if (isScanning) StatusGreen else PrimaryPurple
                        )
                    }

                    // Quick manual IP connect button
                    IconButton(
                        onClick = { showManualIpDialog = true },
                        modifier = Modifier.testTag("top_manual_ip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Manual IP Connect",
                            tint = PrimaryPurple
                        )
                    }

                    // Profile avatar button with presence dot overlay
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("top_profile_button")
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(userProfile.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.username.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            UserStatusDot(
                                status = userProfile.userStatus,
                                size = 10.dp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Tab 1: Rooms
                NavigationBarItem(
                    selected = selectedTab == AppTab.ROOMS,
                    onClick = { viewModel.selectTab(AppTab.ROOMS) },
                    icon = {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Rooms")
                    },
                    label = { Text("غرف الاتصال") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryPurple.copy(alpha = 0.2f),
                        selectedIconColor = PrimaryPurple
                    )
                )

                // Tab 2: Peers
                NavigationBarItem(
                    selected = selectedTab == AppTab.PEERS,
                    onClick = { viewModel.selectTab(AppTab.PEERS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (peers.isNotEmpty()) {
                                    Badge(containerColor = StatusGreen) {
                                        Text(text = "${peers.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = "Peers")
                        }
                    },
                    label = { Text("الأجهزة") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryPurple.copy(alpha = 0.2f),
                        selectedIconColor = PrimaryPurple
                    )
                )

                // Tab 3: Chat
                NavigationBarItem(
                    selected = selectedTab == AppTab.CHAT,
                    onClick = { viewModel.selectTab(AppTab.CHAT) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (messages.isNotEmpty()) {
                                    Badge(containerColor = PrimaryPurple) {
                                        Text(text = "${messages.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat")
                        }
                    },
                    label = { Text("المراسلات") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryPurple.copy(alpha = 0.2f),
                        selectedIconColor = PrimaryPurple
                    )
                )

                // Tab 4: Network & Guide
                NavigationBarItem(
                    selected = selectedTab == AppTab.NETWORK,
                    onClick = { viewModel.selectTab(AppTab.NETWORK) },
                    icon = {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = "Network")
                    },
                    label = { Text("الشبكة") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryPurple.copy(alpha = 0.2f),
                        selectedIconColor = PrimaryPurple
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.ROOMS -> {
                    RoomsScreen(
                        rooms = rooms,
                        currentRoomId = currentRoomId,
                        peers = peers,
                        userProfile = userProfile,
                        inRoomMessages = inRoomMessages,
                        downloadProgressMap = downloadProgressMap,
                        downloadingIds = downloadingIds,
                        isGroupCallActive = isGroupCallActive,
                        micLevel = micLevel,
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        isOpenMic = isOpenMic,
                        isPushToTalkActive = isPushToTalkActive,
                        isVideoStreaming = isVideoStreaming,
                        remoteVideoFrames = remoteVideoFrames,
                        videoEngine = viewModel.engine.videoEngine,
                        roomJoinError = roomJoinError,
                        typingPeers = typingPeers,
                        isVoiceRecording = isVoiceRecording,
                        voiceRecordingDuration = voiceRecordingDuration,
                        voiceRecordingAmplitudes = voiceRecordingAmplitudes,
                        voiceRecordingCurrentAmp = voiceRecordingCurrentAmp,
                        isPlayingVoiceNote = isPlayingVoiceNote,
                        playingVoiceMessageId = playingVoiceMessageId,
                        voiceNoteProgress = voiceNoteProgress,
                        voiceNoteCurrentMs = voiceNoteCurrentMs,
                        onUserTyping = { isTyping -> viewModel.onUserTyping(isTyping) },
                        onStartVoiceRecording = { viewModel.startVoiceRecording() },
                        onStopAndSendVoiceNote = { viewModel.stopAndSendInRoomVoiceNote() },
                        onCancelVoiceRecording = { viewModel.cancelVoiceRecording() },
                        onTogglePlayVoiceNote = { msg -> viewModel.toggleVoiceNotePlayback(msg) },
                        onSeekVoiceNote = { frac -> viewModel.seekVoiceNote(frac) },
                        onSelectRoom = { roomId -> viewModel.joinRoom(roomId) },
                        onCreateRoomClick = { showAddRoomDialog = true },
                        onInvitePeersClick = { room -> roomToInvite = room },
                        onToggleGroupVoiceCall = { viewModel.toggleGroupVoiceCall() },
                        onToggleOpenMic = { viewModel.toggleOpenMic() },
                        onPushToTalkChange = { pressed -> viewModel.setPushToTalk(pressed) },
                        onToggleMute = { viewModel.toggleMute() },
                        onToggleSpeaker = { viewModel.toggleSpeaker() },
                        onToggleCameraStream = {
                            if (isVideoStreaming) {
                                viewModel.engine.videoEngine.stopCameraStream()
                            }
                            viewModel.engine.broadcastPresence()
                        },
                        onToggleCameraLens = { viewModel.toggleCamera() },
                        onSendInRoomMessage = { text, bitmap -> viewModel.sendInRoomMessage(text, bitmap) },
                        onSendInRoomFile = { uri, caption -> viewModel.sendInRoomFile(uri, caption) },
                        onDownloadFile = { msg -> viewModel.downloadMessageFile(msg) },
                        onOpenFile = { msg -> viewModel.openMessageFile(msg) },
                        onDirectCallPeer = { peer, isVideo -> viewModel.startCall(peer, isVideo) },
                        onClearRoomJoinError = { viewModel.clearRoomJoinError() }
                    )
                }

                AppTab.PEERS -> {
                    PeersScreen(
                        peers = peers,
                        localIp = localIp,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        scannedIpCount = scannedIpCount,
                        currentScanSubnet = currentScanSubnet,
                        lastScanTime = lastScanTime,
                        onRescanClick = { viewModel.rescanNetwork() },
                        onAudioCall = { peer -> viewModel.startCall(peer, false) },
                        onVideoCall = { peer -> viewModel.startCall(peer, true) },
                        onDirectChat = { peer -> viewModel.openDirectChat(peer) },
                        onManualConnectClick = { showManualIpDialog = true }
                    )
                }

                AppTab.CHAT -> {
                    ChatScreen(
                        messages = messages,
                        currentTarget = currentChatTarget,
                        isDirectChat = isDirectChat,
                        currentPeer = currentChatPeer,
                        currentRoomInfo = currentRoomInfo,
                        typingPeers = typingPeers,
                        downloadProgressMap = downloadProgressMap,
                        downloadingIds = downloadingIds,
                        isVoiceRecording = isVoiceRecording,
                        voiceRecordingDuration = voiceRecordingDuration,
                        voiceRecordingAmplitudes = voiceRecordingAmplitudes,
                        voiceRecordingCurrentAmp = voiceRecordingCurrentAmp,
                        isPlayingVoiceNote = isPlayingVoiceNote,
                        playingVoiceMessageId = playingVoiceMessageId,
                        voiceNoteProgress = voiceNoteProgress,
                        voiceNoteCurrentMs = voiceNoteCurrentMs,
                        onUserTyping = { isTyping -> viewModel.onUserTyping(isTyping) },
                        onStartVoiceRecording = { viewModel.startVoiceRecording() },
                        onStopAndSendVoiceNote = { viewModel.stopAndSendVoiceNote() },
                        onCancelVoiceRecording = { viewModel.cancelVoiceRecording() },
                        onTogglePlayVoiceNote = { msg -> viewModel.toggleVoiceNotePlayback(msg) },
                        onSeekVoiceNote = { frac -> viewModel.seekVoiceNote(frac) },
                        onSendMessage = { text, bitmap -> viewModel.sendMessage(text, bitmap) },
                        onSendFile = { uri, caption -> viewModel.sendDirectFile(uri, caption) },
                        onDownloadFile = { msg -> viewModel.downloadMessageFile(msg) },
                        onOpenFile = { msg -> viewModel.openMessageFile(msg) },
                        onCallPeer = { peer, isVideo -> viewModel.startCall(peer, isVideo) }
                    )
                }

                AppTab.NETWORK -> {
                    NetworkGuideScreen(
                        userProfile = userProfile,
                        localIp = localIp,
                        micLevel = micLevel,
                        activePeersCount = peers.size,
                        onEditProfileClick = { showProfileDialog = true },
                        onManualConnectClick = { showManualIpDialog = true }
                    )
                }
            }

            // Fullscreen Active Call Overlay if in 1-to-1 Call
            activeCall?.let { call ->
                CallScreenDialog(
                    activeCall = call,
                    videoEngine = viewModel.engine.videoEngine,
                    remoteVideoFrames = remoteVideoFrames,
                    micLevel = micLevel,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    signalInfo = callSignalInfo,
                    onAccept = { viewModel.acceptCall() },
                    onDecline = { viewModel.endCall() },
                    onEnd = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleCamera = { viewModel.toggleCamera() }
                )
            }
        }
    }

    // User Profile Dialog
    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = userProfile,
            localIp = localIp,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, color, status, bio, userStatus ->
                viewModel.updateProfile(name, color, status, bio, userStatus)
            },
            onLogout = {
                viewModel.logout()
            }
        )
    }

    // Create Room Dialog with Capacity
    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onCreateRoom = { name, desc, capacity ->
                viewModel.createRoom(name, desc, capacity)
            }
        )
    }

    // Invite Peers to Room Dialog
    roomToInvite?.let { room ->
        InvitePeersDialog(
            room = room,
            discoveredPeers = peers,
            onDismiss = { roomToInvite = null },
            onInvitePeer = { peer ->
                viewModel.invitePeerToRoom(peer, room)
            }
        )
    }

    // Incoming Room Invite Dialog
    activeRoomInvitation?.let { invite ->
        IncomingRoomInviteDialog(
            invitation = invite,
            onAccept = { viewModel.acceptRoomInvite(invite) },
            onDecline = { viewModel.declineRoomInvite() }
        )
    }

    // Manual IP Connect Dialog
    if (showManualIpDialog) {
        ManualIpDialog(
            onDismiss = { showManualIpDialog = false },
            onConnect = { ip -> viewModel.manualConnectIp(ip) }
        )
    }
}
