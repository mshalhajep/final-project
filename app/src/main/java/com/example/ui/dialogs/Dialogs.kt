package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.model.Peer
import com.example.model.RoomInfo
import com.example.model.RoomInvitation
import com.example.model.UserProfile
import com.example.model.UserPresenceStatus
import com.example.ui.components.CompactThemeModeSelector
import com.example.ui.components.UserStatusBadge
import com.example.ui.components.UserStatusDot
import com.example.ui.components.UserStatusSelector
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PeerColors
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileDialog(
    userProfile: UserProfile,
    localIp: String,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveProfile: (displayName: String, color: Long, statusMessage: String, bio: String, userStatus: UserPresenceStatus) -> Unit,
    onLogout: () -> Unit
) {
    var displayName by remember { mutableStateOf(userProfile.displayName.ifBlank { userProfile.username }) }
    var statusMessage by remember { mutableStateOf(userProfile.statusMessage) }
    var bio by remember { mutableStateOf(userProfile.bio) }
    var selectedColor by remember { mutableLongStateOf(userProfile.avatarColor) }
    var selectedStatus by remember { mutableStateOf(userProfile.userStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text(text = "الملف الشخصي والحساب", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User ID and Username badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(selectedColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.username.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            UserStatusDot(
                                status = selectedStatus,
                                size = 14.dp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "@${userProfile.username}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                UserStatusBadge(status = selectedStatus, compact = true)
                            }
                            Text(
                                text = "عنوان الجهاز: $localIp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Presence Status Selector
                UserStatusSelector(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it }
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم المعروض") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )

                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("الحالة الشخصية") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_status_input")
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("نبذة مختصرة") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "لون الصورة الرمزية:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PeerColors.forEach { colorValue ->
                        val isSelected = selectedColor == colorValue
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .clickable { selectedColor = colorValue }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // App Theme Selector (Persisted in DataStore)
                CompactThemeModeSelector(
                    currentMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange
                )

                // Logout action
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج / تبديل الحساب")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(displayName.trim(), selectedColor, statusMessage.trim(), bio.trim(), selectedStatus)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التغييرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onCreateRoom: (name: String, description: String, capacity: Int) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableIntStateOf(8) }

    val capacityOptions = listOf(4, 8, 12, 16, 20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text("إنشاء غرفة تواصل جديدة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("اسم الغرفة") },
                    placeholder = { Text("مثال: غرفة النقاش الصوتي") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الغرفة أو الغرض منها") },
                    placeholder = { Text("مثال: دردشة ومكالمات صوتية مباشرة") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_desc_input")
                )

                Text(
                    text = "سعة الغرفة (الحد الأقصى للمستخدمين: $capacity أعضاء):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    capacityOptions.forEach { count ->
                        FilterChip(
                            selected = capacity == count,
                            onClick = { capacity = count },
                            label = { Text("$count") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomName.isNotBlank()) {
                        onCreateRoom(roomName.trim(), description.trim(), capacity)
                        onDismiss()
                    }
                },
                enabled = roomName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_room_submit_button")
            ) {
                Text("إنشاء وانضمام")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun InvitePeersDialog(
    room: RoomInfo,
    discoveredPeers: List<Peer>,
    onDismiss: () -> Unit,
    onInvitePeer: (peer: Peer) -> Unit
) {
    var invitedPeerIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text("دعوة أعضاء إلى: ${room.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر الأجهزة المتصلة على الشبكة المحلية لإرسال دعوة انضمام فورية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (discoveredPeers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد أجهزة متصلة حالياً على نفس شبكة Wi-Fi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredPeers) { peer ->
                            val isInvited = invitedPeerIds.contains(peer.id)
                            val isAlreadyInRoom = peer.currentRoom == room.id

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(peer.avatarColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = peer.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (isAlreadyInRoom) "موجود في هذه الغرفة" else peer.ip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAlreadyInRoom) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isAlreadyInRoom) {
                                        Text(
                                            text = "عضو حالي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (isInvited) {
                                        Text(
                                            text = "تمت الدعوة ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                invitedPeerIds = invitedPeerIds + peer.id
                                                onInvitePeer(peer)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("دعوة", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun IncomingRoomInviteDialog(
    invitation: RoomInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "دعوة انضمام إلى غرفة",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "قام ${invitation.inviterName} بدعوتك للانضمام إلى الغرفة التالية:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = invitation.roomName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryPurple
                        )
                        if (invitation.roomDescription.isNotBlank()) {
                            Text(
                                text = invitation.roomDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "السعة القصوى: ${invitation.maxCapacity} مستخدمين",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("قبول وانضمام للغرفة")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تجاهل")
            }
        }
    )
}

@Composable
fun ManualIpDialog(
    onDismiss: () -> Unit,
    onConnect: (ip: String) -> Unit
) {
    var ip by remember { mutableStateOf("192.168.1.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Text(text = "اتصال يدوي بعنوان IP", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "إذا لم يظهر الجهاز الآخر تلقائياً، أدخل عنوان الـ IP الخاص به للاتصال المباشر:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("عنوان IP للجهاز الهدف") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_ip_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ip.isNotBlank()) {
                        onConnect(ip.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إرسال اتصال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
