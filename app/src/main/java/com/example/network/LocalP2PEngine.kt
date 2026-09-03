package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.ActiveCall
import com.example.model.ActiveGroupCall
import com.example.model.CallReactionEvent
import com.example.model.GroupCallInvitation
import com.example.model.MsgAck
import com.example.model.CallState
import com.example.model.ChatMessage
import com.example.model.ConnectionQualityLevel
import com.example.model.MessageType
import com.example.model.Peer
import com.example.model.PeerSignalInfo
import com.example.model.RoomInfo
import com.example.model.RoomInvitation
import com.example.model.TypingPeer
import com.example.model.UserProfile
import com.example.model.UserPresenceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.audio.VoiceNotePlayer
import com.example.audio.VoiceNoteRecorder
import com.example.utils.LocalNotificationManager
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LocalP2PEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalP2PEngine"
        // 45s: survives slow system file pickers / temporary UI stalls without
        // marking an active user as gone (was 8s, which dropped peers mid-action).
        private const val PEER_TIMEOUT_MS = 45_000L
        private const val CALL_RINGING_TIMEOUT_MS = 45_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var multicastLock: WifiManager.MulticastLock? = null

    val audioEngine = AudioEngine(context)
    val videoEngine = VideoEngine(context)
    val fileTransferEngine = FileTransferEngine(context)
    val voiceNoteRecorder = VoiceNoteRecorder(context)
    val voiceNotePlayer = VoiceNotePlayer(context)

    // RULE 3: dialing ringback, incoming ringtone + vibration, connected chime, busy tone
    private val callToneManager = CallToneManager(context)

    private var ringingTimeoutJob: Job? = null
    private var incomingRingTimeoutJob: Job? = null
    private var groupRingTimeoutJob: Job? = null

    private val nsdEngine = NetworkServiceDiscoveryEngine(context)

    private var broadcastSocket: DatagramSocket? = null
    private var multicastSocket: MulticastSocket? = null

    private var discoveryJob: Job? = null
    private var heartbeatJob: Job? = null
    private var cleanupJob: Job? = null

    private val peersMap = ConcurrentHashMap<String, Peer>()
    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private var scanJob: Job? = null
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    private val _scannedIpCount = MutableStateFlow(0)
    val scannedIpCount = _scannedIpCount.asStateFlow()

    private val _currentScanSubnet = MutableStateFlow("")
    val currentScanSubnet = _currentScanSubnet.asStateFlow()

    private val _lastScanTime = MutableStateFlow(System.currentTimeMillis())
    val lastScanTime = _lastScanTime.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall = _activeCall.asStateFlow()

    // Incoming call held while the user is already in a CONNECTED call (call waiting)
    private val _callWaitingInvite = MutableStateFlow<ActiveCall?>(null)
    val callWaitingInvite = _callWaitingInvite.asStateFlow()

    // Human-readable reason for an ended outgoing call (e.g. peer busy)
    private val _callEndedReason = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val callEndedReason = _callEndedReason.asSharedFlow()

    private val _activeGroupCall = MutableStateFlow<ActiveGroupCall?>(null)
    val activeGroupCall = _activeGroupCall.asStateFlow()

    private val _incomingGroupCallInvite = MutableSharedFlow<GroupCallInvitation>(extraBufferCapacity = 16)
    val incomingGroupCallInvite = _incomingGroupCallInvite.asSharedFlow()

    private var callQualityJob: Job? = null
    private val _callSignalInfo = MutableStateFlow<PeerSignalInfo?>(null)
    val callSignalInfo = _callSignalInfo.asStateFlow()

    private var lastMeasuredLatency = 12L
    private var lastMeasuredJitter = 2L

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

    // Delivery/read acknowledgements for OUR outgoing messages (from peers)
    private val _messageAcks = MutableSharedFlow<MsgAck>(extraBufferCapacity = 64)
    val messageAcks = _messageAcks.asSharedFlow()

    // Floating emoji reactions fired during active calls
    private val _callReactions = MutableSharedFlow<CallReactionEvent>(extraBufferCapacity = 32)
    val callReactions = _callReactions.asSharedFlow()

    private val _incomingRoomInvites = MutableSharedFlow<RoomInvitation>(extraBufferCapacity = 16)
    val incomingRoomInvites = _incomingRoomInvites.asSharedFlow()

    private val typingMap = ConcurrentHashMap<String, TypingPeer>()
    private val _typingPeers = MutableStateFlow<List<TypingPeer>>(emptyList())
    val typingPeers = _typingPeers.asStateFlow()

    private val _currentRoom = MutableStateFlow("general")
    val currentRoom = _currentRoom.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = UUID.randomUUID().toString().substring(0, 8),
            username = "مستخدم " + (100..999).random(),
            displayName = "مستخدم جديد",
            avatarColor = 0xFF6750A4,
            statusMessage = "متصل محلياً ومستعد للحديث",
            bio = "مستخدم لتطبيق المحادثة المحلية",
            isLoggedIn = true
        )
    )
    val userProfile = _userProfile.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp = _localIp.asStateFlow()

    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        videoEngine.setMyIdentity(profile.id, profile.username)
        broadcastPresence()
    }

    fun setUserStatus(status: UserPresenceStatus) {
        _userProfile.value = _userProfile.value.copy(userStatus = status)
        broadcastPresence()
    }

    fun updateProfile(
        username: String,
        avatarColor: Long,
        statusMessage: String = "متصل محلياً ومستعد للحديث",
        displayName: String = "",
        bio: String = "",
        userStatus: UserPresenceStatus = _userProfile.value.userStatus,
        isLoggedIn: Boolean = true
    ) {
        _userProfile.value = _userProfile.value.copy(
            username = username,
            displayName = if (displayName.isNotBlank()) displayName else username,
            avatarColor = avatarColor,
            statusMessage = statusMessage,
            bio = bio,
            userStatus = userStatus,
            isLoggedIn = isLoggedIn
        )
        videoEngine.setMyIdentity(_userProfile.value.id, username)
        broadcastPresence()
    }

    fun start() {
        _localIp.value = NetworkUtils.getLocalIpAddress(context)
        multicastLock = NetworkUtils.acquireMulticastLock(context)

        videoEngine.setMyIdentity(_userProfile.value.id, _userProfile.value.username)

        audioEngine.startAudioPlayback()
        videoEngine.startVideoReceiver()

        startListening()
        startHeartbeat()
        startPeerCleanup()
        triggerNetworkScan()

        // Start Network Service Discovery (mDNS)
        nsdEngine.startDiscoveryAndRegistration(_userProfile.value.id, _userProfile.value.username)
        scope.launch {
            nsdEngine.discoveredMdnsPeers.collect { mdnsPeers ->
                for (peer in mdnsPeers) {
                    if (peer.id != _userProfile.value.id) {
                        val existing = peersMap[peer.id]
                        if (existing == null || existing.ip != peer.ip) {
                            peersMap[peer.id] = peer
                            sendHandshakeProbe(peer.ip)
                        }
                    }
                }
                _discoveredPeers.value = peersMap.values.toList()
                updateRoomAudioTargets(_currentRoom.value)
            }
        }
    }

    /**
     * Sends an immediate handshake probe to a newly discovered peer IP to establish instant bi-directional P2P connection.
     */
    fun sendHandshakeProbe(ip: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "HANDSHAKE_PROBE")
                    put("id", _userProfile.value.id)
                    put("name", _userProfile.value.username)
                    put("color", _userProfile.value.avatarColor)
                    put("avatarUri", _userProfile.value.avatarUri ?: "")
                    put("userStatus", _userProfile.value.userStatus.name)
                    put("statusMessage", _userProfile.value.statusMessage)
                    put("room", _currentRoom.value)
                    put("isSpeaking", audioEngine.micLevel.value > 0.08f)
                    put("isVideoActive", videoEngine.isVideoStreaming.value)
                    put("isMuted", audioEngine.isMuted.value)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonToIp(json, ip)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send handshake probe to $ip", e)
            }
        }
    }

    fun stop() {
        scanJob?.cancel()
        heartbeatJob?.cancel()
        discoveryJob?.cancel()
        cleanupJob?.cancel()
        ringingTimeoutJob?.cancel()
        incomingRingTimeoutJob?.cancel()
        groupRingTimeoutJob?.cancel()
        callToneManager.stopAll()
        ScreenCaptureService.stop(context)
        audioEngine.stopAllAudio()
        videoEngine.stopCameraStream()
        videoEngine.stopVideoReceiver()
        fileTransferEngine.stop()
        nsdEngine.stop()
        try {
            broadcastSocket?.close()
            multicastSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        NetworkUtils.releaseMulticastLock(multicastLock)
    }

    /**
     * Active Subnet Scanner: sweeps IP addresses in the local /24 subnet and sends UDP probes.
     */
    fun triggerNetworkScan() {
        if (_isScanning.value) return
        scanJob?.cancel()
        scanJob = scope.launch {
            _isScanning.value = true
            _scanProgress.value = 0.05f
            _scannedIpCount.value = 0

            val currentIp = NetworkUtils.getLocalIpAddress(context)
            _localIp.value = currentIp

            // 1. Send immediate multicast & broadcast presence
            broadcastPresence()

            // 2. Perform /24 subnet sweep if valid IPv4 is found
            if (currentIp.isNotEmpty() && currentIp != "127.0.0.1" && currentIp.contains(".")) {
                val lastDot = currentIp.lastIndexOf('.')
                if (lastDot > 0) {
                    val prefix = currentIp.substring(0, lastDot + 1)
                    _currentScanSubnet.value = prefix + "0/24"
                    val selfLastOctet = currentIp.substring(lastDot + 1).toIntOrNull() ?: -1

                    val totalIps = 254
                    var processed = 0

                    val pingJson = JSONObject().apply {
                        put("type", "PING")
                        put("id", _userProfile.value.id)
                        put("name", _userProfile.value.username)
                        put("color", _userProfile.value.avatarColor)
                        put("userStatus", _userProfile.value.userStatus.name)
                        put("statusMessage", _userProfile.value.statusMessage)
                        put("room", _currentRoom.value)
                        put("isSpeaking", audioEngine.micLevel.value > 0.08f)
                        put("isVideoActive", videoEngine.isVideoStreaming.value)
                        put("isMuted", audioEngine.isMuted.value)
                        put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                        put("isScanProbe", true)
                    }

                    // Sweep in batches of 20 IPs with gentle pacing
                    for (batchStart in 1..254 step 20) {
                        if (!isActive) break
                        val batchEnd = minOf(batchStart + 19, 254)
                        for (i in batchStart..batchEnd) {
                            if (i == selfLastOctet) {
                                processed++
                                continue
                            }
                            val targetIp = "$prefix$i"
                            sendJsonToIp(pingJson, targetIp)
                            processed++
                        }
                        _scannedIpCount.value = processed
                        _scanProgress.value = (processed.toFloat() / totalIps).coerceIn(0f, 0.95f)
                        delay(60)
                    }
                }
            } else {
                _currentScanSubnet.value = "نطاق الشبكة المحلية"
                for (p in 1..10) {
                    delay(120)
                    _scanProgress.value = (p / 10f).coerceIn(0f, 0.95f)
                    _scannedIpCount.value = p * 25
                }
            }

            // Final broadcast ping to finalize discovery
            broadcastPresence()
            delay(350)
            _scanProgress.value = 1f
            _isScanning.value = false
            _lastScanTime.value = System.currentTimeMillis()
        }
    }

    fun connectDirectIp(ip: String) {
        manualConnectToIp(ip)
    }

    fun sendRoomMessage(roomId: String, message: ChatMessage) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT_MSG")
                    put("id", message.id)
                    put("senderId", message.senderId)
                    put("senderName", message.senderName)
                    put("senderColor", message.senderColor)
                    put("target", roomId)
                    put("isDirect", false)
                    put("content", message.content)
                    put("timestamp", message.timestamp)
                    put("msgType", message.messageType.name)
                    if (message.imageBase64 != null) {
                        put("image", message.imageBase64)
                    }
                    if (message.fileId != null) {
                        put("fileId", message.fileId)
                        put("fileName", message.fileName)
                        put("fileSize", message.fileSize)
                        put("mimeType", message.mimeType)
                        put("durationSeconds", message.durationSeconds)
                        put("senderIp", message.senderIp ?: _localIp.value)
                    }
                }
                sendJsonPacket(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send room message", e)
            }
        }
    }

    fun sendDirectMessage(peer: Peer, message: ChatMessage) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT_MSG")
                    put("id", message.id)
                    put("senderId", message.senderId)
                    put("senderName", message.senderName)
                    put("senderColor", message.senderColor)
                    put("target", peer.id)
                    put("isDirect", true)
                    put("content", message.content)
                    put("timestamp", message.timestamp)
                    put("msgType", message.messageType.name)
                    if (message.imageBase64 != null) {
                        put("image", message.imageBase64)
                    }
                    if (message.fileId != null) {
                        put("fileId", message.fileId)
                        put("fileName", message.fileName)
                        put("fileSize", message.fileSize)
                        put("mimeType", message.mimeType)
                        put("durationSeconds", message.durationSeconds)
                        put("senderIp", message.senderIp ?: _localIp.value)
                    }
                }
                sendJsonToIp(json, peer.ip)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send direct message", e)
            }
        }
    }

    /**
     * Broadcasts or sends user typing status over the local P2P network.
     */
    fun sendTypingStatus(target: String, isDirect: Boolean, isTyping: Boolean) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "TYPING_STATUS")
                    put("senderId", _userProfile.value.id)
                    put("senderName", _userProfile.value.displayName.ifBlank { _userProfile.value.username })
                    put("senderColor", _userProfile.value.avatarColor)
                    put("target", target)
                    put("isDirect", isDirect)
                    put("isTyping", isTyping)
                    put("timestamp", System.currentTimeMillis())
                }
                if (isDirect) {
                    val peer = peersMap[target]
                    if (peer != null) {
                        sendJsonToIp(json, peer.ip)
                    } else {
                        sendJsonPacket(json)
                    }
                } else {
                    sendJsonPacket(json)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send typing status", e)
            }
        }
    }

    fun setRoom(roomId: String) {
        _currentRoom.value = roomId
        updateRoomAudioTargets(roomId)
        broadcastPresence()
    }

    // --- Message delivery/read receipts & floating call reactions ---

    /**
     * Confirms receipt of an incoming message so the sender's tick upgrades
     * to "delivered" (double gray check).
     */
    fun sendMsgAck(message: ChatMessage) {
        val senderIp = message.senderIp ?: return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "MSG_ACK")
                    put("msgId", message.id)
                    put("receiverId", _userProfile.value.id)
                }
                sendJsonToIp(json, senderIp)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Tells the sender that their message is currently displayed on screen
     * ("read" — double blue check).
     */
    fun sendMsgRead(messageId: String, senderIp: String) {
        if (senderIp.isBlank()) return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "MSG_READ")
                    put("msgId", messageId)
                    put("readerId", _userProfile.value.id)
                }
                sendJsonToIp(json, senderIp)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Fires a floating emoji reaction to the remote party of the active call
     * (direct call) or to all room participants (group call).
     */
    fun sendCallReaction(emoji: String) {
        if (emoji.isBlank()) return
        val profile = _userProfile.value
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CALL_REACTION")
                    put("emoji", emoji)
                    put("senderId", profile.id)
                    put("senderName", profile.displayName.ifBlank { profile.username })
                }
                val call = _activeCall.value
                val group = _activeGroupCall.value
                when {
                    call != null -> {
                        json.put("callId", call.callId)
                        sendJsonToIp(json, call.peer.ip)
                    }
                    group != null -> {
                        json.put("callId", group.callId)
                        sendJsonPacket(json)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Updates Audio & Video streaming targets based on who is in the current room.
     */
    fun updateRoomAudioTargets(roomId: String) {
        audioEngine.clearTargets()
        videoEngine.clearTargets()

        val roomPeers = peersMap.values.filter { it.currentRoom == roomId && it.id != _userProfile.value.id }
        for (peer in roomPeers) {
            try {
                val addr = InetAddress.getByName(peer.ip)
                audioEngine.updateTarget(peer.id, addr, NetworkUtils.AUDIO_PORT)
                videoEngine.updateTarget(peer.id, addr, NetworkUtils.VIDEO_PORT)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting target for peer: ${peer.name}", e)
            }
        }
    }

    /**
     * Listens for UDP multicast & broadcast packets on DISCOVERY_PORT.
     */
    private fun startListening() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            try {
                multicastSocket?.close()
                multicastSocket = MulticastSocket(NetworkUtils.DISCOVERY_PORT).apply {
                    reuseAddress = true
                    try {
                        val group = InetAddress.getByName(NetworkUtils.MULTICAST_GROUP_IP)
                        joinGroup(group)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not join multicast group, falling back to broadcast", e)
                    }
                }

                val buffer = ByteArray(65507)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        multicastSocket?.receive(packet)
                        // RULE 5: signaling payloads are AES-256-GCM encrypted; any packet
                        // that fails AEAD authentication is silently discarded.
                        // TODO: Optimize - pass offset/length to decrypt to avoid array copy
                        val decryptedBytes = LocalCryptoEngine.decrypt(
                            packet.data.copyOfRange(0, packet.length)
                        ) ?: continue
                        val dataStr = String(decryptedBytes, Charsets.UTF_8)
                        val senderIp = packet.address.hostAddress ?: ""
                        handleIncomingPacket(dataStr, senderIp)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery listener error", e)
            }
        }
    }

    /**
     * Periodically announces presence to the network.
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                _localIp.value = NetworkUtils.getLocalIpAddress(context)
                broadcastPresence()
                delay(2500)
            }
        }
    }

    /**
     * Removes dead peers who haven't pinged in PEER_TIMEOUT_MS and expired typing indicators.
     */
    private fun startPeerCleanup() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(1200)
                val now = System.currentTimeMillis()
                var peersChanged = false

                // Never expire peers involved in a live call or an active download:
                // system file pickers / heavy UI can silence heartbeats for a while
                // without the peer actually leaving the network.
                val protectedPeerIds = buildSet {
                    _activeCall.value?.peer?.id?.let { add(it) }
                    _callWaitingInvite.value?.peer?.id?.let { add(it) }
                    _activeGroupCall.value?.participants?.forEach { add(it.id) }
                }
                val transferRunning = fileTransferEngine.downloadingIds.value.isNotEmpty()

                peersMap.entries.removeIf { (_, peer) ->
                    val isExpired = (now - peer.lastSeen) > PEER_TIMEOUT_MS &&
                            peer.id !in protectedPeerIds &&
                            !transferRunning
                    if (isExpired) peersChanged = true
                    isExpired
                }
                if (peersChanged) {
                    _discoveredPeers.value = peersMap.values.toList()
                    updateRoomAudioTargets(_currentRoom.value)
                }

                // Cleanup expired typing indicators (older than 3.5 seconds)
                var typingChanged = false
                val iterator = typingMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if ((now - entry.value.timestamp) > 3500L) {
                        typingChanged = true
                        iterator.remove()
                    }
                }
                if (typingChanged) {
                    _typingPeers.value = typingMap.values.toList()
                }
            }
        }
    }

    /**
     * Broadcasts current presence packet.
     */
    fun broadcastPresence() {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "PING")
                    put("id", _userProfile.value.id)
                    put("name", _userProfile.value.username)
                    put("color", _userProfile.value.avatarColor)
                    put("avatarUri", _userProfile.value.avatarUri ?: "")
                    put("avatarBase64", _userProfile.value.avatarBase64 ?: "")
                    put("userStatus", _userProfile.value.userStatus.name)
                    put("statusMessage", _userProfile.value.statusMessage)
                    put("room", _currentRoom.value)
                    put("isSpeaking", audioEngine.micLevel.value > 0.08f)
                    put("isVideoActive", videoEngine.isVideoStreaming.value)
                    put("isMuted", audioEngine.isMuted.value)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                }
                sendJsonPacket(json)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Sends a text or image message over the local network.
     */
    fun sendMessage(
        targetRoomOrPeerId: String,
        content: String,
        isDirect: Boolean = false,
        imageBase64: String? = null,
        messageType: MessageType = MessageType.TEXT,
        fileId: String? = null,
        fileName: String? = null,
        fileSize: Long = 0L,
        mimeType: String? = null
    ): ChatMessage {
        val msgId = UUID.randomUUID().toString()
        val chatMessage = ChatMessage(
            id = msgId,
            senderId = _userProfile.value.id,
            senderName = _userProfile.value.username,
            senderColor = _userProfile.value.avatarColor,
            targetRoomOrPeerId = targetRoomOrPeerId,
            isDirect = isDirect,
            content = content,
            timestamp = System.currentTimeMillis(),
            isMine = true,
            messageType = messageType,
            imageBase64 = imageBase64,
            fileId = fileId,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType
        )

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CHAT_MSG")
                    put("id", msgId)
                    put("senderId", _userProfile.value.id)
                    put("senderName", _userProfile.value.username)
                    put("senderColor", _userProfile.value.avatarColor)
                    put("target", targetRoomOrPeerId)
                    put("isDirect", isDirect)
                    put("content", content)
                    put("timestamp", chatMessage.timestamp)
                    put("msgType", messageType.name)
                    if (imageBase64 != null) {
                        put("image", imageBase64)
                    }
                    fileId?.let { put("fileId", it) }
                    fileName?.let { put("fileName", it) }
                    if (fileSize > 0) put("fileSize", fileSize)
                    mimeType?.let { put("mimeType", it) }
                }

                if (isDirect) {
                    val peer = peersMap[targetRoomOrPeerId]
                    if (peer != null) {
                        sendJsonToIp(json, peer.ip)
                    } else {
                        sendJsonPacket(json)
                    }
                } else {
                    sendJsonPacket(json)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }

        return chatMessage
    }

    // --- Audio session keys (8 bytes, stamped into every audio packet header) ---
    private fun callSessionKey(callId: String) = callId.take(AudioEngine.HEADER_SESSION_SIZE)
    private fun groupSessionKey(callId: String) = ("gr" + callId.take(6)).take(AudioEngine.HEADER_SESSION_SIZE)
    private fun roomSessionKey(roomId: String) = ("rm" + roomId.take(6)).take(AudioEngine.HEADER_SESSION_SIZE)

    /**
     * RULE 1.2: room voice audio only flows once the user explicitly joins the room
     * voice channel; joining authorizes the matching playback session as well.
     * Refuses to start while a call owns the microphone.
     */
    fun startRoomVoiceSession() {
        if (_activeCall.value != null || _activeGroupCall.value != null) return
        val roomId = _currentRoom.value
        audioEngine.authorizeSession(roomSessionKey(roomId))
        audioEngine.setOpenMic(true)
        updateRoomAudioTargets(roomId)
        audioEngine.startAudioRecording(_userProfile.value.id, roomId, roomSessionKey(roomId))
    }

    fun stopRoomVoiceSession() {
        audioEngine.setOpenMic(false)
        audioEngine.setPushToTalk(false)
        audioEngine.stopAudioRecording()
        audioEngine.revokeSession()
    }

    // Room-voice suspension bookkeeping across a call's lifetime
    private var roomVoiceSuspendedForCall = false

    /**
     * Releases the room open-mic before a call claims the microphone. Without this,
     * the AudioRecord session is already active so the call's recording silently
     * fails (early-return) AND the room keeps hearing the private call audio.
     */
    private fun suspendRoomVoiceForCall() {
        if (audioEngine.isRecording.value || audioEngine.isOpenMic.value) {
            roomVoiceSuspendedForCall = true
            audioEngine.setOpenMic(false)
            audioEngine.setPushToTalk(false)
            audioEngine.stopAudioRecording()
            audioEngine.revokeSession()
        }
    }

    /** Restores the room open-mic session suspended when the call started. */
    private fun resumeRoomVoiceAfterCall() {
        if (roomVoiceSuspendedForCall) {
            roomVoiceSuspendedForCall = false
            startRoomVoiceSession()
        } else {
            updateRoomAudioTargets(_currentRoom.value)
        }
    }

    /** Stops the incoming call ringtone/vibration when a group invite is dismissed. */
    fun dismissIncomingGroupCallRing() {
        groupRingTimeoutJob?.cancel()
        groupRingTimeoutJob = null
        callToneManager.stopAll()
    }

    /**
     * Initiates a 1-to-1 Call with another peer.
     * RULE 3: the caller immediately hears the international ringback tone, and the
     * call auto-ends after 45 seconds if it is never answered.
     */
    fun startCall(peer: Peer, isVideo: Boolean) {
        val callId = UUID.randomUUID().toString()

        // The private call now owns the microphone — suspend the room open mic
        suspendRoomVoiceForCall()

        _activeCall.value = ActiveCall(
            callId = callId,
            peer = peer,
            isVideo = isVideo,
            state = CallState.OUTGOING_RINGING,
            isSpeakerOn = audioEngine.isSpeakerOn.value,
            isMicMuted = audioEngine.isMuted.value,
            isCameraOff = videoEngine.isCameraOff.value,
            isFrontCamera = videoEngine.isFrontCamera.value
        )

        callToneManager.playRingback()
        scheduleOutgoingRingTimeout(callId)

        // Set audio/video targets
        try {
            val addr = InetAddress.getByName(peer.ip)
            audioEngine.updateTarget(peer.id, addr, NetworkUtils.AUDIO_PORT)
            videoEngine.updateTarget(peer.id, addr, NetworkUtils.VIDEO_PORT)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving peer IP for call", e)
        }

        scope.launch {
            val json = JSONObject().apply {
                put("type", "CALL_INVITE")
                put("callId", callId)
                put("isVideo", isVideo)
                put("callerId", _userProfile.value.id)
                put("callerName", _userProfile.value.username)
                put("callerColor", _userProfile.value.avatarColor)
                put("callerAvatarUri", _userProfile.value.avatarUri ?: "")
                put("callerAvatarBase64", _userProfile.value.avatarBase64 ?: "")
                put("callerIp", _localIp.value)
            }
            sendJsonToIp(json, peer.ip)
        }
    }

    private fun scheduleOutgoingRingTimeout(callId: String) {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = scope.launch {
            delay(CALL_RINGING_TIMEOUT_MS)
            val call = _activeCall.value
            if (call != null && call.callId == callId && call.state == CallState.OUTGOING_RINGING) {
                Log.i(TAG, "انتهت مهلة الرنين (45 ثانية) — إنهاء المكالمة: لا يوجد رد")
                endCall()
            }
        }
    }

    private fun scheduleIncomingRingTimeout(callId: String) {
        incomingRingTimeoutJob?.cancel()
        incomingRingTimeoutJob = scope.launch {
            delay(CALL_RINGING_TIMEOUT_MS)
            val call = _activeCall.value
            if (call != null && call.callId == callId && call.state == CallState.INCOMING_RINGING) {
                Log.i(TAG, "انتهت مهلة رنين المكالمة الواردة (45 ثانية) — رفض تلقائي")
                callToneManager.stopAll()
                _activeCall.value = null
                resumeRoomVoiceAfterCall()
            }
        }
    }

    private var waitingRingTimeoutJob: Job? = null

    private fun scheduleWaitingRingTimeout(callId: String) {
        waitingRingTimeoutJob?.cancel()
        waitingRingTimeoutJob = scope.launch {
            delay(CALL_RINGING_TIMEOUT_MS)
            val waiting = _callWaitingInvite.value
            if (waiting != null && waiting.callId == callId) {
                declineWaitingCall(reason = "انتهت مهلة انتظار المكالمة")
            }
        }
    }

    /**
     * Promotes a held call-waiting invite to the active call: the current call is
     * ended silently first, then the waiting call enters the normal accept flow.
     */
    fun acceptWaitingCall() {
        val waiting = _callWaitingInvite.value ?: return
        _callWaitingInvite.value = null
        waitingRingTimeoutJob?.cancel()
        callToneManager.stopAll()

        val current = _activeCall.value
        if (current != null) {
            _activeCall.value = null
            stopCallQualityMonitor()
            audioEngine.revokeSession()
            audioEngine.stopAudioRecording()
            videoEngine.stopCameraStream()
            audioEngine.removeTarget(current.peer.id)
            videoEngine.removeTarget(current.peer.id)
            scope.launch {
                try {
                    val json = JSONObject().apply {
                        put("type", "CALL_END")
                        put("callId", current.callId)
                    }
                    sendJsonToIp(json, current.peer.ip)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        _activeCall.value = waiting
        acceptCall()
    }

    /** Declines a held call-waiting invite and notifies the waiting caller. */
    fun declineWaitingCall(reason: String = "رفض المستخدم المكالمة") {
        val waiting = _callWaitingInvite.value ?: return
        _callWaitingInvite.value = null
        waitingRingTimeoutJob?.cancel()
        callToneManager.stopAll()
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CALL_BUSY")
                    put("callId", waiting.callId)
                    put("reason", reason)
                }
                sendJsonToIp(json, waiting.peer.ip)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /** Shows a full-priority incoming call notification when the app is backgrounded. */
    private fun maybeShowIncomingCallNotification(callerName: String, callId: String, isVideo: Boolean) {
        if (!LocalNotificationManager.isAppInForeground) {
            LocalNotificationManager.showIncomingCallNotification(context, callerName, callId, isVideo)
        }
    }

    /**
     * Answers an incoming call. Recording + playback open ONLY here (state = CONNECTED).
     */
    fun acceptCall() {
        val currentCall = _activeCall.value ?: return
        ringingTimeoutJob?.cancel()
        incomingRingTimeoutJob?.cancel()
        waitingRingTimeoutJob?.cancel()
        _callWaitingInvite.value = null
        callToneManager.stopAll()
        callToneManager.playConnectedChime()

        val updatedCall = currentCall.copy(
            state = CallState.CONNECTED,
            startTime = System.currentTimeMillis(),
            isSpeakerOn = audioEngine.isSpeakerOn.value,
            isMicMuted = audioEngine.isMuted.value,
            isCameraOff = videoEngine.isCameraOff.value,
            isFrontCamera = videoEngine.isFrontCamera.value
        )
        _activeCall.value = updatedCall

        // Ensure targets are configured
        try {
            val addr = InetAddress.getByName(currentCall.peer.ip)
            audioEngine.updateTarget(currentCall.peer.id, addr, NetworkUtils.AUDIO_PORT)
            videoEngine.updateTarget(currentCall.peer.id, addr, NetworkUtils.VIDEO_PORT)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting call target", e)
        }

        // RULE 1: microphone transmission and playback start strictly on acceptance.
        // Video receiver always runs so remote screen shares reach audio-only calls too.
        audioEngine.authorizeSession(callSessionKey(currentCall.callId))
        audioEngine.startAudioRecording(_userProfile.value.id, "call_${currentCall.callId}", callSessionKey(currentCall.callId))
        videoEngine.startVideoReceiver()
        startCallQualityMonitor(currentCall.callId, currentCall.peer.ip, currentCall.isVideo)

        scope.launch {
            val json = JSONObject().apply {
                put("type", "CALL_ACCEPT")
                put("callId", currentCall.callId)
                put("peerId", _userProfile.value.id)
                put("acceptorAvatarBase64", _userProfile.value.avatarBase64 ?: "")
            }
            sendJsonToIp(json, currentCall.peer.ip)
        }
    }

    fun toggleCallMic() {
        audioEngine.toggleMute()
        val isMuted = audioEngine.isMuted.value
        _activeCall.value = _activeCall.value?.copy(isMicMuted = isMuted)
        sendCallControl(isMuted = isMuted)
    }

    fun toggleCallCamera() {
        videoEngine.toggleCameraVideo()
        val isCameraOff = videoEngine.isCameraOff.value
        _activeCall.value = _activeCall.value?.copy(isCameraOff = isCameraOff)
        sendCallControl(isCameraOff = isCameraOff)
    }

    fun switchCallCamera() {
        videoEngine.switchCamera()
        _activeCall.value = _activeCall.value?.copy(isFrontCamera = videoEngine.isFrontCamera.value)
    }

    fun toggleCallSpeaker() {
        audioEngine.toggleSpeaker()
        _activeCall.value = _activeCall.value?.copy(isSpeakerOn = audioEngine.isSpeakerOn.value)
    }

    // --- RULE 2: Real MediaProjection screen sharing (foreground service driven) ---

    /**
     * Starts the real screen broadcast: launches the mediaProjection foreground
     * service, routes captured frames into the video engine, and notifies the peer.
     */
    fun startRealScreenShare(appName: String, resultCode: Int, projectionData: android.content.Intent) {
        ScreenCaptureService.onFrameCaptured = { bitmap ->
            videoEngine.sendDirectScreenShareBitmap(bitmap, appName)
        }
        ScreenCaptureService.onShareStopped = { onScreenShareServiceStopped() }
        ScreenCaptureService.start(context, resultCode, projectionData, appName)

        _activeCall.value = _activeCall.value?.copy(
            isScreenSharing = true,
            screenSharedAppName = appName
        )
        _activeGroupCall.value = _activeGroupCall.value?.copy(
            isScreenSharing = true,
            screenSharedAppName = appName
        )
        sendCallControl(isScreenSharing = true, screenSharedAppName = appName)
        sendGroupCallControl(isScreenSharing = true, screenSharedAppName = appName)
    }

    fun stopRealScreenShare() {
        ScreenCaptureService.stop(context)
        videoEngine.stopScreenShare()
        clearScreenShareState()
    }

    private fun onScreenShareServiceStopped() {
        videoEngine.stopScreenShare()
        clearScreenShareState()
    }

    private fun clearScreenShareState() {
        _activeCall.value = _activeCall.value?.copy(
            isScreenSharing = false,
            screenSharedAppName = null
        )
        _activeGroupCall.value = _activeGroupCall.value?.copy(
            isScreenSharing = false,
            screenSharedAppName = null
        )
        sendCallControl(isScreenSharing = false, screenSharedAppName = "")
        sendGroupCallControl(isScreenSharing = false, screenSharedAppName = "")
    }

    private fun sendCallControl(
        isMuted: Boolean? = null,
        isCameraOff: Boolean? = null,
        isScreenSharing: Boolean? = null,
        screenSharedAppName: String? = null
    ) {
        val currentCall = _activeCall.value ?: return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "CALL_CONTROL")
                    put("callId", currentCall.callId)
                    put("senderId", _userProfile.value.id)
                    if (isMuted != null) put("isMuted", isMuted)
                    if (isCameraOff != null) put("isCameraOff", isCameraOff)
                    if (isScreenSharing != null) put("isScreenSharing", isScreenSharing)
                    if (screenSharedAppName != null) put("screenSharedAppName", screenSharedAppName)
                }
                sendJsonToIp(json, currentCall.peer.ip)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Starts continuous low-overhead ping-pong RTT latency and signal strength telemetry during calls.
     */
    private fun startCallQualityMonitor(callId: String, peerIp: String, isVideo: Boolean) {
        callQualityJob?.cancel()
        val initialInfo = PeerSignalInfo(
            peerId = _activeCall.value?.peer?.id ?: "",
            latencyMs = 12L,
            jitterMs = 1L,
            packetLossPercent = 0,
            quality = ConnectionQualityLevel.EXCELLENT,
            bars = 4,
            bitrateKbps = if (isVideo) 960 else 128,
            lastPingTimestamp = System.currentTimeMillis()
        )
        _callSignalInfo.value = initialInfo
        _activeCall.value = _activeCall.value?.copy(signalInfo = initialInfo)

        callQualityJob = scope.launch {
            while (isActive) {
                try {
                    val pingJson = JSONObject().apply {
                        put("type", "CALL_PING")
                        put("callId", callId)
                        put("peerId", _userProfile.value.id)
                        put("t", System.currentTimeMillis())
                        put("isVideo", isVideo)
                    }
                    sendJsonToIp(pingJson, peerIp)
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
                delay(1200)
            }
        }
    }

    private fun stopCallQualityMonitor() {
        callQualityJob?.cancel()
        callQualityJob = null
        _callSignalInfo.value = null
    }

    private fun handleCallPong(callId: String, sentTimestamp: Long, isVideo: Boolean) {
        val now = System.currentTimeMillis()
        val rawRtt = (now - sentTimestamp).coerceIn(1L, 9999L)

        // Exponential moving average for smooth display
        val smoothedLatency = if (lastMeasuredLatency <= 0) rawRtt else ((0.65f * rawRtt) + (0.35f * lastMeasuredLatency)).toLong()
        val jitter = kotlin.math.abs(smoothedLatency - lastMeasuredLatency).coerceAtLeast(1L)
        lastMeasuredLatency = smoothedLatency
        lastMeasuredJitter = jitter

        val quality = when {
            smoothedLatency < 35 -> ConnectionQualityLevel.EXCELLENT
            smoothedLatency in 35..75 -> ConnectionQualityLevel.GOOD
            smoothedLatency in 76..140 -> ConnectionQualityLevel.FAIR
            else -> ConnectionQualityLevel.POOR
        }

        val bars = when (quality) {
            ConnectionQualityLevel.EXCELLENT -> 4
            ConnectionQualityLevel.GOOD -> 3
            ConnectionQualityLevel.FAIR -> 2
            ConnectionQualityLevel.POOR -> 1
            ConnectionQualityLevel.DISCONNECTED -> 1
        }

        val baseBitrate = if (isVideo) 920 else 128
        val randomVariation = if (isVideo) (-40..60).random() else (-8..12).random()
        val bitrate = (baseBitrate + randomVariation).coerceAtLeast(32)

        val signalInfo = PeerSignalInfo(
            peerId = _activeCall.value?.peer?.id ?: "",
            latencyMs = smoothedLatency,
            jitterMs = jitter,
            packetLossPercent = if (smoothedLatency > 160) 6 else 0,
            quality = quality,
            bars = bars,
            bitrateKbps = bitrate,
            lastPingTimestamp = now
        )

        _callSignalInfo.value = signalInfo
        _activeCall.value = _activeCall.value?.copy(signalInfo = signalInfo)
    }

    /**
     * Sends an invitation to a peer to join a room.
     */
    fun sendRoomInvite(peer: Peer, room: com.example.model.RoomInfo) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "ROOM_INVITE")
                    put("roomId", room.id)
                    put("roomName", room.name)
                    put("roomDescription", room.description)
                    put("inviterId", _userProfile.value.id)
                    put("inviterName", _userProfile.value.username)
                    put("inviterColor", _userProfile.value.avatarColor)
                    put("maxCapacity", room.maxCapacity)
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonToIp(json, peer.ip)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send room invite", e)
            }
        }
    }

    /**
     * Declines or ends an active call.
     */
    fun endCall() {
        val currentCall = _activeCall.value ?: return
        _activeCall.value = null
        stopCallQualityMonitor()

        ringingTimeoutJob?.cancel()
        incomingRingTimeoutJob?.cancel()
        waitingRingTimeoutJob?.cancel()
        _callWaitingInvite.value = null
        callToneManager.stopAll()
        LocalNotificationManager.cancelCallNotification(context)

        audioEngine.revokeSession()
        audioEngine.stopAudioRecording()
        videoEngine.stopCameraStream()
        audioEngine.removeTarget(currentCall.peer.id)
        videoEngine.removeTarget(currentCall.peer.id)

        // Restore the room voice session if it was suspended when the call started
        resumeRoomVoiceAfterCall()

        // A ringing/connected call that is actively ended plays the ended cadence;
        // declining a never-connected incoming call stays silent.
        if (currentCall.state == CallState.CONNECTED || currentCall.state == CallState.OUTGOING_RINGING) {
            callToneManager.playDisconnectedTone()
        }

        scope.launch {
            val json = JSONObject().apply {
                put("type", "CALL_END")
                put("callId", currentCall.callId)
            }
            sendJsonToIp(json, currentCall.peer.ip)
        }
    }

    /**
     * Starts a room-wide Group Video & Audio Call (initiator explicitly launched it).
     */
    fun startGroupVideoCall(roomId: String, roomName: String) {
        val callId = UUID.randomUUID().toString()
        val roomPeers = peersMap.values.filter { it.currentRoom == roomId && it.id != _userProfile.value.id }

        // The group call owns the microphone now (room open mic would collide with it)
        suspendRoomVoiceForCall()

        _activeGroupCall.value = ActiveGroupCall(
            callId = callId,
            roomId = roomId,
            roomName = roomName,
            initiatorId = _userProfile.value.id,
            initiatorName = _userProfile.value.displayName.ifBlank { _userProfile.value.username },
            participants = roomPeers,
            isCameraOff = videoEngine.isCameraOff.value,
            isFrontCamera = videoEngine.isFrontCamera.value,
            isMicMuted = audioEngine.isMuted.value,
            isSpeakerOn = audioEngine.isSpeakerOn.value,
            startTime = System.currentTimeMillis()
        )

        updateRoomAudioTargets(roomId)

        audioEngine.authorizeSession(groupSessionKey(callId))
        audioEngine.startAudioRecording(_userProfile.value.id, "group_$callId", groupSessionKey(callId))
        videoEngine.startVideoReceiver()

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "GROUP_CALL_START")
                    put("callId", callId)
                    put("roomId", roomId)
                    put("roomName", roomName)
                    put("initiatorId", _userProfile.value.id)
                    put("initiatorName", _userProfile.value.displayName.ifBlank { _userProfile.value.username })
                    put("initiatorColor", _userProfile.value.avatarColor)
                    put("initiatorAvatarBase64", _userProfile.value.avatarBase64 ?: "")
                    put("initiatorIp", _localIp.value)
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonPacket(json)
                for (peer in roomPeers) {
                    sendJsonToIp(json, peer.ip)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting group video call", e)
            }
        }
    }

    /**
     * Joins an ongoing Group Video & Audio Call — RULE 1: recording and playback
     * start ONLY when the user explicitly clicks "Join Call", never on invite receipt.
     */
    fun joinGroupVideoCall(invitation: GroupCallInvitation) {
        groupRingTimeoutJob?.cancel()
        groupRingTimeoutJob = null
        callToneManager.stopAll()
        callToneManager.playConnectedChime()

        setRoom(invitation.roomId)
        val roomPeers = peersMap.values.filter { it.currentRoom == invitation.roomId && it.id != _userProfile.value.id }

        // The group call owns the microphone now (room open mic would collide with it)
        suspendRoomVoiceForCall()

        _activeGroupCall.value = ActiveGroupCall(
            callId = invitation.callId,
            roomId = invitation.roomId,
            roomName = invitation.roomName,
            initiatorId = invitation.initiatorId,
            initiatorName = invitation.initiatorName,
            participants = roomPeers,
            isCameraOff = videoEngine.isCameraOff.value,
            isFrontCamera = videoEngine.isFrontCamera.value,
            isMicMuted = audioEngine.isMuted.value,
            isSpeakerOn = audioEngine.isSpeakerOn.value,
            startTime = System.currentTimeMillis()
        )

        updateRoomAudioTargets(invitation.roomId)

        audioEngine.authorizeSession(groupSessionKey(invitation.callId))
        audioEngine.startAudioRecording(_userProfile.value.id, "group_${invitation.callId}", groupSessionKey(invitation.callId))
        videoEngine.startVideoReceiver()

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "GROUP_CALL_JOIN")
                    put("callId", invitation.callId)
                    put("roomId", invitation.roomId)
                    put("peerId", _userProfile.value.id)
                    put("peerName", _userProfile.value.displayName.ifBlank { _userProfile.value.username })
                    put("peerColor", _userProfile.value.avatarColor)
                    put("avatarBase64", _userProfile.value.avatarBase64 ?: "")
                    put("peerIp", _localIp.value)
                }
                sendJsonPacket(json)
                for (peer in roomPeers) {
                    sendJsonToIp(json, peer.ip)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error joining group call", e)
            }
        }
    }

    /**
     * Leaves the currently active Group Video Call.
     */
    fun leaveGroupVideoCall() {
        val currentGroupCall = _activeGroupCall.value ?: return
        _activeGroupCall.value = null
        callToneManager.stopAll()

        audioEngine.revokeSession()
        audioEngine.stopAudioRecording()
        videoEngine.stopCameraStream()

        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "GROUP_CALL_LEAVE")
                    put("callId", currentGroupCall.callId)
                    put("roomId", currentGroupCall.roomId)
                    put("peerId", _userProfile.value.id)
                }
                sendJsonPacket(json)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Resume the room voice session if it was suspended when the group call started
        resumeRoomVoiceAfterCall()
    }

    fun toggleGroupCallMic() {
        audioEngine.toggleMute()
        val isMuted = audioEngine.isMuted.value
        _activeGroupCall.value = _activeGroupCall.value?.copy(isMicMuted = isMuted)
        sendGroupCallControl(isMuted = isMuted)
    }

    fun toggleGroupCallCamera() {
        videoEngine.toggleCameraVideo()
        val isCameraOff = videoEngine.isCameraOff.value
        _activeGroupCall.value = _activeGroupCall.value?.copy(isCameraOff = isCameraOff)
        sendGroupCallControl(isCameraOff = isCameraOff)
    }

    fun switchGroupCallCamera() {
        videoEngine.switchCamera()
        _activeGroupCall.value = _activeGroupCall.value?.copy(isFrontCamera = videoEngine.isFrontCamera.value)
    }

    fun toggleGroupCallSpeaker() {
        audioEngine.toggleSpeaker()
        _activeGroupCall.value = _activeGroupCall.value?.copy(isSpeakerOn = audioEngine.isSpeakerOn.value)
    }

    private fun sendGroupCallControl(
        isMuted: Boolean? = null,
        isCameraOff: Boolean? = null,
        isScreenSharing: Boolean? = null,
        screenSharedAppName: String? = null
    ) {
        val currentGroupCall = _activeGroupCall.value ?: return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "GROUP_CALL_CONTROL")
                    put("callId", currentGroupCall.callId)
                    put("roomId", currentGroupCall.roomId)
                    put("senderId", _userProfile.value.id)
                    if (isMuted != null) put("isMuted", isMuted)
                    if (isCameraOff != null) put("isCameraOff", isCameraOff)
                    if (isScreenSharing != null) put("isScreenSharing", isScreenSharing)
                    if (screenSharedAppName != null) put("screenSharedAppName", screenSharedAppName)
                }
                sendJsonPacket(json)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Connects manually to an IP (e.g. 192.168.1.15).
     */
    fun manualConnectToIp(ip: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "PING")
                    put("id", _userProfile.value.id)
                    put("name", _userProfile.value.username)
                    put("color", _userProfile.value.avatarColor)
                    put("avatarUri", _userProfile.value.avatarUri ?: "")
                    put("userStatus", _userProfile.value.userStatus.name)
                    put("statusMessage", _userProfile.value.statusMessage)
                    put("room", _currentRoom.value)
                    put("isSpeaking", false)
                    put("isVideoActive", false)
                    put("isMuted", false)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                }
                sendJsonToIp(json, ip)
            } catch (e: Exception) {
                Log.e(TAG, "Manual connect failed", e)
            }
        }
    }

    private fun handleIncomingPacket(dataStr: String, senderIp: String) {
        try {
            val json = JSONObject(dataStr)
            val type = json.optString("type")

            when (type) {
                "PING" -> {
                    parseAndStorePeer(json, senderIp)

                    // If sender probed during subnet sweep, immediately reply so scanner discovers this device
                    if (json.optBoolean("isScanProbe", false)) {
                        scope.launch {
                            try {
                                val replyJson = JSONObject().apply {
                                    put("type", "PING")
                                    put("id", _userProfile.value.id)
                                    put("name", _userProfile.value.username)
                                    put("color", _userProfile.value.avatarColor)
                                    put("avatarUri", _userProfile.value.avatarUri ?: "")
                                    put("userStatus", _userProfile.value.userStatus.name)
                                    put("statusMessage", _userProfile.value.statusMessage)
                                    put("room", _currentRoom.value)
                                    put("isSpeaking", audioEngine.micLevel.value > 0.08f)
                                    put("isVideoActive", videoEngine.isVideoStreaming.value)
                                    put("isMuted", audioEngine.isMuted.value)
                                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                                }
                                sendJsonToIp(replyJson, senderIp)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }

                "HANDSHAKE_PROBE" -> {
                    val id = json.optString("id")
                    if (id == _userProfile.value.id || id.isEmpty()) return
                    parseAndStorePeer(json, senderIp)

                    scope.launch {
                        try {
                            val ackJson = JSONObject().apply {
                                put("type", "HANDSHAKE_ACK")
                                put("id", _userProfile.value.id)
                                put("name", _userProfile.value.username)
                                put("color", _userProfile.value.avatarColor)
                                put("avatarUri", _userProfile.value.avatarUri ?: "")
                                put("userStatus", _userProfile.value.userStatus.name)
                                put("statusMessage", _userProfile.value.statusMessage)
                                put("room", _currentRoom.value)
                                put("isSpeaking", audioEngine.micLevel.value > 0.08f)
                                put("isVideoActive", videoEngine.isVideoStreaming.value)
                                put("isMuted", audioEngine.isMuted.value)
                                put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                                put("timestamp", System.currentTimeMillis())
                            }
                            sendJsonToIp(ackJson, senderIp)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }

                "HANDSHAKE_ACK" -> {
                    val id = json.optString("id")
                    if (id == _userProfile.value.id || id.isEmpty()) return
                    parseAndStorePeer(json, senderIp)
                }

                "CHAT_MSG" -> {
                    val senderId = json.optString("senderId")
                    if (senderId == _userProfile.value.id) return

                    val target = json.optString("target")
                    val isDirect = json.optBoolean("isDirect", false)

                    // Clear typing indicator for this sender when message is delivered
                    val removedDirect = typingMap.remove("direct_$senderId")
                    val removedRoom = typingMap.remove("room_${target}_$senderId")
                    if (removedDirect != null || removedRoom != null) {
                        _typingPeers.value = typingMap.values.toList()
                    }

                    // Verify if this message is for this device
                    if (isDirect && target != _userProfile.value.id) return
                    if (!isDirect && target != _currentRoom.value && target != "all") return

                    val fileId = if (json.has("fileId")) json.optString("fileId") else null
                    val fileName = if (json.has("fileName")) json.optString("fileName") else null
                    val fileSize = json.optLong("fileSize", 0L)
                    val mimeType = if (json.has("mimeType")) json.optString("mimeType") else null
                    val durationSeconds = json.optInt("durationSeconds", 0)
                    val rawSenderIp = if (json.has("senderIp")) json.optString("senderIp") else null
                    val msgSenderIp = if (!rawSenderIp.isNullOrBlank() && rawSenderIp != "127.0.0.1") rawSenderIp else senderIp

                    val chatMessage = ChatMessage(
                        id = json.optString("id", UUID.randomUUID().toString()),
                        senderId = senderId,
                        senderName = json.optString("senderName", "مستخدم"),
                        senderColor = json.optLong("senderColor", 0xFF0EA5E9),
                        targetRoomOrPeerId = if (isDirect) senderId else target,
                        isDirect = isDirect,
                        content = json.optString("content", ""),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        isMine = false,
                        messageType = try {
                            val parsed = MessageType.valueOf(json.optString("msgType", MessageType.TEXT.name))
                            if (parsed == MessageType.FILE && mimeType?.startsWith("image/") == true) MessageType.IMAGE else parsed
                        } catch (e: Exception) {
                            if (mimeType?.startsWith("image/") == true) MessageType.IMAGE
                            else if (fileId != null) MessageType.FILE
                            else MessageType.TEXT
                        },
                        imageBase64 = if (json.has("image")) json.optString("image") else null,
                        fileId = fileId,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        durationSeconds = durationSeconds,
                        senderIp = msgSenderIp,
                        isDownloaded = false
                    )

                    _incomingMessages.tryEmit(chatMessage)
                }

                "TYPING_STATUS" -> {
                    val senderId = json.optString("senderId")
                    if (senderId == _userProfile.value.id || senderId.isEmpty()) return

                    val target = json.optString("target")
                    val isDirect = json.optBoolean("isDirect", false)
                    val isTyping = json.optBoolean("isTyping", false)
                    val senderName = json.optString("senderName", "مستخدم")
                    val senderColor = json.optLong("senderColor", 0xFF0EA5E9)

                    // For direct chats: check if target is our device
                    if (isDirect && target != _userProfile.value.id) return

                    val typingKey = if (isDirect) "direct_$senderId" else "room_${target}_$senderId"
                    if (isTyping) {
                        typingMap[typingKey] = TypingPeer(
                            peerId = senderId,
                            peerName = senderName,
                            peerColor = senderColor,
                            roomId = if (isDirect) null else target,
                            isDirect = isDirect,
                            targetPeerId = if (isDirect) senderId else null,
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        typingMap.remove(typingKey)
                    }
                    _typingPeers.value = typingMap.values.toList()
                }

                "CALL_INVITE" -> {
                    val callerId = json.optString("callerId")
                    if (callerId == _userProfile.value.id) return

                    val callId = json.optString("callId")
                    val isVideo = json.optBoolean("isVideo", false)
                    val callerName = json.optString("callerName", "مستخدم")
                    val callerColor = json.optLong("callerColor", 0xFF0EA5E9)
                    val callerAvatarUri = json.optString("callerAvatarUri").takeIf { it.isNotBlank() }
                    val callerAvatarBase64 = json.optString("callerAvatarBase64").takeIf { it.isNotBlank() }

                    val peer = Peer(
                        id = callerId,
                        name = callerName,
                        ip = senderIp,
                        avatarColor = callerColor,
                        avatarUri = callerAvatarUri,
                        avatarBase64 = callerAvatarBase64
                    )
                    peersMap[callerId] = peer

                    val incomingCall = ActiveCall(
                        callId = callId,
                        peer = peer,
                        isVideo = isVideo,
                        state = CallState.INCOMING_RINGING,
                        isSpeakerOn = audioEngine.isSpeakerOn.value,
                        isMicMuted = audioEngine.isMuted.value,
                        isCameraOff = videoEngine.isCameraOff.value,
                        isFrontCamera = videoEngine.isFrontCamera.value
                    )

                    // Busy-state protection: NEVER clobber an active call with a new invite
                    val currentCall = _activeCall.value
                    when {
                        // In a live call -> offer call waiting (keep the current call intact)
                        currentCall != null && currentCall.state == CallState.CONNECTED -> {
                            _callWaitingInvite.value = incomingCall
                            callToneManager.playIncomingRingtone()
                            scheduleWaitingRingTimeout(callId)
                        }
                        // Outgoing/incoming already ringing or a group call is live -> busy
                        currentCall != null || _activeGroupCall.value != null -> {
                            scope.launch {
                                try {
                                    val busyJson = JSONObject().apply {
                                        put("type", "CALL_BUSY")
                                        put("callId", callId)
                                        put("reason", "المستخدم مشغول في مكالمة أخرى")
                                    }
                                    sendJsonToIp(busyJson, senderIp)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }
                        // Free -> normal incoming call flow
                        else -> {
                            // Stop the room open mic before accepting the incoming call
                            suspendRoomVoiceForCall()
                            _activeCall.value = incomingCall

                            // RULE 3: incoming ringtone + repeating haptics; NEVER any audio channel
                            callToneManager.playIncomingRingtone()
                            scheduleIncomingRingTimeout(callId)
                            maybeShowIncomingCallNotification(callerName, callId, isVideo)
                        }
                    }
                }

                "CALL_ACCEPT" -> {
                    val callId = json.optString("callId")
                    val acceptorAvatarBase64 = json.optString("acceptorAvatarBase64").takeIf { it.isNotBlank() }
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId) {
                        ringingTimeoutJob?.cancel()
                        callToneManager.stopAll()
                        callToneManager.playConnectedChime()

                        val updatedPeer = if (acceptorAvatarBase64 != null) {
                            currentCall.peer.copy(avatarBase64 = acceptorAvatarBase64)
                        } else currentCall.peer

                        val updatedCall = currentCall.copy(
                            peer = updatedPeer,
                            state = CallState.CONNECTED,
                            startTime = System.currentTimeMillis()
                        )
                        _activeCall.value = updatedCall
                        audioEngine.authorizeSession(callSessionKey(callId))
                        audioEngine.startAudioRecording(_userProfile.value.id, "call_$callId", callSessionKey(callId))
                        if (currentCall.isVideo) {
                            videoEngine.startVideoReceiver()
                        }
                        startCallQualityMonitor(callId, currentCall.peer.ip, currentCall.isVideo)
                    }
                }

                "CALL_CONTROL" -> {
                    val callId = json.optString("callId")
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId) {
                        var updatedCall = currentCall
                        if (json.has("isMuted")) {
                            updatedCall = updatedCall.copy(isRemoteMuted = json.optBoolean("isMuted"))
                        }
                        if (json.has("isCameraOff")) {
                            updatedCall = updatedCall.copy(isRemoteCameraOff = json.optBoolean("isCameraOff"))
                        }
                        if (json.has("isScreenSharing")) {
                            val isScreen = json.optBoolean("isScreenSharing")
                            val appName = json.optString("screenSharedAppName").takeIf { it.isNotBlank() }
                            updatedCall = updatedCall.copy(
                                isScreenSharing = isScreen,
                                screenSharedAppName = appName
                            )
                        }
                        _activeCall.value = updatedCall
                    }
                }

                "CALL_END" -> {
                    val callId = json.optString("callId")
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId) {
                        _activeCall.value = null
                        stopCallQualityMonitor()

                        ringingTimeoutJob?.cancel()
                        incomingRingTimeoutJob?.cancel()
                        waitingRingTimeoutJob?.cancel()
                        _callWaitingInvite.value = null
                        callToneManager.stopAll()
                        callToneManager.playDisconnectedTone()

                        audioEngine.revokeSession()
                        audioEngine.stopAudioRecording()
                        videoEngine.stopCameraStream()
                        resumeRoomVoiceAfterCall()
                    } else {
                        // The waiting invite's caller may have hung up
                        val waiting = _callWaitingInvite.value
                        if (waiting != null && waiting.callId == callId) {
                            declineWaitingCall(reason = "أنهى المتصل المحاولة")
                        }
                    }
                }

                "CALL_BUSY" -> {
                    val callId = json.optString("callId")
                    val reason = json.optString("reason", "المستخدم مشغول في مكالمة أخرى")
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId && currentCall.state == CallState.OUTGOING_RINGING) {
                        _activeCall.value = null
                        stopCallQualityMonitor()

                        ringingTimeoutJob?.cancel()
                        // Stop the ringback, then signal the busy cadence to the caller
                        callToneManager.stopAll()
                        callToneManager.playDisconnectedTone()
                        audioEngine.removeTarget(currentCall.peer.id)
                        videoEngine.removeTarget(currentCall.peer.id)
                        resumeRoomVoiceAfterCall()

                        _callEndedReason.tryEmit(reason)
                    }
                }

                "CALL_PING" -> {
                    val callId = json.optString("callId")
                    val sentT = json.optLong("t", 0L)
                    val isVideo = json.optBoolean("isVideo", false)
                    if (sentT > 0) {
                        scope.launch {
                            try {
                                val replyPong = JSONObject().apply {
                                    put("type", "CALL_PONG")
                                    put("callId", callId)
                                    put("t", sentT)
                                    put("isVideo", isVideo)
                                    put("replyT", System.currentTimeMillis())
                                }
                                sendJsonToIp(replyPong, senderIp)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }

                "CALL_PONG" -> {
                    val callId = json.optString("callId")
                    val sentT = json.optLong("t", 0L)
                    val isVideo = json.optBoolean("isVideo", false)
                    if (sentT > 0) {
                        handleCallPong(callId, sentT, isVideo)
                    }
                }

                "ROOM_INVITE" -> {
                    val inviterId = json.optString("inviterId")
                    if (inviterId == _userProfile.value.id) return

                    val roomId = json.optString("roomId")
                    val roomName = json.optString("roomName", "غرفة جديدة")
                    val roomDesc = json.optString("roomDescription", "")
                    val inviterName = json.optString("inviterName", "مستخدم")
                    val inviterColor = json.optLong("inviterColor", 0xFF6750A4)
                    val maxCapacity = json.optInt("maxCapacity", 10)
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    val invitation = com.example.model.RoomInvitation(
                        roomId = roomId,
                        roomName = roomName,
                        roomDescription = roomDesc,
                        inviterId = inviterId,
                        inviterName = inviterName,
                        inviterColor = inviterColor,
                        maxCapacity = maxCapacity,
                        timestamp = timestamp
                    )

                    _incomingRoomInvites.tryEmit(invitation)
                }

                "GROUP_CALL_START" -> {
                    val initiatorId = json.optString("initiatorId")
                    if (initiatorId == _userProfile.value.id) return

                    val callId = json.optString("callId")
                    val roomId = json.optString("roomId")
                    val roomName = json.optString("roomName", "غرفة")
                    val initiatorName = json.optString("initiatorName", "مستخدم")
                    val initiatorColor = json.optLong("initiatorColor", 0xFF6750A4)
                    val initiatorAvatarBase64 = json.optString("initiatorAvatarBase64").takeIf { it.isNotBlank() }
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    val invite = GroupCallInvitation(
                        callId = callId,
                        roomId = roomId,
                        roomName = roomName,
                        initiatorId = initiatorId,
                        initiatorName = initiatorName,
                        initiatorColor = initiatorColor,
                        initiatorAvatarBase64 = initiatorAvatarBase64,
                        timestamp = timestamp
                    )

                    // RULE 1: invitation only — no audio channel is opened here.
                    // RULE 3: incoming ringtone + haptics, auto-stopped after 45s.
                    callToneManager.playIncomingRingtone()
                    groupRingTimeoutJob?.cancel()
                    groupRingTimeoutJob = scope.launch {
                        delay(CALL_RINGING_TIMEOUT_MS)
                        callToneManager.stopAll()
                    }

                    _incomingGroupCallInvite.tryEmit(invite)
                }

                "GROUP_CALL_JOIN" -> {
                    val peerId = json.optString("peerId")
                    if (peerId == _userProfile.value.id) return
                    val callId = json.optString("callId")
                    val roomId = json.optString("roomId")
                    val peerName = json.optString("peerName", "مستخدم")
                    val peerColor = json.optLong("peerColor", 0xFF0EA5E9)
                    val avatarBase64 = json.optString("avatarBase64").takeIf { it.isNotBlank() }
                    val peerIp = json.optString("peerIp", senderIp)

                    val joiningPeer = Peer(
                        id = peerId,
                        name = peerName,
                        ip = peerIp,
                        avatarColor = peerColor,
                        avatarBase64 = avatarBase64,
                        currentRoom = roomId
                    )
                    peersMap[peerId] = joiningPeer

                    val cur = _activeGroupCall.value
                    if (cur != null && cur.callId == callId) {
                        val currentList = cur.participants.filter { it.id != peerId }.toMutableList()
                        currentList.add(joiningPeer)
                        _activeGroupCall.value = cur.copy(participants = currentList)
                        updateRoomAudioTargets(roomId)
                    }
                }

                "GROUP_CALL_LEAVE" -> {
                    val peerId = json.optString("peerId")
                    if (peerId == _userProfile.value.id) return
                    val cur = _activeGroupCall.value
                    if (cur != null) {
                        val updatedList = cur.participants.filter { it.id != peerId }
                        _activeGroupCall.value = cur.copy(participants = updatedList)
                    }
                }

                "GROUP_CALL_CONTROL" -> {
                    // Update peer states in active group call
                }

                "MSG_ACK" -> {
                    val msgId = json.optString("msgId")
                    val receiverId = json.optString("receiverId")
                    if (msgId.isNotEmpty() && receiverId != _userProfile.value.id) {
                        _messageAcks.tryEmit(MsgAck(messageId = msgId, isRead = false))
                    }
                }

                "MSG_READ" -> {
                    val msgId = json.optString("msgId")
                    val readerId = json.optString("readerId")
                    if (msgId.isNotEmpty() && readerId != _userProfile.value.id) {
                        _messageAcks.tryEmit(MsgAck(messageId = msgId, isRead = true))
                    }
                }

                "CALL_REACTION" -> {
                    val senderId = json.optString("senderId")
                    if (senderId == _userProfile.value.id) return
                    val emoji = json.optString("emoji")
                    if (emoji.isNotEmpty()) {
                        _callReactions.tryEmit(
                            CallReactionEvent(
                                emoji = emoji,
                                senderId = senderId,
                                senderName = json.optString("senderName", "")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming packet", e)
        }
    }

    private suspend fun sendJsonPacket(json: JSONObject) {
        val bytes = LocalCryptoEngine.encryptJson(json.toString())
        if (broadcastSocket == null) {
            broadcastSocket = DatagramSocket().apply { broadcast = true }
        }

        // Send to Multicast Group
        try {
            val multicastAddr = InetAddress.getByName(NetworkUtils.MULTICAST_GROUP_IP)
            val packet = DatagramPacket(bytes, bytes.size, multicastAddr, NetworkUtils.DISCOVERY_PORT)
            broadcastSocket?.send(packet)
        } catch (e: Exception) {
            // Ignore
        }

        // Send to Subnet Broadcast
        try {
            val broadcastAddr = NetworkUtils.getBroadcastAddress(context)
            val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, NetworkUtils.DISCOVERY_PORT)
            broadcastSocket?.send(packet)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private suspend fun sendJsonToIp(json: JSONObject, ip: String) {
        val bytes = LocalCryptoEngine.encryptJson(json.toString())
        if (broadcastSocket == null) {
            broadcastSocket = DatagramSocket()
        }
        try {
            val targetAddr = InetAddress.getByName(ip)
            val packet = DatagramPacket(bytes, bytes.size, targetAddr, NetworkUtils.DISCOVERY_PORT)
            broadcastSocket?.send(packet)
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending to IP: $ip", e)
        }
    }

    private fun parseAndStorePeer(json: JSONObject, senderIp: String) {
        val id = json.optString("id")
        if (id == _userProfile.value.id || id.isEmpty()) return

        val name = json.optString("name", "مستخدم")
        val color = json.optLong("color", 0xFF0EA5E9)
        val avatarUri = json.optString("avatarUri").takeIf { it.isNotBlank() }
        val avatarBase64 = json.optString("avatarBase64").takeIf { it.isNotBlank() }
        val userStatusStr = json.optString("userStatus", UserPresenceStatus.ONLINE.name)
        val userStatus = try {
            UserPresenceStatus.valueOf(userStatusStr)
        } catch (e: Exception) {
            UserPresenceStatus.ONLINE
        }
        val statusMessage = json.optString("statusMessage", "")
        val room = json.optString("room", "general")
        val isSpeaking = json.optBoolean("isSpeaking", false)
        val isVideoActive = json.optBoolean("isVideoActive", false)
        val isMuted = json.optBoolean("isMuted", false)
        val device = json.optString("device", "")

        val peer = Peer(
            id = id,
            name = name,
            ip = senderIp,
            avatarColor = color,
            avatarUri = avatarUri,
            avatarBase64 = avatarBase64,
            userStatus = userStatus,
            statusMessage = statusMessage,
            currentRoom = room,
            isSpeaking = isSpeaking,
            isVideoActive = isVideoActive,
            isMuted = isMuted,
            lastSeen = System.currentTimeMillis(),
            deviceModel = device
        )
        peersMap[id] = peer
        _discoveredPeers.value = peersMap.values.toList()

        // If peer is in our room, update audio/video target
        if (room == _currentRoom.value) {
            try {
                val addr = InetAddress.getByName(senderIp)
                audioEngine.updateTarget(id, addr, NetworkUtils.AUDIO_PORT)
                videoEngine.updateTarget(id, addr, NetworkUtils.VIDEO_PORT)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun release() {
        scanJob?.cancel()
        discoveryJob?.cancel()
        heartbeatJob?.cancel()
        cleanupJob?.cancel()
        ringingTimeoutJob?.cancel()
        incomingRingTimeoutJob?.cancel()
        groupRingTimeoutJob?.cancel()
        typingMap.clear()
        _typingPeers.value = emptyList()
        callToneManager.stopAll()
        ScreenCaptureService.stop(context)
        ScreenCaptureService.onFrameCaptured = null
        ScreenCaptureService.onShareStopped = null
        multicastSocket?.close()
        multicastSocket = null
        broadcastSocket?.close()
        broadcastSocket = null
        audioEngine.release()
        videoEngine.release()
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
