package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ActiveCall
import com.example.model.CallState
import com.example.model.PeerSignalInfo
import com.example.model.PeerVideoFrame
import com.example.network.VideoEngine
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CallSignalMonitor
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.RemotePeerVideoView
import com.example.ui.components.SignalBarsGraphic
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyan
import kotlinx.coroutines.delay

@Composable
fun CallScreenDialog(
    activeCall: ActiveCall,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    signalInfo: PeerSignalInfo? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit
) {
    var callSeconds by remember { mutableStateOf(0) }

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

    val currentSignal = signalInfo ?: activeCall.signalInfo

    Dialog(
        onDismissRequest = { /* Modal call, dismiss through explicit action */ },
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
                        signalInfo = currentSignal,
                        durationText = durationText,
                        videoEngine = videoEngine,
                        remoteVideoFrames = remoteVideoFrames,
                        micLevel = micLevel,
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        onEnd = onEnd,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleCamera = onToggleCamera
                    )
                }

                CallState.ENDED, CallState.IDLE -> {
                    // Closed
                }
            }
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(activeCall.peer.avatarColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(activeCall.peer.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = activeCall.peer.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeCall.isVideo) "مكالمة فيديو مباشرة في الشبكة..." else "مكالمة صوتية مباشرة في الشبكة...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    SignalBarsGraphic(bars = 4, activeColor = AccentGreen, modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اتصال محلي مباشر • ${activeCall.peer.ip}",
                        style = MaterialTheme.typography.labelSmall,
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
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                modifier = Modifier.size(36.dp)
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(activeCall.peer.avatarColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(activeCall.peer.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = activeCall.peer.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeCall.isVideo) "مكالمة فيديو واردة من الشبكة المحلية" else "مكالمة صوتية واردة من الشبكة المحلية",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    SignalBarsGraphic(bars = 4, activeColor = AccentGreen, modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اتصال P2P مباشر • ${activeCall.peer.ip}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryCyan
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline (Red)
            FloatingActionButton(
                onClick = onDecline,
                containerColor = AccentRose,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Accept (Green)
            FloatingActionButton(
                onClick = onAccept,
                containerColor = AccentGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (activeCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Accept",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedCallView(
    activeCall: ActiveCall,
    signalInfo: PeerSignalInfo?,
    durationText: String,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (activeCall.isVideo) {
            // Video Mode: Fullscreen remote stream
            val remoteFrame = remoteVideoFrames[activeCall.peer.id]
            RemotePeerVideoView(
                bitmap = remoteFrame?.bitmap,
                peerName = activeCall.peer.name,
                modifier = Modifier.fillMaxSize()
            )

            // Local PiP Preview in top-end corner
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryCyan),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                CameraPreviewSurface(
                    videoEngine = videoEngine,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Status & Signal Monitor Area for Video Mode
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call status banner
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
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
                            color = Color.White
                        )
                    }
                }

                // Signal Strength & Connectivity Monitor
                CallSignalMonitor(
                    signalInfo = signalInfo,
                    peerIp = activeCall.peer.ip,
                    isVideo = true
                )
            }
        } else {
            // Audio Mode: Large Profile Avatar, Waveform, and Signal Monitor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(activeCall.peer.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = activeCall.peer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryCyan
                )

                Spacer(modifier = Modifier.height(16.dp))
                AudioWaveformVisualizer(
                    isSpeaking = micLevel > 0.08f && !isMuted,
                    audioLevel = micLevel,
                    barCount = 9,
                    maxHeight = 40.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Signal Strength Monitor in Audio Call
                CallSignalMonitor(
                    signalInfo = signalInfo,
                    peerIp = activeCall.peer.ip,
                    isVideo = false,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Top Status Pill
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
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
                        color = Color.White
                    )
                }
            }
        }

        // Control Dock at bottom
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) AccentRose.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) AccentRose else Color.White
                    )
                }

                // Speaker toggle
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) PrimaryCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Speaker",
                        tint = if (isSpeakerOn) PrimaryCyan else Color.White
                    )
                }

                // Camera Switch (if video)
                if (activeCall.isVideo) {
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }

                // End Call button
                FloatingActionButton(
                    onClick = onEnd,
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
