package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.ActiveCall
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
        private const val PEER_TIMEOUT_MS = 8000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var multicastLock: WifiManager.MulticastLock? = null

    val audioEngine = AudioEngine(context)
    val videoEngine = VideoEngine(context)
    val fileTransferEngine = FileTransferEngine(context)
    val voiceNoteRecorder = VoiceNoteRecorder(context)
    val voiceNotePlayer = VoiceNotePlayer(context)

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

    private var callQualityJob: Job? = null
    private val _callSignalInfo = MutableStateFlow<PeerSignalInfo?>(null)
    val callSignalInfo = _callSignalInfo.asStateFlow()

    private var lastMeasuredLatency = 12L
    private var lastMeasuredJitter = 2L

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

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
                        peersMap[peer.id] = peer
                    }
                }
                _discoveredPeers.value = peersMap.values.toList()
                updateRoomAudioTargets(_currentRoom.value)
            }
        }
    }

    fun stop() {
        scanJob?.cancel()
        heartbeatJob?.cancel()
        discoveryJob?.cancel()
        cleanupJob?.cancel()
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
                        val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
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
                peersMap.entries.removeIf { (_, peer) ->
                    val isExpired = (now - peer.lastSeen) > PEER_TIMEOUT_MS
                    if (isExpired) peersChanged = true
                    isExpired
                }
                if (peersChanged) {
                    _discoveredPeers.value = peersMap.values.toList()
                    updateRoomAudioTargets(_currentRoom.value)
                }

                // Cleanup expired typing indicators (older than 3.5 seconds)
                var typingChanged = false
                typingMap.entries.removeIf { (_, typingPeer) ->
                    val isExpired = (now - typingPeer.timestamp) > 3500L
                    if (isExpired) typingChanged = true
                    isExpired
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
        messageType: MessageType = MessageType.TEXT
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
            imageBase64 = imageBase64
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

    /**
     * Initiates a 1-to-1 Call with another peer.
     */
    fun startCall(peer: Peer, isVideo: Boolean) {
        val callId = UUID.randomUUID().toString()
        _activeCall.value = ActiveCall(
            callId = callId,
            peer = peer,
            isVideo = isVideo,
            state = CallState.OUTGOING_RINGING
        )

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
                put("callerIp", _localIp.value)
            }
            sendJsonToIp(json, peer.ip)
        }
    }

    /**
     * Answers an incoming call.
     */
    fun acceptCall() {
        val currentCall = _activeCall.value ?: return
        val updatedCall = currentCall.copy(
            state = CallState.CONNECTED,
            startTime = System.currentTimeMillis()
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

        audioEngine.startAudioPlayback()
        audioEngine.startAudioRecording(_userProfile.value.id, "call_${currentCall.callId}")
        if (currentCall.isVideo) {
            videoEngine.startVideoReceiver()
        }
        startCallQualityMonitor(currentCall.callId, currentCall.peer.ip, currentCall.isVideo)

        scope.launch {
            val json = JSONObject().apply {
                put("type", "CALL_ACCEPT")
                put("callId", currentCall.callId)
                put("peerId", _userProfile.value.id)
            }
            sendJsonToIp(json, currentCall.peer.ip)
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

        audioEngine.stopAudioRecording()
        videoEngine.stopCameraStream()
        audioEngine.removeTarget(currentCall.peer.id)
        videoEngine.removeTarget(currentCall.peer.id)

        // Restore room audio targets
        updateRoomAudioTargets(_currentRoom.value)

        scope.launch {
            val json = JSONObject().apply {
                put("type", "CALL_END")
                put("callId", currentCall.callId)
            }
            sendJsonToIp(json, currentCall.peer.ip)
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
                    val id = json.optString("id")
                    if (id == _userProfile.value.id || id.isEmpty()) return

                    val name = json.optString("name", "مستخدم")
                    val color = json.optLong("color", 0xFF0EA5E9)
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

                    // If sender probed during subnet sweep, immediately reply so scanner discovers this device
                    if (json.optBoolean("isScanProbe", false)) {
                        scope.launch {
                            try {
                                val replyJson = JSONObject().apply {
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
                                }
                                sendJsonToIp(replyJson, senderIp)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }

                    // If peer is in our room, update target
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
                    val msgSenderIp = json.optString("senderIp", senderIp)

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
                            MessageType.valueOf(json.optString("msgType", MessageType.TEXT.name))
                        } catch (e: Exception) {
                            if (fileId != null) MessageType.FILE else MessageType.TEXT
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

                    val peer = Peer(
                        id = callerId,
                        name = callerName,
                        ip = senderIp,
                        avatarColor = callerColor
                    )
                    peersMap[callerId] = peer

                    _activeCall.value = ActiveCall(
                        callId = callId,
                        peer = peer,
                        isVideo = isVideo,
                        state = CallState.INCOMING_RINGING
                    )
                }

                "CALL_ACCEPT" -> {
                    val callId = json.optString("callId")
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId) {
                        val updatedCall = currentCall.copy(
                            state = CallState.CONNECTED,
                            startTime = System.currentTimeMillis()
                        )
                        _activeCall.value = updatedCall
                        audioEngine.startAudioPlayback()
                        audioEngine.startAudioRecording(_userProfile.value.id, "call_$callId")
                        if (currentCall.isVideo) {
                            videoEngine.startVideoReceiver()
                        }
                        startCallQualityMonitor(callId, currentCall.peer.ip, currentCall.isVideo)
                    }
                }

                "CALL_END" -> {
                    val callId = json.optString("callId")
                    val currentCall = _activeCall.value
                    if (currentCall != null && currentCall.callId == callId) {
                        _activeCall.value = null
                        stopCallQualityMonitor()
                        audioEngine.stopAudioRecording()
                        videoEngine.stopCameraStream()
                        updateRoomAudioTargets(_currentRoom.value)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming packet", e)
        }
    }

    private suspend fun sendJsonPacket(json: JSONObject) {
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
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
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
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

    fun release() {
        scanJob?.cancel()
        discoveryJob?.cancel()
        heartbeatJob?.cancel()
        cleanupJob?.cancel()
        typingMap.clear()
        _typingPeers.value = emptyList()
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
