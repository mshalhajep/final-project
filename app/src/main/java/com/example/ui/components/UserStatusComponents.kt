package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserPresenceStatus
import com.example.ui.theme.AccentRose
import com.example.ui.theme.StatusGreen

/**
 * Returns theme color matching the presence status.
 */
@Composable
fun userStatusColor(status: UserPresenceStatus): Color {
    return when (status) {
        UserPresenceStatus.ONLINE -> StatusGreen
        UserPresenceStatus.BUSY -> AccentRose
        UserPresenceStatus.AWAY -> Color(0xFFF59E0B) // Amber
    }
}

/**
 * Returns icon matching presence status.
 */
fun userStatusIcon(status: UserPresenceStatus): ImageVector {
    return when (status) {
        UserPresenceStatus.ONLINE -> Icons.Default.CheckCircle
        UserPresenceStatus.BUSY -> Icons.Default.DoNotDisturb
        UserPresenceStatus.AWAY -> Icons.Default.AccessTime
    }
}

/**
 * Returns a descriptive status helper text in Arabic.
 */
fun userStatusDescription(status: UserPresenceStatus): String {
    return when (status) {
        UserPresenceStatus.ONLINE -> "متاح للمحادثة والمكالمات"
        UserPresenceStatus.BUSY -> "مشغول - الرجاء عدم الإزعاج"
        UserPresenceStatus.AWAY -> "غير متواجد عند الجهاز حالياً"
    }
}

/**
 * Standalone circular presence indicator dot with optional outer pulse ring.
 */
@Composable
fun UserStatusDot(
    status: UserPresenceStatus,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    showBorder: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    enablePulse: Boolean = true
) {
    val statusColor = userStatusColor(status)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing wave for Online status
        if (enablePulse && status == UserPresenceStatus.ONLINE) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = pulseAlpha))
            )
        }

        // Inner solid dot with optional border
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(statusColor)
                .then(
                    if (showBorder) {
                        Modifier.border(1.5.dp, borderColor, CircleShape)
                    } else Modifier
                )
        )
    }
}

/**
 * Visual pill badge displaying status dot + Arabic status label.
 */
@Composable
fun UserStatusBadge(
    status: UserPresenceStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val statusColor = userStatusColor(status)

    Surface(
        color = statusColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            )
        ) {
            UserStatusDot(
                status = status,
                size = if (compact) 7.dp else 8.dp,
                showBorder = false,
                enablePulse = false
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = status.labelAr,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontSize = if (compact) 10.sp else 11.sp
            )
        }
    }
}

/**
 * Interactive Status Selector component for dialogs or profile configuration.
 */
@Composable
fun UserStatusSelector(
    selectedStatus: UserPresenceStatus,
    onStatusSelected: (UserPresenceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "حالة التواجد على الشبكة:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        UserPresenceStatus.values().forEach { status ->
            val isSelected = selectedStatus == status
            val statusColor = userStatusColor(status)
            val icon = userStatusIcon(status)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) statusColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, statusColor) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onStatusSelected(status) }
                    .testTag("status_option_${status.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.labelAr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userStatusDescription(status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top App Bar Quick User Status Dropdown Pill.
 * Allows the user to change their live status in 1 tap without opening full profile dialog.
 */
@Composable
fun UserStatusQuickDropdown(
    currentStatus: UserPresenceStatus,
    onStatusSelected: (UserPresenceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = userStatusColor(currentStatus)

    Box(modifier = modifier) {
        Surface(
            color = statusColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .testTag("quick_status_dropdown")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                UserStatusDot(
                    status = currentStatus,
                    size = 8.dp,
                    showBorder = false,
                    enablePulse = currentStatus == UserPresenceStatus.ONLINE
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = currentStatus.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Change status",
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UserPresenceStatus.values().forEach { status ->
                val isSelected = currentStatus == status
                val itemColor = userStatusColor(status)
                val icon = userStatusIcon(status)

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = itemColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = status.labelAr,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userStatusDescription(status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    },
                    modifier = Modifier.testTag("dropdown_status_${status.name.lowercase()}")
                )
            }
        }
    }
}
