package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ActiveGroupCall
import com.example.model.GroupCallInvitation
import com.example.model.Peer
import com.example.model.PeerVideoFrame
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.GroupCallGridLayoutManager
import com.example.ui.components.RemotePeerVideoView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay

/**
 * Fullscreen Interactive Multi-Peer Group Video Call Dialog with Dynamic Split Screen Grid Layout.
 */
@Composable
fun GroupCallScreenDialog(
    activeGroupCall: ActiveGroupCall,
    userProfile: UserProfile,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    callVolume: Float,
    onEndGroupCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onStartScreenShare: (String, () -> Bitmap?) -> Unit,
    onSendScreenShareFrame: (Bitmap, String) -> Unit,
    onStopScreenShare: () -> Unit,
    onMinimize: () -> Unit = {}
) {
    var callDurationSeconds by remember { mutableStateOf(0L) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showScreenSharePicker by remember { mutableStateOf(false) }

    // Real-time duration timer
    LaunchedEffect(activeGroupCall.startTime) {
        while (true) {
            val elapsed = (System.currentTimeMillis() - activeGroupCall.startTime) / 1000
            callDurationSeconds = elapsed.coerceAtLeast(0L)
            delay(1000)
        }
    }

    // Screen sharing loop
    LaunchedEffect(activeGroupCall.isScreenSharing, activeGroupCall.screenSharedAppName) {
        if (activeGroupCall.isScreenSharing && activeGroupCall.screenSharedAppName != null) {
            val appName = activeGroupCall.screenSharedAppName
            while (activeGroupCall.isScreenSharing) {
                val bitmap = generateScreenShareFrame(appName, System.currentTimeMillis())
                onSendScreenShareFrame(bitmap, appName)
                delay(120)
            }
        }
    }

    Dialog(
        onDismissRequest = onMinimize,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // --- Top Header Bar ---
                    GroupCallHeader(
                        roomName = activeGroupCall.roomName,
                        durationSeconds = callDurationSeconds,
                        participantCount = activeGroupCall.participants.size + 1,
                        isScreenSharing = activeGroupCall.isScreenSharing,
                        sharedAppName = activeGroupCall.screenSharedAppName,
                        onMinimize = onMinimize
                    )

                    // --- Dynamic Multi-User Video Grid (Responsive Animated Grid Layout Manager) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        GroupCallGridLayoutManager(
                            activeGroupCall = activeGroupCall,
                            userProfile = userProfile,
                            videoEngine = videoEngine,
                            remoteVideoFrames = remoteVideoFrames,
                            micLevel = micLevel,
                            isMuted = isMuted
                        )
                    }

                    // --- Bottom Floating Control Dock ---
                    GroupCallControlDock(
                        isMicMuted = isMuted,
                        isCameraOff = activeGroupCall.isCameraOff,
                        isFrontCamera = activeGroupCall.isFrontCamera,
                        isSpeakerOn = isSpeakerOn,
                        isScreenSharing = activeGroupCall.isScreenSharing,
                        callVolume = callVolume,
                        micLevel = micLevel,
                        onToggleMute = onToggleMute,
                        onToggleCamera = onToggleCamera,
                        onSwitchCamera = onSwitchCamera,
                        onToggleSpeaker = onToggleSpeaker,
                        onOpenVolumeDialog = { showVolumeDialog = true },
                        onOpenScreenSharePicker = { showScreenSharePicker = true },
                        onStopScreenShare = onStopScreenShare,
                        onEndCall = onEndGroupCall
                    )
                }

                // Volume Dialog Overlay
                if (showVolumeDialog) {
                    GroupCallVolumeDialog(
                        currentVolume = callVolume,
                        onVolumeChanged = onVolumeChanged,
                        onDismiss = { showVolumeDialog = false }
                    )
                }

                // Screen Share App Picker Dialog
                if (showScreenSharePicker) {
                    ScreenShareAppPickerDialog(
                        onAppSelected = { appName ->
                            showScreenSharePicker = false
                            onStartScreenShare(appName) {
                                generateScreenShareFrame(appName, System.currentTimeMillis())
                            }
                        },
                        onDismiss = { showScreenSharePicker = false }
                    )
                }
            }
        }
    }
}

/**
 * Top Header of the Group Video Call with Room info, timer, and member badge.
 */
@Composable
private fun GroupCallHeader(
    roomName: String,
    durationSeconds: Long,
    participantCount: Int,
    isScreenSharing: Boolean,
    sharedAppName: String?,
    onMinimize: () -> Unit = {}
) {
    Surface(
        color = Color(0xFF1E1B4B).copy(alpha = 0.85f),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Room Name & Live Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Pulsing Red Dot for Live Call
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AccentRose.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = roomName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurple.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "مباشر",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC084FC),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isScreenSharing && sharedAppName != null) {
                        Text(
                            text = "مشاركة الشاشة: $sharedAppName",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    } else {
                        Text(
                            text = "مكالمة فيديو جماعية عالية الدقة عبر Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Timer & Member Count & Minimize Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Participant Count Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$participantCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Duration
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Minimize to Floating PiP Button
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("group_call_minimize_pip")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloseFullscreen,
                        contentDescription = "تصغير إلى نافذة عائمة",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Responsive Split Screen Video Grid layout adapting dynamically based on participant count.
 */
@Composable
private fun GroupCallVideoGrid(
    activeGroupCall: ActiveGroupCall,
    userProfile: UserProfile,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean
) {
    val participants = activeGroupCall.participants
    val totalParticipants = participants.size + 1 // Include self

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth

        when {
            // Case 1: Sole participant (waiting for others in room)
            totalParticipants == 1 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(totalHeight * 0.72f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkCard)
                            .border(2.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!activeGroupCall.isCameraOff) {
                            CameraPreviewSurface(
                                videoEngine = videoEngine,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SelfAvatarView(userProfile = userProfile, micLevel = micLevel, isMuted = isMuted)
                        }

                        // Bottom Self Tag
                        ParticipantNameBadge(
                            name = "أنت (${userProfile.displayName.ifBlank { userProfile.username }})",
                            isMuted = isMuted,
                            isSelf = true,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Friendly waiting card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم بدء المكالمة الجماعية! بانتظار انضمام باقي أعضاء الغرفة...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Case 2: 2 Participants (1-on-1 split: Top/Bottom or Side/Side)
            totalParticipants == 2 -> {
                val peer = participants.first()
                val peerFrame = remoteVideoFrames[peer.id]

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: Remote Peer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkCard)
                            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    ) {
                        PeerVideoTileContent(
                            peer = peer,
                            frame = peerFrame
                        )
                    }

                    // Tile 2: Self
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkCard)
                            .border(
                                width = if (micLevel > 0.08f && !isMuted) 2.dp else 1.5.dp,
                                color = if (micLevel > 0.08f && !isMuted) StatusGreen else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        if (!activeGroupCall.isCameraOff) {
                            CameraPreviewSurface(
                                videoEngine = videoEngine,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SelfAvatarView(userProfile = userProfile, micLevel = micLevel, isMuted = isMuted)
                        }

                        ParticipantNameBadge(
                            name = "أنت",
                            isMuted = isMuted,
                            isSelf = true,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }

            // Case 3: 3 or 4 Participants (2x2 Balanced Grid)
            totalParticipants in 3..4 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1 (Self + Peer 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tile 1: Self
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(
                                    width = if (micLevel > 0.08f && !isMuted) 2.dp else 1.dp,
                                    color = if (micLevel > 0.08f && !isMuted) StatusGreen else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            if (!activeGroupCall.isCameraOff) {
                                CameraPreviewSurface(videoEngine = videoEngine, modifier = Modifier.fillMaxSize())
                            } else {
                                SelfAvatarView(userProfile = userProfile, micLevel = micLevel, isMuted = isMuted)
                            }
                            ParticipantNameBadge(name = "أنت", isMuted = isMuted, isSelf = true, modifier = Modifier.align(Alignment.BottomStart))
                        }

                        // Tile 2: Peer 1
                        val peer1 = participants[0]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            PeerVideoTileContent(peer = peer1, frame = remoteVideoFrames[peer1.id])
                        }
                    }

                    // Row 2 (Peer 2 + Peer 3 / Blank)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tile 3: Peer 2
                        val peer2 = participants[1]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            PeerVideoTileContent(peer = peer2, frame = remoteVideoFrames[peer2.id])
                        }

                        // Tile 4: Peer 3 or Placeholder
                        if (participants.size > 2) {
                            val peer3 = participants[2]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkCard)
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            ) {
                                PeerVideoTileContent(peer = peer3, frame = remoteVideoFrames[peer3.id])
                            }
                        } else {
                            // Blank quadrant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.04f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "متاح للانضمام",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Case 4: 5+ Participants (2-Column Scrollable Grid)
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Item 1: Self
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(
                                    width = if (micLevel > 0.08f && !isMuted) 2.dp else 1.dp,
                                    color = if (micLevel > 0.08f && !isMuted) StatusGreen else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            if (!activeGroupCall.isCameraOff) {
                                CameraPreviewSurface(videoEngine = videoEngine, modifier = Modifier.fillMaxSize())
                            } else {
                                SelfAvatarView(userProfile = userProfile, micLevel = micLevel, isMuted = isMuted)
                            }
                            ParticipantNameBadge(name = "أنت", isMuted = isMuted, isSelf = true, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }

                    // Remote peers
                    items(participants) { peer ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            PeerVideoTileContent(peer = peer, frame = remoteVideoFrames[peer.id])
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders video or animated avatar for a remote peer in the group call.
 */
@Composable
private fun PeerVideoTileContent(
    peer: Peer,
    frame: PeerVideoFrame?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val hasVideo = frame?.bitmap != null && !frame.isCameraOff
        if (hasVideo) {
            Image(
                bitmap = frame!!.bitmap!!.asImageBitmap(),
                contentDescription = "Video from ${peer.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Avatar Fallback with Speaking Wave
            PeerAvatarView(peer = peer)
        }

        // Screen Share Pill if sharing
        if (frame?.isScreenShare == true) {
            Surface(
                color = PrimaryPurple.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = frame.appTitle ?: "شاشة مشتركة",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        // Participant Name & Mute Tag
        ParticipantNameBadge(
            name = peer.name,
            isMuted = peer.isMuted,
            isSelf = false,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun ParticipantNameBadge(
    name: String,
    isMuted: Boolean,
    isSelf: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isMuted) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Muted",
                    tint = AccentRose,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SelfAvatarView(
    userProfile: UserProfile,
    micLevel: Float,
    isMuted: Boolean
) {
    val isSpeaking = micLevel > 0.08f && !isMuted

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isSpeaking) {
            val infiniteTransition = rememberInfiniteTransition(label = "speakWave")
            val waveScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveScale"
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(waveScale)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.25f))
            )
        }

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(userProfile.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            val base64 = userProfile.avatarBase64
            if (!base64.isNullOrEmpty()) {
                val decoded = remember(base64) {
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (decoded != null) {
                    Image(
                        bitmap = decoded.asImageBitmap(),
                        contentDescription = "My Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userProfile.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = userProfile.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PeerAvatarView(peer: Peer) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(peer.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            val base64 = peer.avatarBase64
            if (!base64.isNullOrEmpty()) {
                val decoded = remember(base64) {
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (decoded != null) {
                    Image(
                        bitmap = decoded.asImageBitmap(),
                        contentDescription = "Peer Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = peer.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = peer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Bottom Control Floating Dock for Group Call.
 */
@Composable
private fun GroupCallControlDock(
    isMicMuted: Boolean,
    isCameraOff: Boolean,
    isFrontCamera: Boolean,
    isSpeakerOn: Boolean,
    isScreenSharing: Boolean,
    callVolume: Float,
    micLevel: Float,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenVolumeDialog: () -> Unit,
    onOpenScreenSharePicker: () -> Unit,
    onStopScreenShare: () -> Unit,
    onEndCall: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1B4B).copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // In-Call Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Mic Mute / Unmute
                FloatingActionButton(
                    onClick = onToggleMute,
                    containerColor = if (isMicMuted) AccentRose else Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("group_call_mic_button")
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic Toggle",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Camera Toggle
                FloatingActionButton(
                    onClick = onToggleCamera,
                    containerColor = if (isCameraOff) AccentRose else Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("group_call_camera_button")
                ) {
                    Icon(
                        imageVector = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Camera Toggle",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Switch Camera Lens
                FloatingActionButton(
                    onClick = onSwitchCamera,
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("group_call_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 4. Screen Share
                FloatingActionButton(
                    onClick = {
                        if (isScreenSharing) {
                            onStopScreenShare()
                        } else {
                            onOpenScreenSharePicker()
                        }
                    },
                    containerColor = if (isScreenSharing) StatusGreen else Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("group_call_screen_share_button")
                ) {
                    Icon(
                        imageVector = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                        contentDescription = "Screen Share",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 5. Speaker / Earpiece
                FloatingActionButton(
                    onClick = onToggleSpeaker,
                    containerColor = if (isSpeakerOn) PrimaryPurple else Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("group_call_speaker_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker Toggle",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 6. End / Leave Call (Red)
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("group_call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Volume Tune Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVolumeDialog() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مستوى الصوت: ${(callVolume * 100).toInt()}% • اضغط للضبط",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/**
 * Incoming Group Video Call Alert Dialog for room peers.
 */
@Composable
fun IncomingGroupCallDialog(
    invitation: GroupCallInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = Color(0xFF1E1B4B),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        icon = {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(StatusGreen.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(StatusGreen, Color(0xFF059669))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "مكالمة فيديو جماعية مباشرة!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "بدأ \"${invitation.initiatorName}\" مكالمة فيديو جماعية في غرفة:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPurple.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "🔊 ${invitation.roomName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC084FC),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "انضم الآن لمشاهدة كاميرات المجموعة والتحدث مباشرة عبر شبكة Wi-Fi المحلية بدون إنترنت.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            FloatingActionButton(
                onClick = onAccept,
                containerColor = StatusGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("accept_group_call_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انضمام للمكالمة الجماعية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("decline_group_call_button")
            ) {
                Text("تجاهل", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

/**
 * Volume Slider Dialog for Group Calls.
 */
@Composable
private fun GroupCallVolumeDialog(
    currentVolume: Float,
    onVolumeChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableFloatStateOf(currentVolume) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text("التحكم بمستوى صوت المكالمة الجماعية", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = "Low",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            onVolumeChanged(it)
                        },
                        valueRange = 0f..1.5f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("group_call_volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryPurple,
                            activeTrackColor = PrimaryPurple
                        )
                    )

                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "High",
                        tint = PrimaryPurple
                    )
                }

                Text(
                    text = "يمكن رفع الصوت حتى 150% لسماع جميع أعضاء الغرفة بوضوح.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Screen share frame generator.
 */
private fun generateScreenShareFrame(appName: String, timestamp: Long): Bitmap {
    val width = 480
    val height = 360
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background Gradient
    val bgPaint = Paint().apply {
        color = 0xFF1E1B4B.toInt()
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Grid pattern
    val gridPaint = Paint().apply {
        color = 0xFF312E81.toInt()
        strokeWidth = 1.5f
    }
    for (x in 0 until width step 40) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
    }
    for (y in 0 until height step 40) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    // App Window Card
    val cardPaint = Paint().apply {
        color = 0xFF0F172A.toInt()
    }
    canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), 20f, 20f, cardPaint)

    // App Header Bar
    val headerPaint = Paint().apply {
        color = 0xFF6366F1.toInt()
    }
    canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), 70f, 20f, 20f, headerPaint)

    // App Title Text
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(appName, (width / 2).toFloat(), 52f, textPaint)

    // Content Simulation
    val contentTextPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("بث حي لشاشة: $appName", (width / 2).toFloat(), 140f, contentTextPaint)

    val timeStr = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    canvas.drawText("مزامنة فورية • $timeStr", (width / 2).toFloat(), 180f, contentTextPaint)

    // Dynamic wave bar
    val wavePaint = Paint().apply {
        color = 0xFF10B981.toInt()
        strokeWidth = 4f
    }
    val centerY = 240f
    for (i in 40 until (width - 40) step 10) {
        val offset = kotlin.math.sin((i + timestamp * 0.01) * 0.05).toFloat() * 25f
        canvas.drawLine(i.toFloat(), centerY, i.toFloat(), centerY + offset, wavePaint)
    }

    return bitmap
}
