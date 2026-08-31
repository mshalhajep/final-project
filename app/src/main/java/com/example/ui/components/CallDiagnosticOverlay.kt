package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionQualityLevel
import com.example.model.PeerSignalInfo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple

/**
 * Professional semi-transparent diagnostic overlay (HUD) for active calls.
 * Displays real-time frame rate (FPS), jitter, packet loss, RTT latency, and bitrate
 * with futuristic glassmorphic styling.
 */
@Composable
fun CallDiagnosticOverlay(
    signalInfo: PeerSignalInfo?,
    peerIp: String,
    modifier: Modifier = Modifier
) {
    val info = signalInfo ?: PeerSignalInfo(
        peerId = "",
        latencyMs = 12L,
        jitterMs = 2L,
        packetLossPercent = 0,
        fps = 30.0f,
        quality = ConnectionQualityLevel.EXCELLENT,
        bars = 4,
        bitrateKbps = 960
    )

    var isExpanded by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val fpsColor = if (info.fps >= 24f) AccentGreen else Color(0xFFF59E0B)
    val jitterColor = if (info.jitterMs <= 10L) AccentGreen else Color(0xFFF59E0B)
    val lossColor = if (info.packetLossPercent == 0) AccentGreen else AccentRose

    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(PrimaryCyan.copy(alpha = 0.6f), PrimaryPurple.copy(alpha = 0.4f), Color.Transparent)
            )
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag("call_diagnostic_overlay")
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Header / Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = pulseAlpha))
                    )
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "HUD تشخيص المكالمة الحي",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${info.fps.toInt()} FPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "طي/توسيع التشخيص",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Real-time Telemetry Metrics
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Metric 1: Frame Rate (FPS)
                    DiagnosticMetricRow(
                        label = "معدل الإطارات (FPS)",
                        value = String.format("%.1f FPS", info.fps),
                        indicatorColor = fpsColor,
                        description = if (info.fps >= 24f) "سلس وعالي الجودة" else "انخفاض مؤقت"
                    )

                    // Metric 2: Jitter (استقرار الشبكة)
                    DiagnosticMetricRow(
                        label = "استقرار الإشارة (Jitter)",
                        value = "${info.jitterMs} ms",
                        indicatorColor = jitterColor,
                        description = if (info.jitterMs <= 8L) "ممتاز (مستقر جداً)" else "تذبذب ملحوظ"
                    )

                    // Metric 3: Packet Loss (فقدان الحزم)
                    DiagnosticMetricRow(
                        label = "فقدان الحزم (Packet Loss)",
                        value = "${info.packetLossPercent}%",
                        indicatorColor = lossColor,
                        description = if (info.packetLossPercent == 0) "بدون فقدان (0%)" else "تحذير: حزم مفقودة"
                    )

                    // Metric 4: Latency RTT (زمن الانتقال)
                    DiagnosticMetricRow(
                        label = "زمن الاستجابة (RTT Latency)",
                        value = "${info.latencyMs} ms",
                        indicatorColor = PrimaryCyan,
                        description = "اتصال مباشر P2P"
                    )

                    // Metric 5: Bitrate & Protocol
                    DiagnosticMetricRow(
                        label = "تدفق البيانات (Bitrate)",
                        value = if (info.bitrateKbps >= 1000) "${String.format("%.1f", info.bitrateKbps / 1000.0)} Mbps" else "${info.bitrateKbps} kbps",
                        indicatorColor = PrimaryPurple,
                        description = "UDP Socket / Opus & H.264"
                    )

                    // Footer Peer IP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النظير: $peerIp",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "مباشر آمن",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGreen,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetricRow(
    label: String,
    value: String,
    indicatorColor: Color,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
