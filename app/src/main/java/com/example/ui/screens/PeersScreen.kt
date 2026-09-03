package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Peer
import com.example.model.UserPresenceStatus
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.StatusGreen

@Composable
fun PeersScreen(
    peers: List<Peer>,
    localIp: String,
    isScanning: Boolean = false,
    scanProgress: Float = 0f,
    scannedIpCount: Int = 0,
    currentScanSubnet: String = "",
    lastScanTime: Long = 0L,
    onRescanClick: () -> Unit = {},
    onAudioCall: (Peer) -> Unit,
    onVideoCall: (Peer) -> Unit,
    onDirectChat: (Peer) -> Unit,
    onManualConnectClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // High-Tech Radar Sonar Visual Scan Card
        item {
            NetworkScanSonarCard(
                localIp = localIp,
                peersCount = peers.size,
                isScanning = isScanning,
                scanProgress = scanProgress,
                scannedIpCount = scannedIpCount,
                currentScanSubnet = currentScanSubnet,
                onRescanClick = onRescanClick
            )
        }

        // Section Title & Connection Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الأجهزة المكتشفة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (peers.isNotEmpty()) StatusGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${peers.size} متصل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (peers.isNotEmpty()) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onManualConnectClick,
                    modifier = Modifier.testTag("manual_connect_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال بـ IP يدوي", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Discovered Devices List or Empty Radar State
        if (peers.isEmpty()) {
            item {
                EmptyScanningStateCard(
                    localIp = localIp,
                    isScanning = isScanning,
                    onRescanClick = onRescanClick,
                    onManualConnectClick = onManualConnectClick
                )
            }
        } else {
            items(peers, key = { it.id }) { peer ->
                PeerDetailCard(
                    peer = peer,
                    onAudioCall = { onAudioCall(peer) },
                    onVideoCall = { onVideoCall(peer) },
                    onDirectChat = { onDirectChat(peer) }
                )
            }
        }

        // Network Diagnostics & Guidance Note
        item {
            NetworkConnectionTipsCard(localIp = localIp)
        }
    }
}

/**
 * Animated Sonar Visual Scan Card featuring interactive radar sweep, pulsing ripples, and live IP progress.
 */
@Composable
private fun NetworkScanSonarCard(
    localIp: String,
    peersCount: Int,
    isScanning: Boolean,
    scanProgress: Float,
    scannedIpCount: Int,
    currentScanSubnet: String,
    onRescanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_transition")

    // Radar rotating sweep angle (0° to 360°)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Pulsing outer ripple ring 1
    val ripple1Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    val ripple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1_alpha"
    )

    // Pulsing outer ripple ring 2 (staggered)
    val ripple2Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2_alpha"
    )

    // Rotating refresh icon animation
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = scanProgress,
        animationSpec = tween(150),
        label = "scan_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Sonar Visualizer + Scan Status & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar Sonar Canvas
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f

                        // Background circular tint
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.08f),
                            radius = maxRadius,
                            center = center
                        )

                        // Concentric grid rings
                        val ringColors = primaryColor.copy(alpha = 0.25f)
                        drawCircle(
                            color = ringColors,
                            radius = maxRadius * 0.35f,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = ringColors,
                            radius = maxRadius * 0.70f,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = ringColors,
                            radius = maxRadius * 0.98f,
                            center = center,
                            style = Stroke(width = 2f)
                        )

                        // Crosshair radar guides
                        drawLine(
                            color = primaryColor.copy(alpha = 0.2f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, size.height),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = primaryColor.copy(alpha = 0.2f),
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 1f
                        )

                        // Pulsing ripple wave rings
                        if (isScanning || peersCount > 0) {
                            drawCircle(
                                color = primaryColor.copy(alpha = ripple1Alpha * 0.5f),
                                radius = maxRadius * ripple1Scale,
                                center = center,
                                style = Stroke(width = 2f)
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = ripple2Alpha * 0.5f),
                                radius = maxRadius * ripple2Scale,
                                center = center,
                                style = Stroke(width = 2f)
                            )
                        }

                        // Rotating radar sweep beam (sweeping conical fan effect)
                        val sweepRad = Math.toRadians(sweepAngle.toDouble())
                        val lineEnd = Offset(
                            x = (center.x + maxRadius * Math.cos(sweepRad)).toFloat(),
                            y = (center.y + maxRadius * Math.sin(sweepRad)).toFloat()
                        )

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.1f)),
                                start = center,
                                end = lineEnd
                            ),
                            start = center,
                            end = lineEnd,
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )

                        // Glowing center beacon dot
                        drawCircle(
                            color = if (isScanning) PrimaryPurpleLight else StatusGreen,
                            radius = 5.5f,
                            center = center
                        )
                    }

                    // Central beacon icon overlay
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Radar else Icons.Default.Sensors,
                            contentDescription = "Radar Beacon",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Status Description & Rescan Trigger
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isScanning) PrimaryPurple else StatusGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "جاري مسح الشبكة..." else "مسح الشبكة نشط",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isScanning) primaryColor else StatusGreen
                            )
                        }

                        // Rescan Button
                        FilledTonalIconButton(
                            onClick = onRescanClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("rescan_network_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة فحص الشبكة",
                                tint = primaryColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .then(if (isScanning) Modifier.rotate(refreshRotation) else Modifier)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "عنوان جهازك: $localIp",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (currentScanSubnet.isNotBlank()) "النطاق: $currentScanSubnet" else "البحث عبر UDP Broadcast & Multicast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Scanning Live Progress Bar & Counter
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فحص عناوين IP في النطاق المحلي ($scannedIpCount/254)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = primaryColor.copy(alpha = 0.2f)
                    )
                }
            }

            // Quick Status Chips
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanInfoPill(
                    icon = Icons.Default.Wifi,
                    label = "منفذ 8888 P2P",
                    modifier = Modifier.weight(1f)
                )
                ScanInfoPill(
                    icon = Icons.Default.WifiTethering,
                    label = if (peersCount > 0) "اكتشاف $peersCount جهاز" else "في انتظار الأجهزة",
                    modifier = Modifier.weight(1f),
                    tint = if (peersCount > 0) StatusGreen else primaryColor
                )
            }
        }
    }
}

@Composable
private fun ScanInfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Empty Scanning State Card providing active visual feedback and step-by-step connection clarity.
 */
@Composable
private fun EmptyScanningStateCard(
    localIp: String,
    isScanning: Boolean,
    onRescanClick: () -> Unit,
    onManualConnectClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (isScanning) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Radar else Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isScanning) "جاري البحث عن أجهزة قريبة على الـ Wi-Fi..." else "لم يتم العثور على أجهزة بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تأكد من فتح التطبيق على الهواتف الأخرى واتصالها بنفس شبكة الـ Wi-Fi أو نقطة الاتصال (Hotspot).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRescanClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة الفحص", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onManualConnectClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدخال IP مباشر", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Peer Card showing discovered device with active signal badge and call/chat actions.
 */
@Composable
private fun PeerDetailCard(
    peer: Peer,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onDirectChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with presence status dot
                val avatarBmpState = androidx.compose.runtime.remember(peer.avatarBase64) { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }
                val avatarBmp = avatarBmpState.value
                androidx.compose.runtime.LaunchedEffect(peer.avatarBase64) {
                    if (!peer.avatarBase64.isNullOrBlank()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val bytes = Base64.decode(peer.avatarBase64, Base64.DEFAULT)
                                avatarBmpState.value = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Throwable) { /* ignore */ }
                        }
                    }
                }

                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(peer.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBmp != null) {
                            Image(
                                bitmap = avatarBmp.asImageBitmap(),
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
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    UserStatusDot(
                        status = peer.userStatus,
                        size = 14.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = peer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        UserStatusBadge(status = peer.userStatus, compact = true)
                    }

                    if (peer.statusMessage.isNotBlank()) {
                        Text(
                            text = "\"${peer.statusMessage}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "IP: ${peer.ip} • ${if (peer.deviceModel.isNotBlank()) peer.deviceModel else "هاتف أندرويد"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "متواجد في: ${if (peer.currentRoom == "general") "الغرفة العامة" else peer.currentRoom}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio Call
                FilledTonalButton(
                    onClick = onAudioCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("صوت", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }

                // Video Call
                Button(
                    onClick = onVideoCall,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondarySlate),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فيديو", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }

                // Direct Chat
                Button(
                    onClick = onDirectChat,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("محادثة", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Diagnostic Tips Card for Wi-Fi connection clarity.
 */
@Composable
private fun NetworkConnectionTipsCard(localIp: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "نصائح لضمان الاكتشاف السريع:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• إذا كنت بدون راوتر Wi-Fi، شغّل نقطة اتصال (Hotspot) على أحد الهواتف واجعل بقية الأجهزة تتصل بها.\n• لا يلزم وجود اتصال بالإنترنت أو شريحة SIM.\n• يتم مسح النطاق بالكامل دورياً لإعادة اكتشاف الأجهزة عند تغيير الشبكة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
