package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.model.UserProfile
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ThemeModeSettingsCard
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryTeal

@Composable
fun NetworkGuideScreen(
    userProfile: UserProfile,
    localIp: String,
    micLevel: Float,
    activePeersCount: Int,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onEditProfileClick: () -> Unit,
    onManualConnectClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // App Theme & Appearance Settings Card
        item {
            ThemeModeSettingsCard(
                currentMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange
            )
        }

        // User Profile Summary Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(userProfile.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = userProfile.username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "معرف الجهاز: #${userProfile.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "عنوانك المحلي: $localIp",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onEditProfileClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل")
                    }
                }
            }
        }

        // Offline Network Setup Guide Section
        item {
            Text(
                text = "دليل التوصيل المحلي بدون إنترنت 📡",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Method 1: Hotspot without internet
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الطريقة 1: نقطة اتصال الهاتف (Hotspot)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. قم بتشغيل 'نقطة الاتصال المحمولة' (Hotspot) على أحد الهواتف.\n2. اجعل الأجهزة الأخرى تتصل بهذه الشبكة عبر الـ Wi-Fi.\n3. افتح التطبيق في جميع الأجهزة، وسيتصل الجميع صوتياً ومرئياً وكتابياً فوراً بدون رصيد أو إنترنت!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Method 2: Local Wi-Fi Router
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SecondaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = SecondaryTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الطريقة 2: نفس راوتر الـ Wi-Fi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اتصل بنفس شبكة الراوتر المنزلي أو المكتبي. التطبيق يعمل حتى لو كانت خدمة الإنترنت مقطوعة عن الراوتر لأن الاتصال يتم محلياً بالكامل عبر بروتوكولات UDP و TCP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Diagnostic Info
        item {
            Text(
                text = "بيانات الشبكة والتشخيص المباشر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticRow(
                        title = "عنوان IP المحلي:",
                        value = localIp,
                        icon = Icons.Default.NetworkCheck,
                        iconTint = PrimaryCyan
                    )
                    DiagnosticRow(
                        title = "منفذ الاستكشاف والتحكم:",
                        value = "UDP 8888",
                        icon = Icons.Default.Security,
                        iconTint = SecondaryTeal
                    )
                    DiagnosticRow(
                        title = "منفذ البث الصوتي الفوري:",
                        value = "UDP 8889 (PCM 16kHz Low Latency)",
                        icon = Icons.Default.Mic,
                        iconTint = PrimaryCyan
                    )
                    DiagnosticRow(
                        title = "منفذ بث الفيديو المباشر:",
                        value = "UDP 8890 (H.264/JPEG Live Stream)",
                        icon = Icons.Default.Speed,
                        iconTint = SecondaryTeal
                    )
                    DiagnosticRow(
                        title = "الأجهزة المكتشفة النشطة:",
                        value = "$activePeersCount أجهزة",
                        icon = Icons.Default.CheckCircle,
                        iconTint = AccentGreen
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Microphone Live Test
                    Column {
                        Text(
                            text = "اختبار حساسية الميكروفون المباشر:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AudioWaveformVisualizer(
                            isSpeaking = micLevel > 0.03f,
                            audioLevel = micLevel,
                            barCount = 14,
                            maxHeight = 22.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
