package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeMode
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple

/**
 * Settings Card for Theme selection with Light/Dark toggle and System Default support.
 * Persisted persistently via Jetpack DataStore.
 */
@Composable
fun ThemeModeSettingsCard(
    currentMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemInDark = isSystemInDarkTheme()
    val isEffectivelyDark = when (currentMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_settings_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Main Switch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEffectivelyDark) PrimaryPurple.copy(alpha = 0.2f)
                                else PrimaryCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEffectivelyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isEffectivelyDark) PrimaryPurple else PrimaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "مظهر التطبيق (الوضع الليلي)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentMode.titleAr + " • " + if (isEffectivelyDark) "الوضع الداكن مفعّل" else "الوضع الفاتح مفعّل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Toggle Switch
                Switch(
                    checked = isEffectivelyDark,
                    onCheckedChange = { checked ->
                        onThemeModeChange(if (checked) ThemeMode.DARK else ThemeMode.LIGHT)
                    },
                    thumbContent = {
                        Icon(
                            imageVector = if (isEffectivelyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryPurple,
                        checkedTrackColor = PrimaryPurple.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.testTag("theme_mode_switch")
                )
            }

            // 3-Segment Theme Mode Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionSegment(
                    title = "تلقائي",
                    subtitle = "حسب النظام",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                    testTag = "theme_option_system"
                )

                ThemeOptionSegment(
                    title = "فاتح",
                    subtitle = "نهاري",
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                    testTag = "theme_option_light"
                )

                ThemeOptionSegment(
                    title = "داكن",
                    subtitle = "ليلي",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                    testTag = "theme_option_dark"
                )
            }

            // Persistence info footnote
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "يتم حفظ تفضيل المظهر تلقائياً في ذاكرة DataStore المحلية",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionSegment(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "border_color"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "bg_color"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = animatedBgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, animatedBorderColor),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact Theme Mode Switcher for dialogs and profile sheets.
 */
@Composable
fun CompactThemeModeSelector(
    currentMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemInDark = isSystemInDarkTheme()
    val isEffectivelyDark = when (currentMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isEffectivelyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "مظهر التطبيق:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = isEffectivelyDark,
                    onCheckedChange = { checked ->
                        onThemeModeChange(if (checked) ThemeMode.DARK else ThemeMode.LIGHT)
                    },
                    modifier = Modifier.testTag("dialog_theme_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeOptionSegment(
                    title = "تلقائي",
                    subtitle = "النظام",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionSegment(
                    title = "فاتح",
                    subtitle = "نهار",
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionSegment(
                    title = "داكن",
                    subtitle = "ليل",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
