package com.example.ui.components

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveGroupCall
import com.example.model.Peer
import com.example.model.PeerVideoFrame
import com.example.model.UserProfile
import com.example.network.VideoEngine
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkCard
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusGreen

/**
 * Model representing a single participant tile inside the group call grid.
 */
data class VideoTileModel(
    val id: String,
    val isSelf: Boolean,
    val name: String,
    val avatarColor: Long,
    val avatarBase64: String?,
    val isMuted: Boolean,
    val isCameraOff: Boolean,
    val isScreenSharing: Boolean,
    val screenShareAppName: String?,
    val videoFrame: PeerVideoFrame?,
    val isSpeaking: Boolean
)

/**
 * Calculated 2D geometric bounds for layout manager positioning.
 */
data class TileGeometry(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp
)

/**
 * Responsive Animated Grid Layout Manager for Group Video Calls.
 * Automatically recalculates and smoothly animates positions, dimensions,
 * and scales of video tiles when participants join, leave, speak, or share screen.
 */
@Composable
fun GroupCallGridLayoutManager(
    activeGroupCall: ActiveGroupCall,
    userProfile: UserProfile,
    videoEngine: VideoEngine,
    remoteVideoFrames: Map<String, PeerVideoFrame>,
    micLevel: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    // Pinned tile ID (stage view focus)
    var pinnedTileId by remember { mutableStateOf<String?>(null) }
    var layoutModeOverride by remember { mutableStateOf<String?>("AUTO") } // "GRID" or "STAGE"

    // Construct participant tile models
    val selfSpeaking = micLevel > 0.08f && !isMuted

    val tiles = remember(
        activeGroupCall.participants,
        remoteVideoFrames,
        userProfile,
        activeGroupCall.isCameraOff,
        activeGroupCall.isScreenSharing,
        isMuted,
        selfSpeaking
    ) {
        val list = mutableListOf<VideoTileModel>()

        // 1. Self Tile
        list.add(
            VideoTileModel(
                id = "self",
                isSelf = true,
                name = "أنت (${userProfile.displayName.ifBlank { userProfile.username }})",
                avatarColor = userProfile.avatarColor,
                avatarBase64 = userProfile.avatarBase64,
                isMuted = isMuted,
                isCameraOff = activeGroupCall.isCameraOff,
                isScreenSharing = activeGroupCall.isScreenSharing,
                screenShareAppName = activeGroupCall.screenSharedAppName,
                videoFrame = null,
                isSpeaking = selfSpeaking
            )
        )

        // 2. Remote Peers
        for (peer in activeGroupCall.participants) {
            val frame = remoteVideoFrames[peer.id]
            val isCamOff = frame?.isCameraOff ?: false
            val isSharing = frame?.isScreenShare ?: false

            list.add(
                VideoTileModel(
                    id = peer.id,
                    isSelf = false,
                    name = peer.name,
                    avatarColor = peer.avatarColor,
                    avatarBase64 = peer.avatarBase64,
                    isMuted = peer.isMuted,
                    isCameraOff = isCamOff,
                    isScreenSharing = isSharing,
                    screenShareAppName = frame?.appTitle,
                    videoFrame = frame,
                    isSpeaking = !peer.isMuted // active speaker indication
                )
            )
        }

        list
    }

    // Auto-focus on active screen-sharer if available
    LaunchedEffect(tiles) {
        val sharer = tiles.firstOrNull { it.isScreenSharing }
        if (sharer != null && pinnedTileId == null) {
            pinnedTileId = sharer.id
        } else if (pinnedTileId != null && tiles.none { it.id == pinnedTileId }) {
            pinnedTileId = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val gap = 8.dp

        // Compute geometry map for each tile
        val geometries = remember(tiles, pinnedTileId, containerWidth, containerHeight) {
            computeTileGeometries(
                tiles = tiles,
                pinnedId = pinnedTileId,
                totalWidth = containerWidth,
                totalHeight = containerHeight,
                gap = gap
            )
        }

        // Layout control overlay badge
        Box(modifier = Modifier.fillMaxSize()) {
            // Render each tile with animated position and size
            tiles.forEach { tile ->
                val targetGeo = geometries[tile.id] ?: TileGeometry(0.dp, 0.dp, containerWidth, containerHeight)

                AnimatedVideoTile(
                    tile = tile,
                    geometry = targetGeo,
                    videoEngine = videoEngine,
                    userProfile = userProfile,
                    micLevel = micLevel,
                    isPinned = pinnedTileId == tile.id,
                    onTileClick = {
                        pinnedTileId = if (pinnedTileId == tile.id) null else tile.id
                    }
                )
            }

            // Top Layout Switcher Pill & Participant Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pinnedTileId != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryPurple.copy(alpha = 0.85f),
                        modifier = Modifier.clickable { pinnedTileId = null }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "عرض الشبكة",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إلغاء التثبيت",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grid3x3,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${tiles.size} شاشات متزامنة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates responsive geometric bounding boxes for each participant based on room count and focus mode.
 */
private fun computeTileGeometries(
    tiles: List<VideoTileModel>,
    pinnedId: String?,
    totalWidth: Dp,
    totalHeight: Dp,
    gap: Dp
): Map<String, TileGeometry> {
    val map = mutableMapOf<String, TileGeometry>()
    val count = tiles.size

    if (count == 0) return map

    // --- Mode 1: Pinned / Stage Focus View ---
    if (pinnedId != null && tiles.any { it.id == pinnedId }) {
        val stageHeight = (totalHeight * 0.72f) - (gap / 2)
        val filmstripHeight = (totalHeight * 0.28f) - (gap / 2)
        val filmstripY = stageHeight + gap

        // Main Stage Tile
        map[pinnedId] = TileGeometry(
            x = 0.dp,
            y = 0.dp,
            width = totalWidth,
            height = stageHeight
        )

        // Remaining tiles in bottom filmstrip row
        val others = tiles.filter { it.id != pinnedId }
        if (others.isNotEmpty()) {
            val totalGaps = (others.size - 1) * gap.value
            val tileWidth = ((totalWidth.value - totalGaps) / others.size).coerceAtLeast(80f).dp

            others.forEachIndexed { index, peerTile ->
                val tileX = (tileWidth + gap) * index
                map[peerTile.id] = TileGeometry(
                    x = tileX,
                    y = filmstripY,
                    width = tileWidth,
                    height = filmstripHeight
                )
            }
        }
        return map
    }

    // --- Mode 2: Responsive Balanced Grid ---
    when (count) {
        1 -> {
            // Sole participant: centered generous tile
            map[tiles[0].id] = TileGeometry(
                x = 0.dp,
                y = 0.dp,
                width = totalWidth,
                height = totalHeight
            )
        }

        2 -> {
            // 2 Participants: Top / Bottom vertical split for portrait
            val halfH = (totalHeight - gap) / 2
            map[tiles[0].id] = TileGeometry(x = 0.dp, y = 0.dp, width = totalWidth, height = halfH)
            map[tiles[1].id] = TileGeometry(x = 0.dp, y = halfH + gap, width = totalWidth, height = halfH)
        }

        3 -> {
            // 3 Participants: Top tile full width, bottom row split 50/50
            val halfH = (totalHeight - gap) / 2
            val halfW = (totalWidth - gap) / 2

            map[tiles[0].id] = TileGeometry(x = 0.dp, y = 0.dp, width = totalWidth, height = halfH)
            map[tiles[1].id] = TileGeometry(x = 0.dp, y = halfH + gap, width = halfW, height = halfH)
            map[tiles[2].id] = TileGeometry(x = halfW + gap, y = halfH + gap, width = halfW, height = halfH)
        }

        4 -> {
            // 4 Participants: 2x2 Balanced Quad
            val halfH = (totalHeight - gap) / 2
            val halfW = (totalWidth - gap) / 2

            map[tiles[0].id] = TileGeometry(x = 0.dp, y = 0.dp, width = halfW, height = halfH)
            map[tiles[1].id] = TileGeometry(x = halfW + gap, y = 0.dp, width = halfW, height = halfH)
            map[tiles[2].id] = TileGeometry(x = 0.dp, y = halfH + gap, width = halfW, height = halfH)
            map[tiles[3].id] = TileGeometry(x = halfW + gap, y = halfH + gap, width = halfW, height = halfH)
        }

        5 -> {
            // 5 Participants: Top row 2 tiles, Bottom row 3 tiles
            val halfH = (totalHeight - gap) / 2
            val halfW = (totalWidth - gap) / 2
            val thirdW = (totalWidth - (gap * 2)) / 3

            map[tiles[0].id] = TileGeometry(x = 0.dp, y = 0.dp, width = halfW, height = halfH)
            map[tiles[1].id] = TileGeometry(x = halfW + gap, y = 0.dp, width = halfW, height = halfH)

            map[tiles[2].id] = TileGeometry(x = 0.dp, y = halfH + gap, width = thirdW, height = halfH)
            map[tiles[3].id] = TileGeometry(x = thirdW + gap, y = halfH + gap, width = thirdW, height = halfH)
            map[tiles[4].id] = TileGeometry(x = (thirdW * 2) + (gap * 2), y = halfH + gap, width = thirdW, height = halfH)
        }

        6 -> {
            // 6 Participants: 3 rows x 2 cols
            val rowH = (totalHeight - (gap * 2)) / 3
            val halfW = (totalWidth - gap) / 2

            for (i in 0 until 6) {
                val row = i / 2
                val col = i % 2
                val tileX = if (col == 0) 0.dp else halfW + gap
                val tileY = (rowH + gap) * row
                map[tiles[i].id] = TileGeometry(x = tileX, y = tileY, width = halfW, height = rowH)
            }
        }

        else -> {
            // 7 to 9+ Participants: 3x3 Grid
            val rowH = (totalHeight - (gap * 2)) / 3
            val colW = (totalWidth - (gap * 2)) / 3

            for (i in 0 until count.coerceAtMost(9)) {
                val row = i / 3
                val col = i % 3
                val tileX = (colW + gap) * col
                val tileY = (rowH + gap) * row
                map[tiles[i].id] = TileGeometry(x = tileX, y = tileY, width = colW, height = rowH)
            }
        }
    }

    return map
}

/**
 * Individual animated tile with spring-interpolated position, dimension, active speaker glowing border,
 * and tap-to-pin interaction.
 */
@Composable
private fun AnimatedVideoTile(
    tile: VideoTileModel,
    geometry: TileGeometry,
    videoEngine: VideoEngine,
    userProfile: UserProfile,
    micLevel: Float,
    isPinned: Boolean,
    onTileClick: () -> Unit
) {
    // Smoothly animate position (x, y) and size (width, height)
    val animSpec = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val animatedX by animateDpAsState(targetValue = geometry.x, animationSpec = animSpec, label = "tileX")
    val animatedY by animateDpAsState(targetValue = geometry.y, animationSpec = animSpec, label = "tileY")
    val animatedW by animateDpAsState(targetValue = geometry.width, animationSpec = animSpec, label = "tileW")
    val animatedH by animateDpAsState(targetValue = geometry.height, animationSpec = animSpec, label = "tileH")

    // Speaking glow border animation
    val speakingBorderColor = if (tile.isSpeaking) StatusGreen else Color.White.copy(alpha = 0.12f)
    val speakingBorderWidth = if (tile.isSpeaking) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .offset(x = animatedX, y = animatedY)
            .size(width = animatedW, height = animatedH)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(speakingBorderWidth, speakingBorderColor, RoundedCornerShape(16.dp))
            .clickable { onTileClick() }
            .testTag("video_tile_${tile.id}")
    ) {
        // Tile Video or Avatar Content
        if (tile.isSelf) {
            if (tile.isScreenSharing) {
                val localScreenBitmap by videoEngine.localScreenShareBitmap.collectAsState()
                if (localScreenBitmap != null && !localScreenBitmap!!.isRecycled) {
                    Image(
                        bitmap = localScreenBitmap!!.asImageBitmap(),
                        contentDescription = "Live Screen Broadcast",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ScreenShare,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "بث شاشتك المباشر",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            } else if (!tile.isCameraOff) {
                CameraPreviewSurface(
                    videoEngine = videoEngine,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SelfAvatarTileView(
                    userProfile = userProfile,
                    micLevel = micLevel,
                    isMuted = tile.isMuted
                )
            }
        } else {
            val frame = tile.videoFrame
            if (tile.isScreenSharing && frame?.bitmap != null && !frame.bitmap.isRecycled) {
                Image(
                    bitmap = frame.bitmap.asImageBitmap(),
                    contentDescription = "Screen Share from ${tile.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (frame?.bitmap != null && !tile.isCameraOff && !frame.bitmap.isRecycled) {
                Image(
                    bitmap = frame.bitmap.asImageBitmap(),
                    contentDescription = "Video from ${tile.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                PeerAvatarTileView(
                    name = tile.name,
                    avatarColor = tile.avatarColor,
                    avatarBase64 = tile.avatarBase64
                )
            }
        }

        // Screen Share Overlay Badge
        if (tile.isScreenSharing) {
            Surface(
                color = PrimaryPurple.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tile.screenShareAppName ?: "شاشة مشتركة",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pin Badge if Pinned
        if (isPinned) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "مثبت",
                    tint = StatusGreen,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(14.dp)
                )
            }
        }

        // Bottom User Name & Mute State Pill
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tile.isSpeaking) {
                    // Small live speaking indicator
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = tile.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (tile.isMuted) {
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
}

@Composable
private fun SelfAvatarTileView(
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
            val infiniteTransition = rememberInfiniteTransition(label = "speakPulse")
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
                    .size(76.dp)
                    .scale(waveScale)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.25f))
            )
        }

        Box(
            modifier = Modifier
                .size(62.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = userProfile.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PeerAvatarTileView(
    name: String,
    avatarColor: Long,
    avatarBase64: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarBase64.isNullOrEmpty()) {
                val decoded = remember(avatarBase64) {
                    try {
                        val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
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
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
