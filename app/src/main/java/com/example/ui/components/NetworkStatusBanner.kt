package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NetworkStatusState
import com.example.ui.theme.AccentRose

/**
 * Reusable banner component that detects drops or instability in the local Wi-Fi network connection
 * and displays a clear, accessible offline warning message with a quick rescan button.
 */
@Composable
fun NetworkStatusBanner(
    networkStatus: NetworkStatusState,
    onRescanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = networkStatus != NetworkStatusState.Connected,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier.testTag("network_status_banner_container")
    ) {
        val isDisconnected = networkStatus is NetworkStatusState.Disconnected
        val bannerText = when (val s = networkStatus) {
            is NetworkStatusState.Disconnected -> "⚠️ تم قطع الاتصال بالشبكة المحلية Wi-Fi. أنت حالياً غير متصل."
            is NetworkStatusState.Unstable -> "⚠️ ${s.reason} - جودة اتصال منخفضة."
            else -> ""
        }

        Surface(
            color = if (isDisconnected) AccentRose.copy(alpha = 0.95f) else Color(0xFFF59E0B).copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isDisconnected) Icons.Default.WifiOff else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("network_status_banner_text")
                )
                TextButton(
                    onClick = onRescanClick,
                    modifier = Modifier.testTag("network_status_banner_rescan_button")
                ) {
                    Text("إعادة فحص", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Smart Warning Banner displayed when Mobile Data is active while hosting a Hotspot,
 * alerting the user to turn off mobile data so connected devices do not consume their internet plan.
 */
@Composable
fun HotspotDataSaverBanner(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier.testTag("hotspot_data_saver_banner")
    ) {
        Surface(
            color = Color(0xFFD97706), // Amber-600 warning
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "⚠️ تنبيه: نقطة البث وبيانات الهاتف مفعّلة معاً!",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "نظام أندرويد يشارك إنترنت الشريحة مع الهواتف المتصلة، مما قد يستهلك باقتك بسرعة في تحديثات هواتفهم بالخلفية. يُنصح بإيقاف 'بيانات الهاتف' لضمان عمل التطبيق محلياً وبشكل مجاني 100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            try {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF92400E)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("إعدادات البيانات والشبكة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
