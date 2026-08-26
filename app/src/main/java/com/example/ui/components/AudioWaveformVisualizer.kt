package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryTeal

@Composable
fun AudioWaveformVisualizer(
    isSpeaking: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    maxHeight: Dp = 32.dp,
    activeColor: Color = SecondaryTeal,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animDuration = 250 + (i * 70)
            val scaleAnim by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val currentHeight = if (isSpeaking) {
                val factor = ((audioLevel * 1.5f + scaleAnim * 0.5f)).coerceIn(0.2f, 1.0f)
                maxHeight * factor
            } else {
                maxHeight * 0.15f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(currentHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isSpeaking) activeColor else inactiveColor)
            )
        }
    }
}
