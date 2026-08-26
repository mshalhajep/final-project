package com.example.model

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Real-time user presence status for offline local network sync.
 */
enum class UserPresenceStatus(val labelAr: String, val labelEn: String, val colorHex: Long) {
    ONLINE("متصل", "Online", 0xFF22C55E),
    BUSY("مشغول", "Busy", 0xFFEF4444),
    AWAY("بالخارج", "Away", 0xFFF59E0B)
}

/**
 * Information about a peer discovered on the local offline network.
 */
data class Peer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 8888,
    val avatarColor: Long = 0xFF0EA5E9,
    val userStatus: UserPresenceStatus = UserPresenceStatus.ONLINE,
    val statusMessage: String = "",
    val currentRoom: String = "general",
    val isSpeaking: Boolean = false,
    val isVideoActive: Boolean = false,
    val isMuted: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val deviceModel: String = ""
)

/**
 * Room data model for audio/video voice channels and in-room chat.
 */
data class RoomInfo(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val iconName: String = "group",
    val creatorId: String = "system",
    val creatorName: String = "النظام",
    val maxCapacity: Int = 10,
    val isPrivate: Boolean = false
)

/**
 * Invitation to join a specific room.
 */
data class RoomInvitation(
    val roomId: String,
    val roomName: String,
    val roomDescription: String = "",
    val inviterId: String,
    val inviterName: String,
    val inviterColor: Long = 0xFF6750A4,
    val maxCapacity: Int = 10,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time typing state of a peer in a room or direct chat over local network.
 */
data class TypingPeer(
    val peerId: String,
    val peerName: String,
    val peerColor: Long = 0xFF0EA5E9,
    val roomId: String? = null,
    val isDirect: Boolean = false,
    val targetPeerId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Message types for local offline communication.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    VOICE_NOTE,
    VOICE_STATUS,
    SYSTEM,
    ROOM_INVITE
}

/**
 * Chat message model.
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderColor: Long,
    val targetRoomOrPeerId: String,
    val isDirect: Boolean = false,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val imageBase64: String? = null,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val durationSeconds: Int = 0,
    val senderIp: String? = null,
    val localFilePath: String? = null,
    val downloadProgress: Float = 0f,
    val isDownloaded: Boolean = false
)

/**
 * State for 1-to-1 or group calling.
 */
enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED
}

/**
 * Quality levels for peer-to-peer Wi-Fi signal monitor.
 */
enum class ConnectionQualityLevel(val labelAr: String, val labelEn: String) {
    EXCELLENT("ممتازة", "Excellent"),
    GOOD("جيدة", "Good"),
    FAIR("متوسطة", "Fair"),
    POOR("ضعيفة", "Poor"),
    DISCONNECTED("منقطعة", "Disconnected")
}

/**
 * Real-time connectivity and latency telemetry between local peer devices.
 */
data class PeerSignalInfo(
    val peerId: String,
    val latencyMs: Long = 12L,
    val jitterMs: Long = 2L,
    val packetLossPercent: Int = 0,
    val fps: Float = 24.0f,
    val quality: ConnectionQualityLevel = ConnectionQualityLevel.EXCELLENT,
    val bars: Int = 4, // 1 to 4
    val bitrateKbps: Int = 128,
    val lastPingTimestamp: Long = System.currentTimeMillis()
)

data class ActiveCall(
    val callId: String,
    val peer: Peer,
    val isVideo: Boolean,
    val state: CallState,
    val startTime: Long = 0L,
    val isMicMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val signalInfo: PeerSignalInfo? = null
)

/**
 * Local device profile and user account model.
 */
data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String = "",
    val avatarColor: Long = 0xFF6750A4,
    val userStatus: UserPresenceStatus = UserPresenceStatus.ONLINE,
    val statusMessage: String = "متصل محلياً ومستعد للحديث",
    val bio: String = "",
    val isLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Received video frame from a peer.
 */
data class PeerVideoFrame(
    val peerId: String,
    val peerName: String,
    val bitmap: android.graphics.Bitmap?,
    val timestamp: Long
)
