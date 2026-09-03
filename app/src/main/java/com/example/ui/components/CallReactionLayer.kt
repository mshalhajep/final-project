package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallReactionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** One emoji currently floating upward on the call screen. */
data class FloatingReaction(
    val event: CallReactionEvent,
    val startTimeMs: Long
)

private const val REACTION_DURATION_MS = 2600

/**
 * Collects newly arrived [reactions] into a mutable floating list, and drives a
 * frame ticker that recomposes the overlay while reactions are in flight.
 * Time-based progress (instead of Animatable jobs) keeps animations immune to
 * effect restarts when the reactions list identity changes.
 */
@Composable
fun rememberFloatingReactions(reactions: List<CallReactionEvent>): List<FloatingReaction> {
    val floating = remember { mutableStateListOf<FloatingReaction>() }
    val seenIds = remember { mutableSetOf<Long>() }

    LaunchedEffect(reactions.size) {
        reactions.filter { it.id !in seenIds }.forEach { event ->
            seenIds.add(event.id)
            // Cap concurrent floating emojis: a reaction flood must not pile up
            while (floating.size >= 15) {
                val oldest = floating.minByOrNull { it.startTimeMs }
                if (oldest != null) floating.remove(oldest) else break
            }
            floating.add(FloatingReaction(event, System.currentTimeMillis()))
        }
        if (floating.isNotEmpty()) {
            floating.removeAll { System.currentTimeMillis() - it.startTimeMs > REACTION_DURATION_MS }
        }
    }

    // Frame ticker: recomposes the overlay ~60fps while any reaction is animating
    LaunchedEffect(Unit) {
        while (isActive) {
            if (floating.isNotEmpty()) {
                withFrameNanos { }
                floating.removeAll { System.currentTimeMillis() - it.startTimeMs > REACTION_DURATION_MS }
            } else {
                delay(120)
            }
        }
    }

    return floating
}

/**
 * Renders floating emojis rising from above the control dock with a wavy
 * horizontal drift, growing scale and a fading tail (Meet/Instagram style).
 */
@Composable
fun BoxScope.CallReactionOverlay(
    floatingReactions: List<FloatingReaction>,
    modifier: Modifier = Modifier
) {
    floatingReactions.forEach { reaction ->
        val progress = ((System.currentTimeMillis() - reaction.startTimeMs).toFloat() / REACTION_DURATION_MS)
            .coerceIn(0f, 1f)
        if (progress >= 1f) return@forEach

        val seed = (reaction.event.id % 11L).toInt()
        val horizontalDrift = ((seed - 5) * 13).dp * progress
        val alpha = if (progress > 0.72f) ((1f - progress) / 0.28f).coerceIn(0f, 1f) else 1f
        val scale = 0.8f + 0.5f * progress

        Text(
            text = reaction.event.emoji,
            fontSize = (30 + (seed % 3) * 6).sp,
            modifier = modifier
                .align(Alignment.BottomCenter)
                .offset(x = horizontalDrift, y = -(progress * 340).dp)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                    rotationZ = ((seed - 5) * 2.5f) * progress
                }
        )
    }
}

/** Quick-reaction emoji strip shown above the call control dock. */
@Composable
fun CallEmojiPickerRow(
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("❤️", "👍", "🔥", "😂", "👏").forEach { emoji ->
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onSendReaction(emoji) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}
