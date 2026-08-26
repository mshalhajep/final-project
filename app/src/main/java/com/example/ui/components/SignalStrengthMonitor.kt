package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.StatusGreen

/**
 * Visual Signal Strength Monitor for Voice & Video calls displaying latency,
 * signal strength bars, jitter, and packet transmission quality.
 */
@Composable
fun CallSignalMonitor(
    signalInfo: PeerSignalInfo?,
    peerIp: String,
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    val info = signalInfo ?: PeerSignalInfo(
        peerId = "",
        latencyMs = 15L,
        jitterMs = 2L,
        packetLossPercent = 0,
        quality = ConnectionQualityLevel.EXCELLENT,
        bars = 4,
        bitrateKbps = if (isVideo) 960 else 128
    )

    var expandedDetails by remember { mutableStateOf(false) }

    val qualityColor by animateColorAsState(
        targetValue = when (info.quality) {
            ConnectionQualityLevel.EXCELLENT -> AccentGreen
            ConnectionQualityLevel.GOOD -> PrimaryCyan
            ConnectionQualityLevel.FAIR -> Color(0xFFF59E0B) // Amber
            ConnectionQualityLevel.POOR -> AccentRose
            ConnectionQualityLevel.DISCONNECTED -> Color.Gray
        },
        animationSpec = tween(300),
        label = "quality_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_signal")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, qualityColor.copy(alpha = 0.4f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { expandedDetails = !expandedDetails }
            .testTag("signal_strength_monitor")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Compact Header View
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Signal 4-Bars graphic
                SignalBarsGraphic(
                    bars = info.bars,
                    activeColor = qualityColor,
                    modifier = Modifier.height(14.dp)
                )

                // Latency Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(qualityColor.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${info.latencyMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Quality Label Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = qualityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = info.quality.labelAr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Expand/Collapse Icon indicator
                Icon(
                    imageVector = if (expandedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "تفاصيل الإشارة",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Expanded Telemetry Breakdown
            AnimatedVisibility(
                visible = expandedDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    SignalDetailRow(
                        label = "معدل الإطارات (FPS):",
                        value = String.format("%.1f FPS", info.fps),
                        valueColor = AccentGreen
                    )

                    SignalDetailRow(
                        label = "استقرار الإشارة (Jitter):",
                        value = "${info.jitterMs} ms",
                        valueColor = Color.White
                    )

                    SignalDetailRow(
                        label = "فقدان الحزم (Loss):",
                        value = "${info.packetLossPercent}%",
                        valueColor = if (info.packetLossPercent == 0) AccentGreen else AccentRose
                    )

                    SignalDetailRow(
                        label = "معدل النقل التقريبي:",
                        value = if (info.bitrateKbps >= 1000) "${info.bitrateKbps / 1000.0} Mbps" else "${info.bitrateKbps} kbps",
                        valueColor = PrimaryCyan
                    )

                    SignalDetailRow(
                        label = "عنوان IP النظير:",
                        value = peerIp,
                        valueColor = Color.White.copy(alpha = 0.8f)
                    )

                    SignalDetailRow(
                        label = "البروتوكول المحلي:",
                        value = "UDP P2P Direct Socket",
                        valueColor = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 4-Bar Signal Indicator Graphic.
 */
@Composable
fun SignalBarsGraphic(
    bars: Int,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedBars = bars.coerceIn(1, 4)
    val heights = listOf(4.dp, 7.dp, 10.dp, 14.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 4) {
            val isActive = i < clampedBars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heights[i])
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) activeColor else Color.White.copy(alpha = 0.2f))
            )
        }
    }
}

@Composable
private fun SignalDetailRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontSize = 10.sp
        )
    }
}
