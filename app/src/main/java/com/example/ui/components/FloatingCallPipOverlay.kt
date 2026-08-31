package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveCall
import com.example.model.ActiveGroupCall
import com.example.model.PeerVideoFrame
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkCard
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusGreen
import kotlin.math.roundToInt

/**
 * High-performance Draggable Floating Call Overlay (Picture-in-Picture / PiP)
 * for 1-to-1 Calls and Group Room Calls.
 */
@Composable
fun FloatingCallPipOverlay(
    activeCall: ActiveCall?,
    activeGroupCall: ActiveGroupCall?,
    userProfile: UserProfile?,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    onMaximize: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Draggable position offsets
    var offsetX by remember { mutableFloatStateOf(16f) }
    var offsetY by remember { mutableFloatStateOf(120f) }

    val localScreenBitmap by videoEngine.localScreenShareBitmap.collectAsState()
    val isFrontCamera by videoEngine.isFrontCamera.collectAsState()

    val title = when {
        activeCall != null -> activeCall.peer.name
        activeGroupCall != null -> "غرفة: ${activeGroupCall.roomName}"
        else -> "مكالمة جارية"
    }

    val isVideo = activeCall?.isVideo == true || activeGroupCall != null
    val isScreenSharing = activeCall?.isScreenSharing == true || activeGroupCall?.isScreenSharing == true
    val isLocalCameraOff = activeCall?.isCameraOff == true || activeGroupCall?.isCameraOff == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Transparent touch barrier only around the floating box
            }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCard,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(PrimaryPurple, PrimaryCyan))
            ),
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(width = 175.dp, height = 240.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .testTag("floating_call_pip_overlay")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Video / Screen Share / Avatar stream
                if (isVideo) {
                    when {
                        // Local user is screen sharing
                        isScreenSharing && localScreenBitmap != null && !localScreenBitmap!!.isRecycled -> {
                            Image(
                                bitmap = localScreenBitmap!!.asImageBitmap(),
                                contentDescription = "Live Screen Stream",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Remote video frame available
                        activeCall != null && remoteVideoFrames[activeCall.peer.id]?.bitmap != null -> {
                            val frame = remoteVideoFrames[activeCall.peer.id]!!
                            Image(
                                bitmap = frame.bitmap!!.asImageBitmap(),
                                contentDescription = "Remote Stream",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Active group call with any remote frame
                        activeGroupCall != null && remoteVideoFrames.values.any { it.bitmap != null } -> {
                            val firstFrame = remoteVideoFrames.values.first { it.bitmap != null }
                            Image(
                                bitmap = firstFrame.bitmap!!.asImageBitmap(),
                                contentDescription = "Group Video",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Local camera preview active
                        !isLocalCameraOff -> {
                            CameraPreviewSurface(
                                videoEngine = videoEngine,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Placeholder Avatar with Speaking Waveform
                        else -> {
                            PipAvatarPlaceholder(
                                name = title,
                                isSpeaking = micLevel > 0.08f && !isMuted
                            )
                        }
                    }
                } else {
                    PipAvatarPlaceholder(
                        name = title,
                        isSpeaking = micLevel > 0.08f && !isMuted
                    )
                }

                // Dark gradient overlay on top & bottom for high-contrast controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.75f),
                                0.25f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                )

                // 2. Top Bar: Live Status + Name + Maximize button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Maximize / Expand button
                    IconButton(
                        onClick = onMaximize,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .testTag("pip_maximize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Maximize",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Screen Share Indicator if active
                if (isScreenSharing) {
                    Surface(
                        color = PrimaryPurple.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenShare,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "بث مباشر",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Bottom Control Row: Mic, Camera, Switch Camera, End Call
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) AccentRose.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Switch camera (front/back)
                    if (isVideo && !isLocalCameraOff) {
                        IconButton(
                            onClick = onSwitchCamera,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // End Call
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentRose)
                            .testTag("pip_end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipAvatarPlaceholder(
    name: String,
    isSpeaking: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        if (isSpeaking) {
            val infiniteTransition = rememberInfiniteTransition(label = "pipSpeak")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "speakScale"
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.3f))
            )
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryPurple, PrimaryCyan)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
