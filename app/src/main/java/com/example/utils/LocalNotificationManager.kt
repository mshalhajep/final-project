package com.example.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Navigation bus for notification taps: carries the chat target the user opened
 * from a message notification (retained until the UI consumes it).
 */
object NotificationNavigation {
    /** (targetId, isDirect) — latest tap wins. */
    val openChat = MutableStateFlow<Pair<String, Boolean>?>(null)
}

/**
 * Local notification system: heads-up message notifications with MessagingStyle threading
 * and maximum-priority incoming call notifications with Accept/Decline actions that work
 * while the app is closed or the device is locked.
 */
object LocalNotificationManager {

    private const val CHANNEL_MESSAGES = "localconnect_messages"
    private const val CHANNEL_CALLS = "localconnect_calls"

    /** In-memory thread history cache to stack multiple incoming messages per chat. */
    data class CachedMessage(val senderName: String, val text: String, val timestamp: Long)
    private val conversationHistory = ConcurrentHashMap<String, MutableList<CachedMessage>>()

    /** Set by the main activity lifecycle; gates notification emission. */
    @Volatile
    var isAppInForeground: Boolean = true
        private set

    fun setAppInForeground(foreground: Boolean) {
        isAppInForeground = foreground
    }

    fun ensureChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val messages = NotificationChannel(
            CHANNEL_MESSAGES,
            "الرسائل",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات الرسائل الفورية"
            enableVibration(true)
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
        }
        val calls = NotificationChannel(
            CHANNEL_CALLS,
            "المكالمات",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات المكالمات الواردة"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
            enableLights(true)
            lightColor = android.graphics.Color.GREEN
            setBypassDnd(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(messages)
        manager.createNotificationChannel(calls)
    }

    private fun contentIntent(context: Context, targetId: String, isDirect: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openTarget", targetId)
            putExtra("isDirect", isDirect)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            targetId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Heads-up message notification with WhatsApp/Telegram style MessagingStyle thread stacking.
     */
    fun showMessageNotification(
        context: Context,
        targetId: String,
        isDirect: Boolean,
        senderName: String,
        contentPreview: String
    ) {
        ensureChannels(context)

        val history = conversationHistory.getOrPut(targetId) { Collections.synchronizedList(mutableListOf()) }
        history.add(CachedMessage(senderName, contentPreview, System.currentTimeMillis()))
        if (history.size > 10) {
            history.removeAt(0)
        }
        
        if (conversationHistory.size > 50) {
            val oldestKey = conversationHistory.keys.first()
            conversationHistory.remove(oldestKey)
        }

        val userPerson = Person.Builder().setName("أنت").build()
        val senderPerson = Person.Builder().setName(senderName).build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
        if (!isDirect) {
            messagingStyle.conversationTitle = senderName
            messagingStyle.isGroupConversation = true
        } else {
            messagingStyle.isGroupConversation = false
        }

        synchronized(history) {
            for (msg in history) {
                messagingStyle.addMessage(msg.text, msg.timestamp, senderPerson)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(contentPreview)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent(context, targetId, isDirect))
            .build()

        notifySafely(context, targetId.hashCode(), notification)
    }

    /**
     * Maximum-priority incoming call notification with Accept / Decline actions
     * routed through [CallNotificationReceiver] — works from the lock screen.
     */
    fun showIncomingCallNotification(context: Context, callerName: String, callId: String, isVideo: Boolean) {
        ensureChannels(context)

        val acceptIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_ACCEPT
            putExtra("callId", callId)
        }
        val declineIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_DECLINE
            putExtra("callId", callId)
        }

        val acceptPending = PendingIntent.getBroadcast(
            context, callId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePending = PendingIntent.getBroadcast(
            context, callId.hashCode() + 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openTarget", "call")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            callId.hashCode() + 3,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isVideo) "مكالمة فيديو واردة" else "مكالمة صوتية واردة")
            .setContentText("$callerName يتصل بك عبر الشبكة المحلية")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(0, "قبول", acceptPending)
            .addAction(0, "رفض", declinePending)
            .build()

        notifySafely(context, CALL_NOTIFICATION_ID, notification)
    }

    fun cancelTargetNotification(context: Context, targetId: String) {
        try {
            conversationHistory.remove(targetId)
            NotificationManagerCompat.from(context).cancel(targetId.hashCode())
        } catch (_: SecurityException) {
        }
    }

    fun cancelMessageNotifications(context: Context) {
        try {
            conversationHistory.clear()
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: SecurityException) {
        }
    }

    fun cancelCallNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(CALL_NOTIFICATION_ID)
        } catch (_: SecurityException) {
        }
    }

    private fun notifySafely(context: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — notifications silently skipped
        }
    }

    private const val CALL_NOTIFICATION_ID = 9001
}

/**
 * Handles Accept/Decline actions of incoming call notifications. The handler is
 * wired by MainViewModel so actions operate on the live P2P engine state.
 */
class CallNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACCEPT = "com.example.localconnect.CALL_ACCEPT"
        const val ACTION_DECLINE = "com.example.localconnect.CALL_DECLINE"

        /** (accept: Boolean) — wired by MainViewModel. */
        @Volatile
        var handler: ((Boolean) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ACCEPT -> {
                handler?.invoke(true)
                try {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra("openTarget", "call")
                    }
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    android.util.Log.e("CallNotificationReceiver", "Failed to launch MainActivity on call accept", e)
                }
            }
            ACTION_DECLINE -> handler?.invoke(false)
        }
        LocalNotificationManager.cancelCallNotification(context)
    }
}
