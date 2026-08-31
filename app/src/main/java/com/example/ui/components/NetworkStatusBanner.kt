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
