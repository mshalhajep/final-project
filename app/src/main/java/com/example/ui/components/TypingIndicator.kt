package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TypingPeer
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple

/**
 * Animated 3-dot typing wave indicator.
 */
@Composable
fun TypingDotsAnimation(
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
    dotColor: Color = PrimaryPurple,
    spacing: Dp = 3.5.dp
) {
    val transition = rememberInfiniteTransition(label = "typing_dots_transition")

    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.7f))
        )
    }
}

/**
 * A fluid, interactive typing pill bubble for the chat screen.
 */
@Composable
fun ChatTypingIndicator(
    typingPeers: List<TypingPeer>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = typingPeers.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(250)),
        modifier = modifier
    ) {
        if (typingPeers.isEmpty()) return@AnimatedVisibility

        val typingText = when (typingPeers.size) {
            1 -> "${typingPeers[0].peerName} يكتب الآن..."
            2 -> "${typingPeers[0].peerName} و ${typingPeers[1].peerName} يكتبان الآن..."
            else -> "${typingPeers[0].peerName} و ${typingPeers.size - 1} آخرين يكتبون الآن..."
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("chat_typing_indicator_bubble")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stack of avatar dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    typingPeers.take(3).forEach { peer ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(peer.peerColor))
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peer.peerName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Text
                Text(
                    text = typingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(2.dp))

                // Bouncing wave dots
                TypingDotsAnimation(
                    dotSize = 5.dp,
                    dotColor = PrimaryPurple,
                    spacing = 3.dp
                )
            }
        }
    }
}
