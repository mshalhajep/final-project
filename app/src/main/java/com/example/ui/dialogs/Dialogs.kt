package com.example.ui.dialogs

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.heightIn
import com.example.data.local.BlockedPeerEntity
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.layout.fillMaxSize
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileDialog(
    userProfile: UserProfile,
    localIp: String,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDismiss: () -> Unit,
    blockedPeersCount: Int = 0,
    onOpenBlockedPeers: () -> Unit = {},
    onSaveProfile: (displayName: String, color: Long, statusMessage: String, bio: String, userStatus: UserPresenceStatus, avatarUri: String?) -> Unit,
    onLogout: () -> Unit
) {
    var displayName by remember { mutableStateOf(userProfile.displayName.ifBlank { userProfile.username }) }
    var statusMessage by remember { mutableStateOf(userProfile.statusMessage) }
    var bio by remember { mutableStateOf(userProfile.bio) }
    var selectedColor by remember { mutableLongStateOf(userProfile.avatarColor) }
    var selectedStatus by remember { mutableStateOf(userProfile.userStatus) }
    var selectedAvatarUri by remember { mutableStateOf(userProfile.avatarUri) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri.toString()
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAvatarUri != null) {
                                AsyncImage(
                                    model = selectedAvatarUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = userProfile.username.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            UserStatusDot(
                                status = selectedStatus,
                                size = 12.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
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
                            if (selectedAvatarUri != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                TextButton(
                                    onClick = { selectedAvatarUri = null },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("حذف الصورة الشخصية", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
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

                // Blocked Peers list button (S-02)
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenBlockedPeers()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = AccentRose,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("قائمة الأجهزة المحظورة ($blockedPeersCount)")
                }

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
                    onSaveProfile(displayName.trim(), selectedColor, statusMessage.trim(), bio.trim(), selectedStatus, selectedAvatarUri)
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
    onCreateRoom: (name: String, description: String, capacity: Int, password: String?) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableIntStateOf(8) }
    var isPasswordProtected by remember { mutableStateOf(false) }
    var roomPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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

                // Password Protection Toggle (S-01)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPasswordProtected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "حماية الغرفة برمز سري",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = isPasswordProtected,
                            onCheckedChange = { isPasswordProtected = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryPurple
                            )
                        )
                    }
                }

                AnimatedVisibility(visible = isPasswordProtected) {
                    OutlinedTextField(
                        value = roomPassword,
                        onValueChange = { roomPassword = it },
                        label = { Text("رمز مرور الغرفة (PIN أو كلمة سر)") },
                        placeholder = { Text("أدخل 4 خانات على الأقل") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val canSubmit = roomName.isNotBlank() && (!isPasswordProtected || roomPassword.trim().length >= 4)
            Button(
                onClick = {
                    if (canSubmit) {
                        onCreateRoom(
                            roomName.trim(),
                            description.trim(),
                            capacity,
                            if (isPasswordProtected) roomPassword.trim() else null
                        )
                        onDismiss()
                    }
                },
                enabled = canSubmit,
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

/**
 * Dialog prompting for password to join a protected room (S-01).
 */
@Composable
fun EnterRoomPasswordDialog(
    room: RoomInfo,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("غرفة محمية: ${room.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "هذه الغرفة خاصة وتتطلب إدخال كلمة المرور للانضمام والمراسلة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة مرور الغرفة") },
                    placeholder = { Text("أدخل كلمة المرور") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password.isNotBlank()) onJoin(password)
                        }
                    ),
                    isError = !errorMessage.isNullOrBlank(),
                    supportingText = {
                        if (!errorMessage.isNullOrBlank()) {
                            Text(errorMessage, color = AccentRose)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(password) },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("انضمام")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Dialog displaying and managing the blacklist of blocked peers (S-02).
 */
@Composable
fun BlockedPeersDialog(
    blockedPeers: List<BlockedPeerEntity>,
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = AccentRose,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("قائمة الأجهزة المحظورة", fontWeight = FontWeight.Bold)
        },
        text = {
            if (blockedPeers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أجهزة محظورة حالياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedPeers, key = { it.peerId }) { peer ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peer.peerName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "المعرف: ${peer.peerId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onUnblock(peer.peerId) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("إلغاء الحظر", style = MaterialTheme.typography.labelSmall)
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
