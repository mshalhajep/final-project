package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One row of the chat list: either a WhatsApp-style day divider or a message bubble. */
sealed class ChatListItem {
    abstract val key: String

    data class DateHeader(override val key: String, val text: String) : ChatListItem()
    data class Message(override val key: String, val message: ChatMessage) : ChatListItem()
}

private fun dayOfYearKey(timestamp: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(then: Calendar, now: Calendar): Boolean {
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

/** "اليوم" / "أمس" / "d MMMM yyyy" exactly like WhatsApp. */
fun chatDateHeaderText(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) -> "اليوم"
        isYesterday(then, now) -> "أمس"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Builds the flat chat rows, inserting a centered date chip whenever the calendar
 * day changes between consecutive messages.
 */
fun buildChatListItems(messages: List<ChatMessage>): List<ChatListItem> {
    val items = mutableListOf<ChatListItem>()
    var lastDay = Int.MIN_VALUE
    for (message in messages) {
        val day = dayOfYearKey(message.timestamp)
        if (day != lastDay) {
            lastDay = day
            val text = chatDateHeaderText(message.timestamp)
            items.add(ChatListItem.DateHeader(key = "date_header_$text", text = text))
        }
        items.add(ChatListItem.Message(key = message.id, message = message))
    }
    return items
}

/** Centered translucent date chip ("اليوم" / "أمس" / full date). */
@Composable
fun DateHeaderChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
