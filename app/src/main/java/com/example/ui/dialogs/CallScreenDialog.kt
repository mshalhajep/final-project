package com.example.ui.dialogs

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ActiveCall
import com.example.model.CallReactionEvent
import com.example.model.CallState
import com.example.model.ConnectionQualityLevel
import com.example.model.Peer
import com.example.model.PeerSignalInfo
import com.example.model.PeerVideoFrame
import com.example.network.VideoEngine
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CallEmojiPickerRow
import com.example.ui.components.CallReactionOverlay
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.RemotePeerVideoView
import com.example.ui.components.rememberFloatingReactions
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AppIcons
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay

/**
 * RULE 4: ergonomic 3-tier call screen.
 * Tier 1 top bar: encryption badge, duration timer, signal quality, minimize to PiP.
 * Tier 2 bottom dock: mic / camera / speaker / more + red End Call (max 5 items).
 * Tier 3 "More Options" bottom sheet: screen share, camera lens, volume, diagnostics.
 */
@Composable
fun CallScreenDialog(
    activeCall: ActiveCall,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    callVolume: Float = 1.0f,
    signalInfo: PeerSignalInfo? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleCameraOff: () -> Unit,
    onSetVolume: (Float) -> Unit = {},
    onStartScreenShare: (String) -> Unit = {},
    onStopScreenShare: () -> Unit = {},
    onMinimize: () -> Unit = {},
    reactions: List<CallReactionEvent> = emptyList(),
    onSendReaction: (String) -> Unit = {}
) {
    var callSeconds by remember { mutableStateOf(0) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(activeCall.state) {
        if (activeCall.state == CallState.CONNECTED) {
            callSeconds = 0
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    val minutes = callSeconds / 60
    val seconds = callSeconds % 60
    val durationText = "\u200E${String.format("%02d:%02d", minutes, seconds)}"

    Dialog(
        onDismissRequest = onMinimize,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            when (activeCall.state) {
                CallState.OUTGOING_RINGING -> {
                    OutgoingCallView(
                        activeCall = activeCall,
                        onCancel = onEnd
                    )
                }

                CallState.INCOMING_RINGING -> {
                    IncomingCallView(
                        activeCall = activeCall,
                        onAccept = onAccept,
                        onDecline = onDecline
                    )
                }

                CallState.CONNECTED -> {
                    ConnectedCallView(
                        activeCall = activeCall,
                        durationText = durationText,
                        videoEngine = videoEngine,
                        remoteVideoFrames = remoteVideoFrames,
                        micLevel = micLevel,
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        callVolume = callVolume,
                        signalInfo = signalInfo,
                        onEnd = onEnd,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleCameraLens = onToggleCameraLens,
                        onToggleCameraOff = onToggleCameraOff,
                        onSetVolume = onSetVolume,
                        onStartScreenShare = onStartScreenShare,
                        onStopScreenShare = onStopScreenShare,
                        onMinimize = onMinimize,
                        onOpenMoreOptions = { showMoreOptionsSheet = true },
                        onSendReaction = onSendReaction
                    )
                }

                CallState.ENDED, CallState.IDLE -> {
                    // Closed
                }
            }

            // Floating emoji reactions layer (rises above the control dock)
            val floatingReactions = rememberFloatingReactions(reactions)
            CallReactionOverlay(floatingReactions)

            // Tier 3: More Options bottom sheet
            if (showMoreOptionsSheet && activeCall.state == CallState.CONNECTED) {
                CallMoreOptionsSheet(
                    isVideoCall = activeCall.isVideo,
                    isCameraOff = activeCall.isCameraOff,
                    isScreenSharing = activeCall.isScreenSharing,
                    callVolume = callVolume,
                    signalInfo = signalInfo,
                    onDismiss = { showMoreOptionsSheet = false },
                    onSetVolume = onSetVolume,
                    onToggleScreenShare = {
                        showMoreOptionsSheet = false
                        if (activeCall.isScreenSharing) {
                            onStopScreenShare()
                        } else {
                            onStartScreenShare("شاشة النظام")
                        }
                    },
                    onSwitchCameraLens = {
                        showMoreOptionsSheet = false
                        onToggleCameraLens()
                    }
                )
            }
        }
    }
}

@Composable
private fun PeerAvatarDisplay(
    peer: Peer,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(peer.avatarBase64) {
        if (!peer.avatarBase64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(peer.avatarBase64, Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(peer.avatarColor)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = peer.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (!peer.avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = peer.avatarUri,
                contentDescription = peer.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = peer.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp
            )
        }
    }
}

/** Animated expanding radial waves behind a ringing avatar. */
@Composable
private fun RingingWavesAvatar(
    peer: Peer,
    avatarSize: androidx.compose.ui.unit.Dp = 130.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_waves")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(2400, easing = LinearEasing)
        ),
        label = "waveProgress"
    )
    val avatarColor = Color(peer.avatarColor)

    Box(contentAlignment = Alignment.Center) {
        repeat(3) { index ->
            val phase = (waveProgress + index / 3f) % 1f
            val waveSize = avatarSize + (110.dp * phase)
            Box(
                modifier = Modifier
                    .size(waveSize)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = avatarColor.copy(alpha = (1f - phase) * 0.55f),
                        shape = CircleShape
                    )
            )
        }

        PeerAvatarDisplay(
            peer = peer,
            size = avatarSize,
            modifier = Modifier.border(
                3.dp,
                avatarColor.copy(alpha = 0.4f),
                CircleShape
            )
        )
    }
}

@Composable
private fun OutgoingCallView(
    activeCall: ActiveCall,
    onCancel: () -> Unit
) {
    // Responsive layout: the middle block absorbs all spare height (weight) so the
    // cancel button stays pinned above the gesture bar even with large font scale.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                RingingWavesAvatar(peer = activeCall.peer, avatarSize = 120.dp)

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = activeCall.peer.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (activeCall.isVideo) "جاري الاتصال بالفيديو المباشر..." else "جاري الاتصال الصوتي المباشر...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اتصال P2P مباشر • ${activeCall.peer.ip}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryCyan
                        )
                    }
                }
            }
        }

        // Fixed bottom action area — never pushed off-screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onCancel,
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Cancel Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "إلغاء",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * RULE 4.2: fullscreen immersive incoming call screen — deep dark backdrop, radial
 * radar waves around the caller avatar, generously spaced oversized answer buttons.
 */
@Composable
private fun IncomingCallView(
    activeCall: ActiveCall,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Responsive layout: weight()-based middle area guarantees the Accept/Decline
    // buttons remain fully visible and centered above the gesture bar regardless
    // of font scale (large system fonts) or screen size.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0B22),
                        DarkBackground,
                        Color(0xFF12061C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (activeCall.isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                RingingWavesAvatar(peer = activeCall.peer, avatarSize = 120.dp)

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = activeCall.peer.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اتصال P2P مباشر • ${activeCall.peer.ip}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryCyan
                        )
                    }
                }
            }
        }

        // Fixed bottom answer area — generous spacing, large touch targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onDecline,
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(74.dp)
                        .testTag("decline_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "رفض",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onAccept,
                    containerColor = AccentGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(74.dp)
                        .testTag("accept_call_button")
                ) {
                    Icon(
                        imageVector = if (activeCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Accept",
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "قبول",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ConnectedCallView(
    activeCall: ActiveCall,
    durationText: String,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    callVolume: Float,
    signalInfo: PeerSignalInfo?,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleCameraOff: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onStartScreenShare: (String) -> Unit,
    onStopScreenShare: () -> Unit,
    onMinimize: () -> Unit,
    onOpenMoreOptions: () -> Unit,
    onSendReaction: (String) -> Unit
) {
    val isCameraOff = activeCall.isCameraOff
    val isRemoteCameraOff = activeCall.isRemoteCameraOff
    val isRemoteMuted = activeCall.isRemoteMuted
    val isScreenSharing = activeCall.isScreenSharing
    val localScreenShareBitmap by videoEngine.localScreenShareBitmap.collectAsState()
    val remoteFrame = remoteVideoFrames[activeCall.peer.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Main view area: Fullscreen Video, Screen Share, or Audio profile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Video area shows for video calls AND for audio-only calls whose peer
            // is broadcasting their screen.
            val showVideoArea = (activeCall.isVideo && !isRemoteCameraOff) ||
                    remoteFrame?.isScreenShare == true
            if (showVideoArea) {
                RemotePeerVideoView(
                    bitmap = remoteFrame?.bitmap,
                    peerName = activeCall.peer.name,
                    modifier = Modifier.fillMaxSize()
                )

                // If remote is sharing screen, display banner
                if (isScreenSharing) {
                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PresentToAll,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشاركة شاشة: ${activeCall.screenSharedAppName ?: "بث مباشر"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Local Camera / Screen Share Preview (hidden in pure audio calls)
                if (localScreenShareBitmap != null || activeCall.isVideo) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (localScreenShareBitmap != null) PrimaryPurple else if (isCameraOff) AccentRose else PrimaryCyan
                        ),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 64.dp, end = 16.dp)
                            .size(width = 110.dp, height = 160.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (localScreenShareBitmap != null && !localScreenShareBitmap!!.isRecycled) {
                        // Live local screen broadcast preview
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = localScreenShareBitmap!!.asImageBitmap(),
                                contentDescription = "بث شاشتك المباشر",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                color = PrimaryPurple.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "بث شاشتك",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    } else if (isCameraOff) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "كاميرتك متوقفة",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        CameraPreviewSurface(
                            videoEngine = videoEngine,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    }
                }
            } else {
                // Audio Only / Video Off State: Display Peer Profile Avatar + Waveform + Camera Off notification
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        PeerAvatarDisplay(
                            peer = activeCall.peer,
                            size = 130.dp
                        )

                        if (isRemoteCameraOff && activeCall.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentRose),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = "Remote Camera Off",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = activeCall.peer.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryCyan
                    )

                    if (isRemoteCameraOff && activeCall.isVideo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = AccentRose.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "أوقف الطرف الآخر الكاميرا • الصوت مستمر",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentRose,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    AudioWaveformVisualizer(
                        isSpeaking = micLevel > 0.08f && !isMuted,
                        audioLevel = micLevel,
                        barCount = 13,
                        maxHeight = 46.dp
                    )
                }
            }

            // Tier 1 Top App Bar: encryption badge + signal quality + minimize (PiP)
            CallTopBar(
                peerName = activeCall.peer.name,
                durationText = durationText,
                signalInfo = signalInfo,
                isRemoteMuted = isRemoteMuted,
                onMinimize = onMinimize,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Tier 2 Bottom Control Dock: quick reactions + exactly 4 controls + End Call
        Surface(
            color = Color.Black.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick floating emoji reactions strip
                CallEmojiPickerRow(
                    onSendReaction = onSendReaction,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 1. Mic Toggle
                CallDockButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "مكتوم" else "المايك",
                    isActive = isMuted,
                    activeColor = AccentRose,
                    onClick = onToggleMute,
                    testTag = "call_mute_mic_button"
                )

                // 2. Camera Toggle (video calls only)
                if (activeCall.isVideo) {
                    CallDockButton(
                        icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        label = if (isCameraOff) "تشغيل" else "فيديو",
                        isActive = isCameraOff,
                        activeColor = AccentRose,
                        onClick = onToggleCameraOff,
                        testTag = "call_toggle_camera_off_button"
                    )
                }

                // 3. Speaker Toggle
                CallDockButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                    label = if (isSpeakerOn) "مكبر" else "أذن",
                    isActive = isSpeakerOn,
                    activeColor = PrimaryCyan,
                    onClick = onToggleSpeaker,
                    testTag = "call_speaker_button"
                )

                // 4. More Options
                CallDockButton(
                    icon = Icons.Default.MoreVert,
                    label = "المزيد",
                    isActive = false,
                    activeColor = PrimaryPurple,
                    onClick = onOpenMoreOptions,
                    testTag = "call_more_options_button"
                )

                // 5. End Call (vibrant red)
                FloatingActionButton(
                    onClick = onEnd,
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("call_end_active_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(26.dp)
                    )
                }
                }
            }
        }
    }
}

/** Tier 1: glass top bar with encryption badge, timer, signal quality and PiP. */
@Composable
private fun CallTopBar(
    peerName: String,
    durationText: String,
    signalInfo: PeerSignalInfo?,
    isRemoteMuted: Boolean,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // End-to-end encryption badge
            Surface(
                color = StatusGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ShieldEncryption,
                        contentDescription = "تشفير تام",
                        tint = StatusGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشفّر",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }

            // Peer + duration pill
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryCyan
                    )
                    if (isRemoteMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Remote Muted",
                            tint = AccentRose,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (signalInfo != null) {
                SignalQualityPill(signalInfo = signalInfo)
            }

            // Minimize to Floating PiP button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("minimize_call_to_pip")
            ) {
                Icon(
                    imageVector = Icons.Default.CloseFullscreen,
                    contentDescription = "تصغير المكالمة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Compact live signal quality indicator (bars + latency). */
@Composable
private fun SignalQualityPill(signalInfo: PeerSignalInfo) {
    val qualityColor = when (signalInfo.quality) {
        ConnectionQualityLevel.EXCELLENT -> StatusGreen
        ConnectionQualityLevel.GOOD -> AccentGreen
        ConnectionQualityLevel.FAIR -> Color(0xFFF59E0B)
        ConnectionQualityLevel.POOR, ConnectionQualityLevel.DISCONNECTED -> AccentRose
    }

    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            repeat(4) { bar ->
                val isActiveBar = bar < signalInfo.bars
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(width = 4.dp, height = (4 + bar * 3).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActiveBar) qualityColor else Color.White.copy(alpha = 0.25f)
                        )
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${signalInfo.latencyMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun CallDockButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f))
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.8f)
        )
    }
}

/** Tier 3: "More Options" modal bottom sheet for advanced call tools. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallMoreOptionsSheet(
    isVideoCall: Boolean,
    isCameraOff: Boolean,
    isScreenSharing: Boolean,
    callVolume: Float,
    signalInfo: PeerSignalInfo?,
    onDismiss: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onToggleScreenShare: () -> Unit,
    onSwitchCameraLens: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var volume by remember(callVolume) { mutableFloatStateOf(callVolume) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161230),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "خيارات المكالمة المتقدمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Screen share toggle
            MoreOptionRow(
                icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.PresentToAll,
                title = if (isScreenSharing) "إيقاف مشاركة الشاشة" else "مشاركة الشاشة",
                subtitle = if (isScreenSharing) "البث الحي يعمل الآن" else "بث شاشة هاتفك مباشرة للطرف الآخر",
                tint = if (isScreenSharing) AccentRose else PrimaryPurple,
                onClick = onToggleScreenShare,
                testTag = "more_screen_share_option"
            )

            // Switch camera lens
            if (isVideoCall && !isCameraOff) {
                MoreOptionRow(
                    icon = Icons.Default.Cameraswitch,
                    title = "تبديل الكاميرا الأمامية/الخلفية",
                    subtitle = "الانتقال بين العدستين أثناء المكالمة",
                    tint = PrimaryCyan,
                    onClick = onSwitchCameraLens,
                    testTag = "more_switch_lens_option"
                )
            }

            // Call volume slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "مستوى صوت المكالمة (${(volume * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        onSetVolume(it)
                    },
                    valueRange = 0f..1.5f,
                    modifier = Modifier.testTag("call_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple
                    )
                )
            }

            // Live Network Diagnostics HUD
            if (signalInfo != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DiagnosticStat("الكمون", "${signalInfo.latencyMs}ms")
                        DiagnosticStat("الاهتزاز", "${signalInfo.jitterMs}ms")
                        DiagnosticStat("البيانات", "${signalInfo.bitrateKbps}kbps")
                        DiagnosticStat("الجودة", signalInfo.quality.labelAr)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
