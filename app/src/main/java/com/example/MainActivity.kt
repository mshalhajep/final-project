package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.components.UserStatusQuickDropdown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.WifiTethering
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
import com.example.ui.NetworkStatusState
import com.example.ui.dialogs.AddRoomDialog
import com.example.ui.dialogs.CallScreenDialog
import com.example.ui.dialogs.GroupCallScreenDialog
import com.example.ui.dialogs.IncomingGroupCallDialog
import com.example.ui.dialogs.IncomingRoomInviteDialog
import com.example.ui.dialogs.InvitePeersDialog
import com.example.ui.dialogs.ManualIpDialog
import com.example.ui.dialogs.UserProfileDialog
import com.example.ui.components.FloatingCallPipOverlay
import com.example.ui.components.NetworkStatusBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NetworkGuideScreen
import com.example.ui.screens.PeersScreen
import com.example.ui.screens.RoomsScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        /** Live video-call marker so PiP is entered only for video calls. */
        @Volatile
        var isVideoCallActiveForPip: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Theme selected in settings (DARK/LIGHT/SYSTEM) is bound at the root so
            // switching applies instantly instead of always following the system.
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            com.example.ui.theme.MyApplicationTheme(
                darkTheme = when (themeMode) {
                    com.example.data.ThemeMode.DARK -> true
                    com.example.data.ThemeMode.LIGHT -> false
                    com.example.data.ThemeMode.SYSTEM -> systemDarkTheme
                }
            ) {
                RootApp(viewModel = viewModel)
            }
        }
        handleNavigationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val target = intent?.getStringExtra("openTarget") ?: return
        if (target == "call") {
            // Incoming call notification tapped: bringing activity to front is sufficient
            return
        }
        val isDirect = intent.getBooleanExtra("isDirect", false)
        com.example.utils.NotificationNavigation.openChat.value = target to isDirect
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // PiP only for active video calls — audio-only calls and normal browsing
        // must not pop a picture-in-picture window.
        if (!isVideoCallActiveForPip) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(9, 16))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current

    // App foreground state gates notification emission; returning to the app clears
    // stale message notifications.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            val resumed = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME
            com.example.utils.LocalNotificationManager.setAppInForeground(resumed)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Open the chat tapped from a message notification
    LaunchedEffect(Unit) {
        com.example.utils.NotificationNavigation.openChat.collect { nav ->
            if (nav != null) {
                com.example.utils.NotificationNavigation.openChat.value = null
                viewModel.openChatFromNotification(nav.first, nav.second)
            }
        }
    }

    // Surface remote busy/ended reasons from outgoing calls
    val toastContext = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.callEndedReason.collect { reason ->
            android.widget.Toast.makeText(toastContext, reason, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Request Audio, Camera & Notification Permissions
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else if (android.os.Build.VERSION.SDK_INT < 29) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    // RULE 2: real screen sharing — MediaProjection consent flow + foreground service
    var pendingScreenShareAppName by remember { mutableStateOf("شاشة النظام") }
    val mediaProjectionManager = remember {
        context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
                as? android.media.projection.MediaProjectionManager
    }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            viewModel.startRealScreenShare(
                appName = pendingScreenShareAppName,
                resultCode = result.resultCode,
                projectionData = result.data!!
            )
        }
    }
    fun requestScreenShare(appName: String) {
        pendingScreenShareAppName = appName
        try {
            val captureIntent = mediaProjectionManager?.createScreenCaptureIntent() ?: return
            screenCaptureLauncher.launch(captureIntent)
        } catch (_: Exception) {
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
    val callWaitingInvite by viewModel.callWaitingInvite.collectAsState()
    val activeGroupCall by viewModel.activeGroupCall.collectAsState()
    val callSignalInfo by viewModel.callSignalInfo.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inRoomMessages by viewModel.inRoomMessages.collectAsState()
    val currentChatTarget by viewModel.currentChatTarget.collectAsState()
    val isDirectChat by viewModel.currentChatIsDirect.collectAsState()
    val currentChatPeer by viewModel.currentChatPeer.collectAsState()
    val typingPeers by viewModel.typingPeers.collectAsState()
    val activeRoomInvitation by viewModel.activeRoomInvitation.collectAsState()
    var incomingGroupCallInvite by remember { mutableStateOf<com.example.model.GroupCallInvitation?>(null) }

    LaunchedEffect(Unit) {
        viewModel.incomingGroupCallInvite.collect { invite ->
            incomingGroupCallInvite = invite
        }
    }
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
    val callVolume by viewModel.callVolume.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    // Floating call emoji reactions (pruned after their animation window)
    val callReactions = remember { androidx.compose.runtime.mutableStateListOf<com.example.model.CallReactionEvent>() }
    LaunchedEffect(Unit) {
        viewModel.callReactions.collect { event -> callReactions.add(event) }
    }
    LaunchedEffect(callReactions.size) {
        if (callReactions.isNotEmpty()) {
            delay(3200)
            callReactions.removeAll { System.currentTimeMillis() - it.id > 3200 }
        }
    }

    // Scanning states
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scannedIpCount by viewModel.scannedIpCount.collectAsState()
    val currentScanSubnet by viewModel.currentScanSubnet.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val isNotificationMuted by viewModel.isNotificationMuted.collectAsState()

    // Dialogs state
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var roomToInvite by remember { mutableStateOf<RoomInfo?>(null) }
    var isCallMinimizedToPip by remember { mutableStateOf(false) }
    var isGroupCallMinimizedToPip by remember { mutableStateOf(false) }

    LaunchedEffect(activeCall) {
        if (activeCall == null || activeCall?.state == com.example.model.CallState.ENDED || activeCall?.state == com.example.model.CallState.IDLE) {
            isCallMinimizedToPip = false
        }
    }

    // PiP entry marker: video calls only (audio calls / idle app never enter PiP)
    LaunchedEffect(activeCall, activeGroupCall) {
        MainActivity.isVideoCallActiveForPip =
            (activeCall?.isVideo == true && activeCall?.state == com.example.model.CallState.CONNECTED) ||
                    activeGroupCall != null
    }

    LaunchedEffect(activeGroupCall) {
        if (activeGroupCall == null) {
            isGroupCallMinimizedToPip = false
        }
    }

    val currentRoomInfo = rooms.find { it.id == currentRoomId }
    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryPurple, SecondarySlate)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = com.example.ui.theme.AppIcons.P2PRadar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LocalConnect",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserStatusDot(
                                    status = userProfile.userStatus,
                                    size = 6.dp,
                                    showBorder = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "@${userProfile.username} • $localIp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick Rescan Network button
                    IconButton(
                        onClick = { viewModel.rescanNetwork() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("top_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Scan Network",
                            tint = if (isScanning) StatusGreen else PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Profile avatar button with presence dot overlay
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("top_profile_button")
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(userProfile.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.username.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            UserStatusDot(
                                status = userProfile.userStatus,
                                size = 8.dp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }

                    // More Menu Button with dropdown options
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("الاتصال بعنوان IP يدوي") },
                                onClick = {
                                    showOverflowMenu = false
                                    showManualIpDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AddLink, contentDescription = null, tint = PrimaryPurple)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isNotificationMuted) "تفعيل التنبيهات والأصوات" else "كتم التنبيهات والأصوات") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleNotificationMute()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isNotificationMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (isNotificationMuted) AccentRose else PrimaryPurple
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل الملف الشخصي والحالة") },
                                onClick = {
                                    showOverflowMenu = false
                                    showProfileDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!isImeVisible) {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Reusable Network Status & Offline Warning Banner
            NetworkStatusBanner(
                networkStatus = networkStatus,
                onRescanClick = { viewModel.rescanNetwork() }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                        playbackSpeed = playbackSpeed,
                        onCyclePlaybackSpeed = { viewModel.cyclePlaybackSpeed() },
                        onUserTyping = { isTyping -> viewModel.onUserTyping(isTyping) },
                        onStartVoiceRecording = { viewModel.startVoiceRecording() },
                        onStopAndSendVoiceNote = { viewModel.stopAndSendInRoomVoiceNote() },
                        onCancelVoiceRecording = { viewModel.cancelVoiceRecording() },
                        onTogglePlayVoiceNote = { msg -> viewModel.toggleVoiceNotePlayback(msg) },
                        onSeekVoiceNote = { frac -> viewModel.seekVoiceNote(frac) },
                        onSelectRoom = { roomId -> viewModel.joinRoom(roomId) },
                        onCreateRoomClick = { showAddRoomDialog = true },
                        onInvitePeersClick = { room -> roomToInvite = room },
                        onStartGroupVideoCall = { roomId, roomName -> viewModel.startGroupVideoCall(roomId, roomName) },
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
                        onCancelDownloadFile = { msg -> viewModel.cancelDownloadFile(msg) },
                        onOpenFile = { msg -> viewModel.openMessageFile(msg) },
                        onDirectCallPeer = { peer, isVideo -> viewModel.startCall(peer, isVideo) },
                        onClearRoomJoinError = { viewModel.clearRoomJoinError() },
                        onEditMessage = { id, text -> viewModel.editMessage(id, text) },
                        onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                        onForwardMessage = { msg, targetId, isDirect -> viewModel.forwardMessage(msg, targetId, isDirect) }
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
                        playbackSpeed = playbackSpeed,
                        onCyclePlaybackSpeed = { viewModel.cyclePlaybackSpeed() },
                        onUserTyping = { isTyping -> viewModel.onUserTyping(isTyping) },
                        onStartVoiceRecording = { viewModel.startVoiceRecording() },
                        onStopAndSendVoiceNote = { viewModel.stopAndSendVoiceNote() },
                        onCancelVoiceRecording = { viewModel.cancelVoiceRecording() },
                        onTogglePlayVoiceNote = { msg -> viewModel.toggleVoiceNotePlayback(msg) },
                        onSeekVoiceNote = { frac -> viewModel.seekVoiceNote(frac) },
                        onSendMessage = { text, bitmap -> viewModel.sendMessage(text, bitmap) },
                        onSendFile = { uri, caption -> viewModel.sendDirectFile(uri, caption) },
                        onDownloadFile = { msg -> viewModel.downloadMessageFile(msg) },
                        onCancelDownloadFile = { msg -> viewModel.cancelDownloadFile(msg) },
                        onOpenFile = { msg -> viewModel.openMessageFile(msg) },
                        onCallPeer = { peer, isVideo -> viewModel.startCall(peer, isVideo) },
                        onEditMessage = { id, text -> viewModel.editMessage(id, text) },
                        onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                        onForwardMessage = { msg, targetId, isDirect -> viewModel.forwardMessage(msg, targetId, isDirect) },
                        peers = peers,
                        rooms = rooms
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

            // Fullscreen Active Call Overlay if in 1-to-1 Call (when not minimized)
            if (activeCall != null && !isCallMinimizedToPip) {
                CallScreenDialog(
                    activeCall = activeCall!!,
                    videoEngine = viewModel.engine.videoEngine,
                    remoteVideoFrames = remoteVideoFrames,
                    micLevel = micLevel,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    callVolume = callVolume,
                    signalInfo = callSignalInfo,
                    onAccept = { viewModel.acceptCall() },
                    onDecline = { viewModel.endCall() },
                    onEnd = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleCallMic() },
                    onToggleSpeaker = { viewModel.toggleCallSpeaker() },
                    onToggleCameraLens = { viewModel.switchCallCamera() },
                    onToggleCameraOff = { viewModel.toggleCallCamera() },
                    onSetVolume = { vol -> viewModel.setCallVolume(vol) },
                    onStartScreenShare = { appName -> requestScreenShare(appName) },
                    onStopScreenShare = { viewModel.stopRealScreenShare() },
                    onMinimize = { isCallMinimizedToPip = true },
                    reactions = callReactions.toList(),
                    onSendReaction = { emoji -> viewModel.sendCallReaction(emoji) }
                )
            }

            // Fullscreen Active Multi-Peer Group Video Call Overlay (when not minimized)
            if (activeGroupCall != null && !isGroupCallMinimizedToPip) {
                GroupCallScreenDialog(
                    activeGroupCall = activeGroupCall!!,
                    userProfile = userProfile,
                    videoEngine = viewModel.engine.videoEngine,
                    remoteVideoFrames = remoteVideoFrames,
                    micLevel = micLevel,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    callVolume = callVolume,
                    onEndGroupCall = { viewModel.leaveGroupVideoCall() },
                    onToggleMute = { viewModel.toggleGroupCallMic() },
                    onToggleSpeaker = { viewModel.toggleGroupCallSpeaker() },
                    onToggleCamera = { viewModel.toggleGroupCallCamera() },
                    onSwitchCamera = { viewModel.switchGroupCallCamera() },
                    onVolumeChanged = { vol -> viewModel.setCallVolume(vol) },
                    onStartScreenShare = { appName -> requestScreenShare(appName) },
                    onStopScreenShare = { viewModel.stopRealScreenShare() },
                    onMinimize = { isGroupCallMinimizedToPip = true },
                    reactions = callReactions.toList(),
                    onSendReaction = { emoji -> viewModel.sendCallReaction(emoji) }
                )
            }

            // Draggable Floating Call Overlay (Picture-in-Picture) when minimized
            if ((activeCall != null && isCallMinimizedToPip) || (activeGroupCall != null && isGroupCallMinimizedToPip)) {
                FloatingCallPipOverlay(
                    activeCall = activeCall,
                    activeGroupCall = activeGroupCall,
                    userProfile = userProfile,
                    videoEngine = viewModel.engine.videoEngine,
                    remoteVideoFrames = remoteVideoFrames,
                    micLevel = micLevel,
                    isMuted = isMuted,
                    onMaximize = {
                        isCallMinimizedToPip = false
                        isGroupCallMinimizedToPip = false
                    },
                    onEndCall = {
                        if (activeCall != null) viewModel.endCall()
                        if (activeGroupCall != null) viewModel.leaveGroupVideoCall()
                        isCallMinimizedToPip = false
                        isGroupCallMinimizedToPip = false
                    },
                    onToggleMute = {
                        if (activeCall != null) viewModel.toggleCallMic()
                        if (activeGroupCall != null) viewModel.toggleGroupCallMic()
                    },
                    onToggleCamera = {
                        if (activeCall != null) viewModel.toggleCallCamera()
                        if (activeGroupCall != null) viewModel.toggleGroupCallCamera()
                    },
                    onSwitchCamera = {
                        if (activeCall != null) viewModel.switchCallCamera()
                        if (activeGroupCall != null) viewModel.switchGroupCallCamera()
                    }
                )
            }
        }
      }
    }

    // Incoming Group Call Dialog (auto-dismisses when the 45s ring timeout elapses)
    incomingGroupCallInvite?.let { invite ->
        LaunchedEffect(invite.callId) {
            delay(45_000L)
            if (incomingGroupCallInvite?.callId == invite.callId) {
                viewModel.dismissIncomingGroupCallRing()
                incomingGroupCallInvite = null
            }
        }
        IncomingGroupCallDialog(
            invitation = invite,
            onAccept = {
                val toJoin = invite
                incomingGroupCallInvite = null
                viewModel.joinGroupVideoCall(toJoin)
            },
            onDecline = {
                incomingGroupCallInvite = null
                viewModel.dismissIncomingGroupCallRing()
            }
        )
    }

    // Call waiting: an incoming call held while the user is already connected
    callWaitingInvite?.let { waiting ->
        AlertDialog(
            onDismissRequest = { viewModel.declineWaitingCall() },
            title = { Text("مكالمة واردة أخرى", fontWeight = FontWeight.Bold) },
            text = {
                Text("${waiting.peer.name} يتصل بك أثناء مكالمتك الحالية. القبول سينهي المكالمة الحالية ويربطك به مباشرة.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptWaitingCall() }) {
                    Text("قبول وإنهاء الحالية", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineWaitingCall() }) {
                    Text("رفض", color = AccentRose)
                }
            }
        )
    }

    // User Profile Dialog
    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = userProfile,
            localIp = localIp,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, color, status, bio, userStatus, avatarUri ->
                viewModel.updateProfile(name, color, status, bio, userStatus, avatarUri)
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
