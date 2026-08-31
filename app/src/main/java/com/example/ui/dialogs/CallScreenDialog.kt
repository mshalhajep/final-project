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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ActiveCall
import com.example.model.CallState
import com.example.model.Peer
import com.example.model.PeerSignalInfo
import com.example.model.PeerVideoFrame
import com.example.network.VideoEngine
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.RemotePeerVideoView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay

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
    onStartScreenShare: (String, () -> Bitmap?) -> Unit = { _, _ -> },
    onStopScreenShare: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    var callSeconds by remember { mutableStateOf(0) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showScreenSharePicker by remember { mutableStateOf(false) }

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
    val durationText = String.format("%02d:%02d", minutes, seconds)

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
                        onEnd = onEnd,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleCameraLens = onToggleCameraLens,
                        onToggleCameraOff = onToggleCameraOff,
                        onOpenVolumeDialog = { showVolumeDialog = true },
                        onOpenScreenSharePicker = { showScreenSharePicker = true },
                        onStopScreenShare = onStopScreenShare,
                        onMinimize = onMinimize
                    )
                }

                CallState.ENDED, CallState.IDLE -> {
                    // Closed
                }
            }

            // Volume Adjustment Floating Dialog
            if (showVolumeDialog) {
                CallVolumeDialog(
                    currentVolume = callVolume,
                    onVolumeChanged = onSetVolume,
                    onDismiss = { showVolumeDialog = false }
                )
            }

            // Screen Sharing App Picker Dialog
            if (showScreenSharePicker) {
                ScreenShareAppPickerDialog(
                    onAppSelected = { appName ->
                        onStartScreenShare(appName) {
                            generateScreenShareFrame(appName, System.currentTimeMillis())
                        }
                        showScreenSharePicker = false
                    },
                    onDismiss = { showScreenSharePicker = false }
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

@Composable
private fun OutgoingCallView(
    activeCall: ActiveCall,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "outgoing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(activeCall.peer.avatarColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                PeerAvatarDisplay(
                    peer = activeCall.peer,
                    size = 110.dp
                )
            }

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
    }
}

@Composable
private fun IncomingCallView(
    activeCall: ActiveCall,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "incoming_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(activeCall.peer.avatarColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                PeerAvatarDisplay(
                    peer = activeCall.peer,
                    size = 110.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = activeCall.peer.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeCall.isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline (Red)
            FloatingActionButton(
                onClick = onDecline,
                containerColor = AccentRose,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("decline_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Accept (Green)
            FloatingActionButton(
                onClick = onAccept,
                containerColor = AccentGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("accept_call_button")
            ) {
                Icon(
                    imageVector = if (activeCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Accept",
                    modifier = Modifier.size(32.dp)
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
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleCameraOff: () -> Unit,
    onOpenVolumeDialog: () -> Unit,
    onOpenScreenSharePicker: () -> Unit,
    onStopScreenShare: () -> Unit,
    onMinimize: () -> Unit
) {
    val isCameraOff = activeCall.isCameraOff
    val isRemoteCameraOff = activeCall.isRemoteCameraOff
    val isRemoteMuted = activeCall.isRemoteMuted
    val isScreenSharing = activeCall.isScreenSharing
    val isLocalScreenSharing = activeCall.isScreenSharing // local or remote state tracking
    val localScreenShareBitmap by videoEngine.localScreenShareBitmap.collectAsState()

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
            if (activeCall.isVideo && !isRemoteCameraOff) {
                // Video Mode or Remote Screen Share Mode
                val remoteFrame = remoteVideoFrames[activeCall.peer.id]
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
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenShare,
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

                // Local Camera / Screen Share Preview
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

            // Top Status Bar: Status Pill + Minimize (PiP) button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp)
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
                            text = "${activeCall.peer.name} • $durationText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        if (isRemoteMuted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = "Remote Muted",
                                tint = AccentRose,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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

        // Action & Control Dock at bottom with clear buttons
        Surface(
            color = Color.Black.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                // Main Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Microphone Toggle
                    CallDockButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "مكتوم" else "كتم المايك",
                        isActive = isMuted,
                        activeColor = AccentRose,
                        onClick = onToggleMute,
                        testTag = "call_mute_mic_button"
                    )

                    // Loudspeaker Toggle
                    CallDockButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        label = if (isSpeakerOn) "مكبر الصوت" else "سماعة الأذن",
                        isActive = isSpeakerOn,
                        activeColor = PrimaryCyan,
                        onClick = onToggleSpeaker,
                        testTag = "call_speaker_button"
                    )

                    // Volume Tuning Slider Opener
                    CallDockButton(
                        icon = Icons.Default.GraphicEq,
                        label = "مستوى الصوت (${(callVolume * 100).toInt()}%)",
                        isActive = callVolume < 0.95f || callVolume > 1.05f,
                        activeColor = PrimaryPurple,
                        onClick = onOpenVolumeDialog,
                        testTag = "call_volume_button"
                    )

                    // Video Camera Controls (if in video mode)
                    if (activeCall.isVideo) {
                        // Toggle Camera On/Off (Mute Video)
                        CallDockButton(
                            icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            label = if (isCameraOff) "تشغيل الفيديو" else "كتم الفيديو",
                            isActive = isCameraOff,
                            activeColor = AccentRose,
                            onClick = onToggleCameraOff,
                            testTag = "call_toggle_camera_off_button"
                        )

                        // Switch Camera Lens (Front/Back)
                        if (!isCameraOff) {
                            CallDockButton(
                                icon = Icons.Default.Cameraswitch,
                                label = "تبديل العدسة",
                                isActive = false,
                                activeColor = PrimaryCyan,
                                onClick = onToggleCameraLens,
                                testTag = "call_switch_lens_button"
                            )
                        }
                    }

                    // Screen Share Button
                    if (isScreenSharing) {
                        CallDockButton(
                            icon = Icons.Default.StopScreenShare,
                            label = "إيقاف المشاركة",
                            isActive = true,
                            activeColor = AccentRose,
                            onClick = onStopScreenShare,
                            testTag = "call_stop_screen_share_button"
                        )
                    } else {
                        CallDockButton(
                            icon = Icons.Default.PresentToAll,
                            label = "مشاركة الشاشة",
                            isActive = false,
                            activeColor = PrimaryPurple,
                            onClick = onOpenScreenSharePicker,
                            testTag = "call_start_screen_share_button"
                        )
                    }

                    // End Call Button
                    FloatingActionButton(
                        onClick = onEnd,
                        containerColor = AccentRose,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(54.dp)
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

@Composable
private fun CallDockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 3.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f))
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(22.dp)
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

@Composable
fun CallVolumeDialog(
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
                Text("التحكم بمستوى صوت المكالمة", fontWeight = FontWeight.Bold)
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
                            .testTag("call_volume_slider"),
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
                    text = "يمكن رفع الصوت حتى 150% لتوضيح المحادثة في البيئات الصاخبة.",
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

data class ShareableAppOption(
    val name: String,
    val description: String,
    val iconColor: Long
)

@Composable
fun ScreenShareAppPickerDialog(
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val apps = remember {
        listOf(
            ShareableAppOption("شاشة الهاتف بالكامل", "بث الشاشة وتصفح التطبيقات مباشرة", 0xFF0EA5E9),
            ShareableAppOption("لوحة الرسم والملاحظات", "مشاركة مستند تفاعلي ومخططات حية", 0xFF8B5CF6),
            ShareableAppOption("عارض الصور والملفات", "استعراض الصور والمستندات المشتركة", 0xFF10B981),
            ShareableAppOption("متصفح الويب المحلي", "مشاركة نتائج البحث والمواقع", 0xFFF59E0B)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenShare,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text("اختر ما تريد مشاركته", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "سيتم إرسال بث الشاشة والتطبيق المحدد مباشرة وبأعلى دقة للطرف الآخر في المكالمة:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(apps) { app ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app.name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(app.iconColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PresentToAll,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = app.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        confirmButton = {}
    )
}

private fun generateScreenShareFrame(appName: String, timestamp: Long): Bitmap {
    val width = 480
    val height = 640
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Background
    paint.color = 0xFF1E293B.toInt()
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Header bar
    paint.color = 0xFF8B5CF6.toInt()
    canvas.drawRect(0f, 0f, width.toFloat(), 60f, paint)

    // App title
    paint.color = 0xFFFFFFFF.toInt()
    paint.textSize = 24f
    paint.isFakeBoldText = true
    canvas.drawText("مشاركة: $appName", 24f, 40f, paint)

    // Content cards
    paint.color = 0xFF334155.toInt()
    paint.isFakeBoldText = false
    canvas.drawRoundRect(20f, 80f, width - 20f, 220f, 16f, 16f, paint)
    canvas.drawRoundRect(20f, 240f, width - 20f, 380f, 16f, 16f, paint)
    canvas.drawRoundRect(20f, 400f, width - 20f, 540f, 16f, 16f, paint)

    // Text inside cards
    paint.color = 0xFF38BDF8.toInt()
    paint.textSize = 18f
    canvas.drawText("بث تفاعلي حي عبر LocalConnect P2P", 36f, 120f, paint)
    paint.color = 0xFFCBD5E1.toInt()
    paint.textSize = 14f
    canvas.drawText("معدل التحديث: 30 إطار في الثانية • دقة HD", 36f, 150f, paint)
    canvas.drawText("الوقت: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(timestamp))}", 36f, 180f, paint)

    return bmp
}
