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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.ScreenShare
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ActiveGroupCall
import com.example.model.CallReactionEvent
import com.example.model.GroupCallInvitation
import com.example.model.Peer
import com.example.model.PeerVideoFrame
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.components.CameraPreviewSurface
import com.example.ui.components.CallEmojiPickerRow
import com.example.ui.components.CallReactionOverlay
import com.example.ui.components.GroupCallGridLayoutManager
import com.example.ui.components.rememberFloatingReactions
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AppIcons
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay

/**
 * Fullscreen Interactive Multi-Peer Group Video Call Dialog with Dynamic Split Screen
 * Grid Layout. RULE 4: dock holds 4 controls + End Call; advanced tools live in the
 * "More Options" bottom sheet.
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
    onStartScreenShare: (String) -> Unit,
    onStopScreenShare: () -> Unit,
    onMinimize: () -> Unit = {},
    isInPipMode: Boolean = false,
    reactions: List<CallReactionEvent> = emptyList(),
    onSendReaction: (String) -> Unit = {}
) {
    var showMoreOptionsSheet by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) { onMinimize() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isInPipMode) {
                            Modifier
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        } else Modifier
                    )
            ) {
                // --- Top Header Bar ---
                if (!isInPipMode) {
                    GroupCallHeader(
                        roomName = activeGroupCall.roomName,
                        startTime = activeGroupCall.startTime,
                        participantCount = activeGroupCall.participants.size + 1,
                        isScreenSharing = activeGroupCall.isScreenSharing,
                        sharedAppName = activeGroupCall.screenSharedAppName,
                        onMinimize = onMinimize
                    )
                }

                // --- Dynamic Multi-User Video Grid (Responsive Animated Grid Layout Manager) ---
                Box(
                    modifier = if (isInPipMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    }
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

                // --- Bottom Floating Control Dock: 4 controls + End Call ---
                if (!isInPipMode) {
                    GroupCallControlDock(
                        isMicMuted = isMuted,
                        isCameraOff = activeGroupCall.isCameraOff,
                        isSpeakerOn = isSpeakerOn,
                        onToggleMute = onToggleMute,
                        onToggleCamera = onToggleCamera,
                        onToggleSpeaker = onToggleSpeaker,
                        onOpenMoreOptions = { showMoreOptionsSheet = true },
                        onEndCall = onEndGroupCall,
                        onSendReaction = onSendReaction
                    )
                }
            }

            if (!isInPipMode) {
                // Floating emoji reactions layer (rises above the control dock)
                val floatingReactions = rememberFloatingReactions(reactions)
                CallReactionOverlay(floatingReactions, modifier = Modifier.matchParentSize())

                // Tier 3: More Options bottom sheet
                if (showMoreOptionsSheet) {
                    GroupCallMoreOptionsSheet(
                        isScreenSharing = activeGroupCall.isScreenSharing,
                        callVolume = callVolume,
                        onDismiss = { showMoreOptionsSheet = false },
                        onToggleScreenShare = {
                            showMoreOptionsSheet = false
                            if (activeGroupCall.isScreenSharing) {
                                onStopScreenShare()
                            } else {
                                onStartScreenShare("شاشة النظام")
                            }
                        },
                        onSwitchCamera = {
                            showMoreOptionsSheet = false
                            onSwitchCamera()
                        },
                        onVolumeChanged = onVolumeChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCallDurationPill(startTime: Long) {
    var durationSeconds by remember(startTime) { mutableStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            durationSeconds = elapsed.coerceAtLeast(0L)
            delay(1000)
        }
    }
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
}

/**
 * Top Header of the Group Video Call with Room info, timer, and member badge.
 */
@Composable
private fun GroupCallHeader(
    roomName: String,
    startTime: Long = 0L,
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
                GroupCallDurationPill(startTime = startTime)

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
 * RULE 4 bottom dock: mic / camera / speaker / more + vibrant red End Call.
 */
@Composable
private fun GroupCallControlDock(
    isMicMuted: Boolean,
    isCameraOff: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenMoreOptions: () -> Unit,
    onEndCall: () -> Unit,
    onSendReaction: (String) -> Unit = {}
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
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 1. Mic Mute / Unmute
            GroupCallDockButton(
                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMicMuted) "مكتوم" else "المايك",
                isActive = isMicMuted,
                activeColor = AccentRose,
                onClick = onToggleMute,
                testTag = "group_call_mic_button"
            )

            // 2. Camera Toggle
            GroupCallDockButton(
                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = if (isCameraOff) "تشغيل" else "فيديو",
                isActive = isCameraOff,
                activeColor = AccentRose,
                onClick = onToggleCamera,
                testTag = "group_call_camera_button"
            )

            // 3. Speaker / Earpiece
            GroupCallDockButton(
                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                label = if (isSpeakerOn) "مكبر" else "أذن",
                isActive = isSpeakerOn,
                activeColor = PrimaryPurple,
                onClick = onToggleSpeaker,
                testTag = "group_call_speaker_button"
            )

            // 4. More Options (switch lens, screen share, volume)
            GroupCallDockButton(
                icon = Icons.Default.MoreVert,
                label = "المزيد",
                isActive = false,
                activeColor = PrimaryCyan,
                onClick = onOpenMoreOptions,
                testTag = "group_call_more_options_button"
            )

            // 5. End / Leave Call (vibrant red)
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
        }
    }
}

@Composable
private fun GroupCallDockButton(
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

/** Tier 3: advanced group call tools in a modal bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCallMoreOptionsSheet(
    isScreenSharing: Boolean,
    callVolume: Float,
    onDismiss: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onSwitchCamera: () -> Unit,
    onVolumeChanged: (Float) -> Unit
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
                text = "خيارات المكالمة الجماعية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Screen share toggle
            GroupMoreOptionRow(
                icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.PresentToAll,
                title = if (isScreenSharing) "إيقاف مشاركة الشاشة" else "مشاركة الشاشة",
                subtitle = if (isScreenSharing) "البث الحي يعمل الآن" else "بث شاشة هاتفك لجميع أعضاء الغرفة",
                tint = if (isScreenSharing) AccentRose else PrimaryPurple,
                onClick = onToggleScreenShare,
                testTag = "group_more_screen_share_option"
            )

            // Switch camera lens
            GroupMoreOptionRow(
                icon = Icons.Default.Cameraswitch,
                title = "تبديل الكاميرا الأمامية/الخلفية",
                subtitle = "الانتقال بين العدستين أثناء المكالمة",
                tint = PrimaryCyan,
                onClick = onSwitchCamera,
                testTag = "group_more_switch_lens_option"
            )

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
                        onVolumeChanged(it)
                    },
                    valueRange = 0f..1.5f,
                    modifier = Modifier.testTag("group_call_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryPurple,
                        activeTrackColor = PrimaryPurple
                    )
                )
            }
        }
    }
}

@Composable
private fun GroupMoreOptionRow(
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

/**
 * RULE 4.2: fullscreen immersive incoming Group Video Call screen — deep OLED dark
 * backdrop, radar waves around the initiator avatar, oversized answer buttons.
 */
@Composable
fun IncomingGroupCallDialog(
    invitation: GroupCallInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val avatarBitmap = remember(invitation.initiatorAvatarBase64) {
        try {
            invitation.initiatorAvatarBase64?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "group_radar_waves")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(2400, easing = LinearEasing)
        ),
        label = "waveProgress"
    )

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Responsive layout: weight()-based middle guarantees the join/decline
        // buttons stay visible with any font scale or screen size.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
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
                            text = "مكالمة فيديو جماعية واردة",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Radar waves around initiator avatar
                    Box(contentAlignment = Alignment.Center) {
                        val avatarColor = Color(invitation.initiatorColor)
                        repeat(3) { index ->
                            val phase = (waveProgress + index / 3f) % 1f
                            val waveSize = 120.dp + (100.dp * phase)
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

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(avatarColor)
                                .border(3.dp, avatarColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = invitation.initiatorName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = invitation.initiatorName.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = invitation.initiatorName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryPurple.copy(alpha = 0.25f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = invitation.roomName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC084FC)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "انضم الآن لمشاهدة كاميرات المجموعة والتحدث مباشرة عبر شبكة Wi-Fi المحلية بدون إنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Fixed bottom answer area — always visible above the gesture bar
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
                            .testTag("decline_group_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تجاهل",
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
                            .testTag("accept_group_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Join Group Call",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "انضمام",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
