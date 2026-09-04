package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.ThemeMode
import com.example.data.ThemePreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.RoomEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.BlockedPeerEntity
import com.example.model.ActiveCall
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.model.Peer
import com.example.model.RoomInfo
import com.example.model.RoomInvitation
import com.example.model.UserProfile
import com.example.model.UserPresenceStatus
import com.example.network.LocalP2PEngine
import com.example.utils.CallNotificationReceiver
import com.example.utils.LocalNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

enum class AppTab {
    ROOMS,
    PEERS,
    CHAT,
    NETWORK
}

sealed interface AuthState {
    object Loading : AuthState
    object LoggedOut : AuthState
    data class LoggedIn(val profile: UserProfile) : AuthState
}

sealed interface NetworkStatusState {
    object Connected : NetworkStatusState
    data class Unstable(val reason: String) : NetworkStatusState
    object Disconnected : NetworkStatusState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val engine = LocalP2PEngine(application.applicationContext)

    init {
        // Notifications: channels + incoming-call Accept/Decline actions
        LocalNotificationManager.ensureChannels(application.applicationContext)
        CallNotificationReceiver.handler = { accept ->
            if (accept) acceptCall() else endCall()
        }
    }

    private val _networkStatus = MutableStateFlow<NetworkStatusState>(NetworkStatusState.Connected)
    val networkStatus = _networkStatus.asStateFlow()

    private val _isHotspotDataWarning = MutableStateFlow(false)
    val isHotspotDataWarning = _isHotspotDataWarning.asStateFlow()
    private var hotspotWarningDismissed = false

    fun dismissHotspotDataWarning() {
        hotspotWarningDismissed = true
        _isHotspotDataWarning.value = false
    }

    private val _isNotificationMuted = MutableStateFlow(false)
    val isNotificationMuted = _isNotificationMuted.asStateFlow()

    fun toggleNotificationMute() {
        _isNotificationMuted.value = !_isNotificationMuted.value
    }

    private val db = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "offline_p2p_chat.db"
    )
        .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10)
        .fallbackToDestructiveMigration()
        .build()

    private val chatDao = db.chatDao()

    // Blocked Peers list (S-02)
    private val _blockedPeers = MutableStateFlow<List<BlockedPeerEntity>>(emptyList())
    val blockedPeers: StateFlow<List<BlockedPeerEntity>> = _blockedPeers.asStateFlow()

    fun blockPeer(peerId: String, peerName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.insertBlockedPeer(BlockedPeerEntity(peerId = peerId, peerName = peerName))
        }
    }

    fun unblockPeer(peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteBlockedPeer(peerId)
        }
    }

    fun isPeerBlocked(peerId: String): Boolean {
        return _blockedPeers.value.any { it.peerId == peerId }
    }

    // Unread message counts per target (peer/room) for notification badges
    val unreadCounts: StateFlow<Map<String, Int>> = chatDao.getUnreadCounts()
        .map { list -> list.associate { it.targetRoomOrPeerId to it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // In-memory message drafts per conversation target (U-01)
    private val _messageDrafts = androidx.compose.runtime.mutableStateMapOf<String, String>()

    fun getDraft(targetId: String): String = _messageDrafts[targetId] ?: ""

    fun saveDraft(targetId: String, draft: String) {
        if (draft.isBlank()) {
            _messageDrafts.remove(targetId)
        } else {
            _messageDrafts[targetId] = draft
        }
    }

    // Quoted reply state (U-05)
    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    fun setReplyingTo(message: ChatMessage?) {
        _replyingToMessage.value = message
    }

    private val themePreferencesRepository = ThemePreferencesRepository(application.applicationContext)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _selectedTab = MutableStateFlow(AppTab.ROOMS)
    val selectedTab = _selectedTab.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomInfo>>(
        listOf(
            RoomInfo("general", "الغرفة العامة", "المحادثة والصوت العام لجميع المتصلين", isDefault = true, maxCapacity = 15),
            RoomInfo("voice_1", "غرفة الصوت 1", "قناة صوتية للحديث والمناقشة", iconName = "mic", maxCapacity = 8),
            RoomInfo("voice_2", "غرفة الصوت 2", "قناة صوتية ثانوية", iconName = "mic", maxCapacity = 8),
            RoomInfo("gaming", "الألعاب والترفيه", "قناة خاصة للألعاب والمجموعات", iconName = "sports_esports", maxCapacity = 6)
        )
    )
    val rooms = _rooms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inRoomMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val inRoomMessages = _inRoomMessages.asStateFlow()

    private val _currentChatTarget = MutableStateFlow("general")
    val currentChatTarget = _currentChatTarget.asStateFlow()

    private val _currentChatIsDirect = MutableStateFlow(false)
    val currentChatIsDirect = _currentChatIsDirect.asStateFlow()

    private val _currentChatPeer = MutableStateFlow<Peer?>(null)
    val currentChatPeer = _currentChatPeer.asStateFlow()

    private val _activeRoomInvitation = MutableStateFlow<RoomInvitation?>(null)
    val activeRoomInvitation = _activeRoomInvitation.asStateFlow()

    private val _roomJoinError = MutableStateFlow<String?>(null)
    val roomJoinError = _roomJoinError.asStateFlow()

    private val _isGroupCallActive = MutableStateFlow(false)
    val isGroupCallActive = _isGroupCallActive.asStateFlow()

    // Forwarding state flows from engine
    val discoveredPeers = engine.discoveredPeers
    val typingPeers = engine.typingPeers
    val currentRoom = engine.currentRoom
    val userProfile = engine.userProfile
    val localIp = engine.localIp
    val activeCall = engine.activeCall
    val callWaitingInvite = engine.callWaitingInvite
    val callEndedReason = engine.callEndedReason
    val activeGroupCall = engine.activeGroupCall
    val roomActiveGroupCalls = engine.roomActiveGroupCalls
    val incomingGroupCallInvite = engine.incomingGroupCallInvite
    val callSignalInfo = engine.callSignalInfo
    val micLevel = engine.audioEngine.micLevel
    val isMuted = engine.audioEngine.isMuted
    val isSpeakerOn = engine.audioEngine.isSpeakerOn
    val isOpenMic = engine.audioEngine.isOpenMic
    val isPushToTalkActive = engine.audioEngine.isPushToTalkActive
    val isVideoStreaming = engine.videoEngine.isStreaming
    val remoteVideoFrames = engine.videoEngine.remoteVideoFrames
    val downloadProgressMap = engine.fileTransferEngine.downloadProgressMap
    val downloadingIds = engine.fileTransferEngine.downloadingIds

    // Voice Note Recording Flows
    val isVoiceRecording = engine.voiceNoteRecorder.isRecording
    val voiceRecordingDuration = engine.voiceNoteRecorder.durationSeconds
    val voiceRecordingAmplitudes = engine.voiceNoteRecorder.amplitudes
    val voiceRecordingCurrentAmp = engine.voiceNoteRecorder.currentAmplitude

    // Voice Note Playback Flows
    val isPlayingVoiceNote = engine.voiceNotePlayer.isPlaying
    val playingVoiceMessageId = engine.voiceNotePlayer.currentlyPlayingId
    val voiceNoteProgress = engine.voiceNotePlayer.playbackProgress
    val voiceNoteCurrentMs = engine.voiceNotePlayer.currentPositionMs
    val voiceNoteDurationMs = engine.voiceNotePlayer.durationMs
    val playbackSpeed = engine.voiceNotePlayer.playbackSpeed

    /** Cycles voice note playback speed 1.0x -> 1.5x -> 2.0x -> 1.0x. */
    fun cyclePlaybackSpeed() {
        engine.voiceNotePlayer.cyclePlaybackSpeed()
    }

    // Floating emoji reactions fired during active calls
    val callReactions = engine.callReactions

    fun sendCallReaction(emoji: String) {
        engine.sendCallReaction(emoji)
    }

    // Wi-Fi Local Network Scan Flows
    val isScanning = engine.isScanning
    val scanProgress = engine.scanProgress
    val scannedIpCount = engine.scannedIpCount
    val currentScanSubnet = engine.currentScanSubnet
    val lastScanTime = engine.lastScanTime

    fun rescanNetwork() {
        engine.triggerNetworkScan()
    }

    init {
        // Monitor network connection and signal stability
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val ip = engine.localIp.value
                val isNetworkActive = com.example.network.NetworkUtils.isLocalNetworkActive(getApplication<Application>().applicationContext)
                val signal = engine.callSignalInfo.value

                val newStatus = when {
                    !isNetworkActive || ip == "127.0.0.1" -> NetworkStatusState.Disconnected
                    signal?.quality == com.example.model.ConnectionQualityLevel.POOR ||
                    signal?.quality == com.example.model.ConnectionQualityLevel.DISCONNECTED -> 
                        NetworkStatusState.Unstable("اتصال غير مستقر (إشارة ضعيفة)")
                    else -> NetworkStatusState.Connected
                }
                _networkStatus.value = newStatus

                val isMobileData = com.example.network.NetworkUtils.isMobileDataActive(getApplication<Application>().applicationContext)
                val isHotspot = com.example.network.NetworkUtils.isHotspotApActive()
                if (isMobileData && isHotspot) {
                    if (!hotspotWarningDismissed) {
                        _isHotspotDataWarning.value = true
                    }
                } else {
                    hotspotWarningDismissed = false
                    _isHotspotDataWarning.value = false
                }

                delay(3000L)
            }
        }

        // Collect saved theme mode preference from DataStore
        viewModelScope.launch {
            themePreferencesRepository.themeModeFlow.collectLatest { mode ->
                _themeMode.value = mode
            }
        }

        // Initialize user session and rooms
        viewModelScope.launch(Dispatchers.IO) {
            checkUserSession()
            initDefaultRooms()
            observeRoomsFromDb()
        }

        // Listen for incoming messages from P2P engine and store in DB
        viewModelScope.launch(Dispatchers.IO) {
            engine.incomingMessages.collectLatest { msg ->
                chatDao.insertMessage(
                    ChatMessageEntity(
                        id = msg.id,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        senderColor = msg.senderColor,
                        targetRoomOrPeerId = msg.targetRoomOrPeerId,
                        isDirect = msg.isDirect,
                        content = msg.content,
                        timestamp = msg.timestamp,
                        isMine = false,
                        messageType = msg.messageType.name,
                        imageBase64 = msg.imageBase64,
                        fileId = msg.fileId,
                        fileName = msg.fileName,
                        fileSize = msg.fileSize,
                        mimeType = msg.mimeType,
                        durationSeconds = msg.durationSeconds,
                        senderIp = msg.senderIp,
                        localFilePath = msg.localFilePath,
                        isDownloaded = msg.isDownloaded,
                        isRead = msg.isRead,
                        replyToId = msg.replyToId,
                        replyToSender = msg.replyToSender,
                        replyToText = msg.replyToText
                    )
                )

                // Confirm delivery back to the sender (double gray check)
                engine.sendMsgAck(msg)

                // Heads-up message notification while the app is backgrounded and notifications are not muted
                if (!LocalNotificationManager.isAppInForeground && !_isNotificationMuted.value) {
                    val preview = when (msg.messageType) {
                        MessageType.IMAGE -> "📷 أرسل صورة"
                        MessageType.VOICE_NOTE -> "🎙️ أرسل رسالة صوتية"
                        MessageType.FILE -> "📄 ${msg.fileName ?: "ملف"}"
                        else -> msg.content
                    }
                    val notificationTarget = if (msg.isDirect) msg.senderId else msg.targetRoomOrPeerId
                    LocalNotificationManager.showMessageNotification(
                        application.applicationContext,
                        notificationTarget,
                        msg.isDirect,
                        msg.senderName,
                        preview
                    )
                }

                // Auto-download voice notes and images from peer for instant listening/viewing
                if ((msg.messageType == MessageType.VOICE_NOTE || msg.messageType == MessageType.IMAGE) && msg.fileId != null && msg.senderIp != null) {
                    downloadMessageFile(msg)
                }
            }
        }

        // Upgrade delivery ticks of our own outgoing messages from peer ACKs
        viewModelScope.launch(Dispatchers.IO) {
            engine.messageAcks.collectLatest { ack ->
                val status = if (ack.isRead) com.example.model.DELIVERY_READ else com.example.model.DELIVERY_DELIVERED
                chatDao.upgradeMessageDeliveryStatus(ack.messageId, status)
            }
        }

        // Listen for incoming room invitations
        viewModelScope.launch(Dispatchers.IO) {
            engine.incomingRoomInvites.collectLatest { invite ->
                _activeRoomInvitation.value = invite
            }
        }

        // Listen for real-time remote message edits
        viewModelScope.launch(Dispatchers.IO) {
            engine.incomingMessageEdits.collectLatest { (messageId, newContent) ->
                chatDao.updateMessageContent(messageId, newContent)
            }
        }

        // Observe blocked peers (S-02) and sync with engine
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.getAllBlockedPeers().collectLatest { blockedList ->
                _blockedPeers.value = blockedList
                engine.setBlockedPeers(blockedList.map { it.peerId }.toSet())
            }
        }

        // Observe messages for active room and current chat target
        observeCurrentChatMessages()
        observeActiveRoomMessages()
    }

    /** Message ids already confirmed as "read" to their senders (guard against ACK spam). */
    private val readReceiptsSent = java.util.Collections.synchronizedSet(
        LinkedHashSet<String>()
    )

    private fun observeCurrentChatMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentChatTarget.collectLatest { targetId ->
                chatDao.markMessagesAsRead(targetId)
                chatDao.getMessagesForTarget(targetId).collectLatest { entities ->
                    _messages.value = entities.map {
                        ChatMessage(
                            id = it.id,
                            senderId = it.senderId,
                            senderName = it.senderName,
                            senderColor = it.senderColor,
                            targetRoomOrPeerId = it.targetRoomOrPeerId,
                            isDirect = it.isDirect,
                            content = it.content,
                            timestamp = it.timestamp,
                            isMine = it.isMine,
                            messageType = try { MessageType.valueOf(it.messageType) } catch (e: Exception) { MessageType.TEXT },
                            imageBase64 = it.imageBase64,
                            fileId = it.fileId,
                            fileName = it.fileName,
                            fileSize = it.fileSize,
                            mimeType = it.mimeType,
                            durationSeconds = it.durationSeconds,
                            senderIp = it.senderIp,
                            localFilePath = it.localFilePath,
                            isDownloaded = it.isDownloaded || it.isMine,
                            isRead = it.isRead,
                            isEdited = it.isEdited,
                            deliveryStatus = it.deliveryStatus,
                            replyToId = it.replyToId,
                            replyToSender = it.replyToSender,
                            replyToText = it.replyToText
                        )
                    }
                    sendReadReceipts(entities)
                }
            }
        }
    }

    private fun observeActiveRoomMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            engine.currentRoom.collectLatest { roomId ->
                if (roomId.isNotBlank()) {
                    chatDao.markMessagesAsRead(roomId)
                }
                chatDao.getMessagesForTarget(roomId).collectLatest { entities ->
                    _inRoomMessages.value = entities.map {
                        ChatMessage(
                            id = it.id,
                            senderId = it.senderId,
                            senderName = it.senderName,
                            senderColor = it.senderColor,
                            targetRoomOrPeerId = it.targetRoomOrPeerId,
                            isDirect = it.isDirect,
                            content = it.content,
                            timestamp = it.timestamp,
                            isMine = it.isMine,
                            messageType = try { MessageType.valueOf(it.messageType) } catch (e: Exception) { MessageType.TEXT },
                            imageBase64 = it.imageBase64,
                            fileId = it.fileId,
                            fileName = it.fileName,
                            fileSize = it.fileSize,
                            mimeType = it.mimeType,
                            durationSeconds = it.durationSeconds,
                            senderIp = it.senderIp,
                            localFilePath = it.localFilePath,
                            isDownloaded = it.isDownloaded || it.isMine,
                            isRead = it.isRead,
                            isEdited = it.isEdited,
                            deliveryStatus = it.deliveryStatus,
                            replyToId = it.replyToId,
                            replyToSender = it.replyToSender,
                            replyToText = it.replyToText
                        )
                    }
                    sendReadReceipts(entities)
                }
            }
        }
    }

    /**
     * Marks displayed incoming messages as READ back to their senders
     * (double blue check). The in-memory set prevents duplicate receipts.
     */
    private fun sendReadReceipts(entities: List<ChatMessageEntity>) {
        synchronized(readReceiptsSent) {
            if (readReceiptsSent.size > 5000) {
                val toRemove = readReceiptsSent.take(1000)
                readReceiptsSent.removeAll(toRemove.toSet())
            }
        }
        entities.filter { !it.isMine && !it.senderIp.isNullOrBlank() && it.deliveryStatus < 2 }
            .forEach { entity ->
                val shouldSend = synchronized(readReceiptsSent) {
                    readReceiptsSent.add(entity.id)
                }
                if (shouldSend) {
                    engine.sendMsgRead(entity.id, entity.senderIp!!)
                }
            }
    }

    private suspend fun checkUserSession() {
        val activeUser = chatDao.getActiveUser() ?: chatDao.getLastLoggedInUser()
        if (activeUser != null) {
            val userStatus = try {
                UserPresenceStatus.valueOf(activeUser.userStatus)
            } catch (e: Exception) {
                UserPresenceStatus.ONLINE
            }
            val profile = UserProfile(
                id = activeUser.userId,
                username = activeUser.username,
                displayName = activeUser.displayName,
                avatarColor = activeUser.avatarColor,
                userStatus = userStatus,
                statusMessage = activeUser.statusMessage,
                bio = activeUser.bio,
                avatarUri = activeUser.avatarUri,
                isLoggedIn = true
            )
            engine.setUserProfile(profile)
            _authState.value = AuthState.LoggedIn(profile)
            engine.start()
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    private suspend fun initDefaultRooms() {
        val existing = chatDao.getAllRoomsList()
        if (existing.isEmpty()) {
            _rooms.value.forEach { room ->
                chatDao.insertRoom(
                    RoomEntity(
                        id = room.id,
                        name = room.name,
                        description = room.description,
                        iconName = room.iconName,
                        isDefault = room.isDefault,
                        creatorId = room.creatorId,
                        creatorName = room.creatorName,
                        maxCapacity = room.maxCapacity,
                        isPrivate = room.isPrivate
                    )
                )
            }
        }
    }

    private fun observeRoomsFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.getAllRooms().collectLatest { roomEntities ->
                if (roomEntities.isNotEmpty()) {
                    _rooms.value = roomEntities.map {
                        RoomInfo(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            isDefault = it.isDefault,
                            iconName = it.iconName,
                            creatorId = it.creatorId,
                            creatorName = it.creatorName,
                            maxCapacity = it.maxCapacity,
                            isPrivate = it.isPrivate,
                            passwordHash = it.passwordHash
                        )
                    }
                }
            }
        }
    }

    // --- Authentication Actions ---

    fun login(usernameInput: String, passwordInput: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            val username = usernameInput.trim().lowercase()
            val password = passwordInput.trim()

            if (username.isBlank() || password.isBlank()) {
                _authError.value = "يرجى إدخال اسم المستخدم وكلمة المرور"
                return@launch
            }

            val user = chatDao.getUserByUsername(username)
            if (user == null) {
                _authError.value = "اسم المستخدم غير موجود. يمكنك إنشاء حساب جديد."
                return@launch
            }

            val passwordHash = hashPassword(password)
            if (user.passwordHash != passwordHash) {
                _authError.value = "كلمة المرور غير صحيحة"
                return@launch
            }

            chatDao.clearActiveSessions()
            chatDao.setActiveSession(username)

            val userStatus = try {
                UserPresenceStatus.valueOf(user.userStatus)
            } catch (e: Exception) {
                UserPresenceStatus.ONLINE
            }
            val base64 = user.avatarBase64 ?: uriToBase64(user.avatarUri)
            val profile = UserProfile(
                id = user.userId,
                username = user.username,
                displayName = user.displayName,
                avatarColor = user.avatarColor,
                userStatus = userStatus,
                statusMessage = user.statusMessage,
                bio = user.bio,
                avatarUri = user.avatarUri,
                avatarBase64 = base64,
                isLoggedIn = true
            )
            engine.setUserProfile(profile)
            _authState.value = AuthState.LoggedIn(profile)
            engine.start()
        }
    }

    fun register(usernameInput: String, passwordInput: String, displayNameInput: String, avatarColor: Long, avatarUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            val username = usernameInput.trim().lowercase()
            val password = passwordInput.trim()
            val displayName = displayNameInput.trim().ifBlank { username }

            if (username.length < 3) {
                _authError.value = "اسم المستخدم يجب ألا يقل عن 3 أحرف"
                return@launch
            }

            if (password.length < 4) {
                _authError.value = "كلمة المرور يجب ألا تقل عن 4 أحرف أو أرقام"
                return@launch
            }

            val existing = chatDao.getUserByUsername(username)
            if (existing != null) {
                _authError.value = "اسم المستخدم محجوز مسبقاً، يرجى اختيار اسم آخر"
                return@launch
            }

            chatDao.clearActiveSessions()
            val userId = UUID.randomUUID().toString().substring(0, 8)
            val passwordHash = hashPassword(password)
            val base64 = uriToBase64(avatarUri)
            val newUser = UserAccountEntity(
                userId = userId,
                username = username,
                displayName = displayName,
                passwordHash = passwordHash,
                avatarColor = avatarColor,
                userStatus = UserPresenceStatus.ONLINE.name,
                statusMessage = "متصل محلياً ومستعد للحديث",
                bio = "مستخدم في تطبيق LocalConnect",
                avatarUri = avatarUri,
                avatarBase64 = base64,
                createdAt = System.currentTimeMillis(),
                isActiveSession = true
            )
            chatDao.insertUser(newUser)

            val profile = UserProfile(
                id = userId,
                username = username,
                displayName = displayName,
                avatarColor = avatarColor,
                userStatus = UserPresenceStatus.ONLINE,
                statusMessage = "متصل محلياً ومستعد للحديث",
                bio = "مستخدم في تطبيق LocalConnect",
                avatarUri = avatarUri,
                avatarBase64 = base64,
                isLoggedIn = true
            )
            engine.setUserProfile(profile)
            _authState.value = AuthState.LoggedIn(profile)
            engine.start()
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearActiveSessions()
            engine.audioEngine.stopAllAudio()
            engine.videoEngine.stopCameraStream()
            _authState.value = AuthState.LoggedOut
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- UI Navigation & Room Actions ---

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    // In-memory verified room passwords for current session (S-01)
    private val verifiedRoomPasswords = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun isRoomPasswordVerified(roomId: String): Boolean {
        val room = _rooms.value.find { it.id == roomId } ?: return true
        if (room.passwordHash.isNullOrBlank()) return true
        return verifiedRoomPasswords.containsKey(roomId)
    }

    fun joinRoom(roomId: String, password: String? = null): Boolean {
        val targetRoom = _rooms.value.find { it.id == roomId }
        if (targetRoom != null) {
            val currentOccupancy = discoveredPeers.value.count { it.currentRoom == roomId } + 1
            if (currentOccupancy > targetRoom.maxCapacity) {
                _roomJoinError.value = "الغرفة ممتلئة (${targetRoom.maxCapacity}/${targetRoom.maxCapacity} عضو)"
                return false
            }

            // Check password protection (S-01)
            if (!targetRoom.passwordHash.isNullOrBlank()) {
                val isAlreadyVerified = verifiedRoomPasswords.containsKey(roomId)
                if (!isAlreadyVerified) {
                    if (password.isNullOrBlank()) {
                        _roomJoinError.value = "الغرفة محمية بكلمة مرور"
                        return false
                    }
                    val inputHash = hashPassword(password.trim())
                    if (inputHash != targetRoom.passwordHash) {
                        _roomJoinError.value = "كلمة المرور غير صحيحة"
                        return false
                    }
                    verifiedRoomPasswords[roomId] = inputHash
                }
            }
        }
        _roomJoinError.value = null
        engine.setRoom(roomId)
        _currentChatTarget.value = roomId
        _currentChatIsDirect.value = false
        _currentChatPeer.value = null
        return true
    }

    fun clearRoomJoinError() {
        _roomJoinError.value = null
    }

    fun createRoom(
        name: String,
        description: String,
        capacity: Int = 10,
        isPrivate: Boolean = false,
        password: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "room_" + UUID.randomUUID().toString().substring(0, 6)
            val currentProfile = userProfile.value
            val pHash = if (!password.isNullOrBlank()) hashPassword(password.trim()) else null
            val newRoomEntity = RoomEntity(
                id = id,
                name = name,
                description = description,
                iconName = "meeting_room",
                isDefault = false,
                creatorId = currentProfile.id,
                creatorName = currentProfile.displayName.ifBlank { currentProfile.username },
                maxCapacity = capacity,
                isPrivate = isPrivate,
                passwordHash = pHash
            )
            chatDao.insertRoom(newRoomEntity)
            if (pHash != null) {
                verifiedRoomPasswords[id] = pHash
            }
            joinRoom(id, password)
        }
    }

    fun invitePeerToRoom(peer: Peer, room: RoomInfo) {
        engine.sendRoomInvite(peer, room)
    }

    fun acceptRoomInvite(invitation: RoomInvitation) {
        _activeRoomInvitation.value = null
        joinRoom(invitation.roomId)
        _selectedTab.value = AppTab.ROOMS
    }

    fun declineRoomInvite() {
        _activeRoomInvitation.value = null
    }

    fun toggleGroupVoiceCall() {
        val newState = !_isGroupCallActive.value
        _isGroupCallActive.value = newState
        if (newState) {
            // RULE 1.2: room voice session opens only through this explicit toggle
            engine.startRoomVoiceSession()
        } else {
            engine.stopRoomVoiceSession()
        }
    }

    fun openDirectChat(peer: Peer) {
        _currentChatTarget.value = peer.id
        _currentChatIsDirect.value = true
        _currentChatPeer.value = peer
        _selectedTab.value = AppTab.CHAT
        LocalNotificationManager.cancelTargetNotification(getApplication(), peer.id)
    }

    fun openRoomChat(roomId: String) {
        _currentChatTarget.value = roomId
        _currentChatIsDirect.value = false
        _currentChatPeer.value = null
        _selectedTab.value = AppTab.CHAT
        LocalNotificationManager.cancelTargetNotification(getApplication(), roomId)
    }

    fun setUserStatus(status: UserPresenceStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            val updated = current.copy(userStatus = status)
            engine.setUserStatus(status)

            val activeUser = chatDao.getUserByUsername(current.username)
            if (activeUser != null) {
                chatDao.updateUserStatus(current.username, status.name)
            }
        }
    }

    private fun uriToBase64(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = android.net.Uri.parse(uriString)
            val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                val original = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
                val targetSize = 120
                val scaled = Bitmap.createScaledBitmap(original, targetSize, targetSize, true)
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                if (scaled != original) scaled.recycle()
                original.recycle()
                out.toByteArray()
            } ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun updateProfile(
        name: String,
        color: Long,
        status: String = "",
        bio: String = "",
        userStatus: UserPresenceStatus = userProfile.value.userStatus,
        avatarUri: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            val finalAvatarUri = avatarUri ?: current.avatarUri
            val finalAvatarBase64 = if (avatarUri != null && avatarUri != current.avatarUri) {
                uriToBase64(avatarUri)
            } else current.avatarBase64 ?: uriToBase64(finalAvatarUri)

            val updated = current.copy(
                displayName = name,
                avatarColor = color,
                userStatus = userStatus,
                statusMessage = status.ifBlank { current.statusMessage },
                bio = bio.ifBlank { current.bio },
                avatarUri = finalAvatarUri,
                avatarBase64 = finalAvatarBase64
            )
            engine.setUserProfile(updated)

            // Update in Room DB
            val activeUser = chatDao.getUserByUsername(current.username)
            if (activeUser != null) {
                chatDao.updateProfile(
                    username = current.username,
                    displayName = name,
                    avatarColor = color,
                    userStatus = userStatus.name,
                    statusMessage = status.ifBlank { activeUser.statusMessage },
                    bio = bio.ifBlank { activeUser.bio },
                    avatarUri = finalAvatarUri,
                    avatarBase64 = finalAvatarBase64
                )
            }
        }
    }

    fun manualConnectIp(ip: String) {
        engine.connectDirectIp(ip)
    }

    // --- Audio & Video Controls ---

    fun toggleMute() {
        engine.audioEngine.toggleMute()
        engine.broadcastPresence()
    }

    fun toggleSpeaker() {
        engine.audioEngine.toggleSpeaker()
    }

    fun toggleOpenMic() {
        engine.audioEngine.toggleOpenMic()
        engine.broadcastPresence()
    }

    fun setPushToTalk(pressed: Boolean) {
        engine.audioEngine.setPushToTalk(pressed)
        engine.broadcastPresence()
    }

    fun toggleCamera() {
        engine.videoEngine.switchCamera()
    }

    // --- Calling Actions & In-Call Media Routing ---

    fun startCall(peer: Peer, isVideo: Boolean) {
        engine.startCall(peer, isVideo)
    }

    fun acceptCall() {
        engine.acceptCall()
    }

    fun endCall() {
        engine.endCall()
    }

    fun toggleCallMic() {
        engine.toggleCallMic()
    }

    fun toggleCallCamera() {
        engine.toggleCallCamera()
    }

    fun switchCallCamera() {
        engine.switchCallCamera()
    }

    fun toggleCallSpeaker() {
        engine.toggleCallSpeaker()
    }

    fun setCallVolume(volume: Float) {
        engine.audioEngine.setCallVolume(volume)
    }

    val callVolume = engine.audioEngine.callVolume

    // --- RULE 2: Real MediaProjection screen sharing (foreground service) ---

    fun startRealScreenShare(appName: String, resultCode: Int, projectionData: android.content.Intent) {
        engine.startRealScreenShare(appName, resultCode, projectionData)
    }

    fun stopRealScreenShare() {
        engine.stopRealScreenShare()
    }

    fun dismissIncomingGroupCallRing() {
        engine.dismissIncomingGroupCallRing()
    }

    // --- Call waiting (incoming call while already in a CONNECTED call) ---

    fun acceptWaitingCall() {
        engine.acceptWaitingCall()
    }

    fun declineWaitingCall() {
        engine.declineWaitingCall()
    }

    /** Opens a chat screen tapped from a message notification. */
    fun openChatFromNotification(targetId: String, isDirect: Boolean) {
        LocalNotificationManager.cancelTargetNotification(getApplication(), targetId)
        if (isDirect) {
            val peer = discoveredPeers.value.find { it.id == targetId }
                ?: Peer(id = targetId, name = targetId, ip = "")
            openDirectChat(peer)
        } else {
            openRoomChat(targetId)
        }
    }

    // --- Group Video & Audio Call Actions ---

    fun startGroupVideoCall(roomId: String, roomName: String) {
        engine.startGroupVideoCall(roomId, roomName)
    }

    fun joinGroupVideoCall(invitation: com.example.model.GroupCallInvitation) {
        engine.joinGroupVideoCall(invitation)
    }

    fun leaveGroupVideoCall() {
        engine.leaveGroupVideoCall()
    }

    fun toggleGroupCallMic() {
        engine.toggleGroupCallMic()
    }

    fun toggleGroupCallCamera() {
        engine.toggleGroupCallCamera()
    }

    fun switchGroupCallCamera() {
        engine.switchGroupCallCamera()
    }

    fun toggleGroupCallSpeaker() {
        engine.toggleGroupCallSpeaker()
    }

    // --- Theme & Appearance Settings (DataStore Persistence) ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            themePreferencesRepository.toggleDarkMode(isDark)
        }
    }

    // --- Messaging Actions & Typing Indicator ---

    private var lastTypingSentTime = 0L
    private var typingTimeoutJob: Job? = null

    /**
     * Reports user typing activity to peers in the current chat or room.
     */
    fun onUserTyping(isTyping: Boolean) {
        typingTimeoutJob?.cancel()
        val targetId = _currentChatTarget.value
        val isDirect = _currentChatIsDirect.value

        if (!isTyping) {
            lastTypingSentTime = 0L
            engine.sendTypingStatus(target = targetId, isDirect = isDirect, isTyping = false)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastTypingSentTime > 1800L) {
            lastTypingSentTime = now
            engine.sendTypingStatus(target = targetId, isDirect = isDirect, isTyping = true)
        }

        // Auto-stop typing after 3.5 seconds of idle inactivity
        typingTimeoutJob = viewModelScope.launch {
            delay(3500L)
            lastTypingSentTime = 0L
            engine.sendTypingStatus(target = targetId, isDirect = isDirect, isTyping = false)
        }
    }

    fun sendMessage(text: String, bitmap: Bitmap?) {
        onUserTyping(false)
        if (bitmap != null) {
            viewModelScope.launch(Dispatchers.IO) {
                var scaledBmp: Bitmap? = null
                try {
                    val cacheFile = File(getApplication<android.app.Application>().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                    val maxDim = 1280
                    scaledBmp = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt()
                        val targetH = if (ratio >= 1f) (maxDim / ratio).toInt() else maxDim
                        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    } else {
                        null
                    }
                    val bmpToCompress = scaledBmp ?: bitmap
                    FileOutputStream(cacheFile).use { stream ->
                        bmpToCompress.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.flush()
                    }
                    val uri = Uri.fromFile(cacheFile)
                    val isDirect = _currentChatIsDirect.value
                    if (isDirect) {
                        val peer = _currentChatPeer.value ?: discoveredPeers.value.find { it.id == _currentChatTarget.value.removePrefix("peer_") || it.id == _currentChatTarget.value }
                        if (peer != null) {
                            sendDirectFile(peer, uri, text)
                        } else {
                            sendInRoomFile(uri, text)
                        }
                    } else {
                        sendInRoomFile(uri, text)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error sending image message", e)
                } finally {
                    if (scaledBmp != null && scaledBmp != bitmap) {
                        try {
                            scaledBmp.recycle()
                        } catch (_: Exception) {}
                    }
                }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val targetId = _currentChatTarget.value
            val isDirect = _currentChatIsDirect.value
            val currentProfile = userProfile.value

            val replying = _replyingToMessage.value
            val replyToId = replying?.id
            val replyToSender = replying?.senderName
            val replyToText = replying?.content?.take(120)
            _replyingToMessage.value = null
            saveDraft(targetId, "")

            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentProfile.id,
                senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                senderColor = currentProfile.avatarColor,
                targetRoomOrPeerId = targetId,
                isDirect = isDirect,
                content = text,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                messageType = MessageType.TEXT,
                replyToId = replyToId,
                replyToSender = replyToSender,
                replyToText = replyToText
            )

            // Save to DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderColor = msg.senderColor,
                    targetRoomOrPeerId = msg.targetRoomOrPeerId,
                    isDirect = msg.isDirect,
                    content = msg.content,
                    timestamp = msg.timestamp,
                    isMine = true,
                    messageType = msg.messageType.name,
                    replyToId = msg.replyToId,
                    replyToSender = msg.replyToSender,
                    replyToText = msg.replyToText
                )
            )

            // Send to Network
            if (isDirect) {
                val peer = _currentChatPeer.value
                if (peer != null) {
                    engine.sendDirectMessage(peer, msg)
                }
            } else {
                engine.sendRoomMessage(targetId, msg)
            }
        }
    }

    fun sendInRoomMessage(text: String, bitmap: Bitmap?) {
        onUserTyping(false)
        if (bitmap != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val cacheFile = File(getApplication<android.app.Application>().cacheDir, "img_${System.currentTimeMillis()}.jpg")
                    val stream = FileOutputStream(cacheFile)
                    val maxDim = 1280
                    val scaledBmp = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt()
                        val targetH = if (ratio >= 1f) (maxDim / ratio).toInt() else maxDim
                        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    } else {
                        bitmap
                    }
                    scaledBmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.flush()
                    stream.close()
                    val uri = Uri.fromFile(cacheFile)
                    sendInRoomFile(uri, text)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error sending room image", e)
                }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val roomId = currentRoom.value
            val currentProfile = userProfile.value

            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentProfile.id,
                senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                senderColor = currentProfile.avatarColor,
                targetRoomOrPeerId = roomId,
                isDirect = false,
                content = text,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                messageType = MessageType.TEXT,
                senderIp = if (localIp.value.isNotBlank() && localIp.value != "127.0.0.1") localIp.value else com.example.network.NetworkUtils.getLocalIpAddress(getApplication())
            )

            // Save to DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderColor = msg.senderColor,
                    targetRoomOrPeerId = msg.targetRoomOrPeerId,
                    isDirect = false,
                    content = msg.content,
                    timestamp = msg.timestamp,
                    isMine = true,
                    messageType = msg.messageType.name,
                    imageBase64 = null,
                    fileId = null,
                    fileName = null,
                    fileSize = 0L,
                    mimeType = null,
                    senderIp = msg.senderIp,
                    localFilePath = null,
                    isDownloaded = true
                )
            )

            // Send via engine broadcast
            engine.sendRoomMessage(roomId, msg)
        }
    }

    /**
     * Sends a local file or document to all members in the current room via P2P.
     */
    fun sendInRoomFile(uri: android.net.Uri, caption: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val staged = engine.fileTransferEngine.stageFileForSharing(uri) ?: return@launch
            val roomId = currentRoom.value
            val currentProfile = userProfile.value
            val reply = _replyingToMessage.value
            _replyingToMessage.value = null
            val isImage = staged.mimeType.startsWith("image/") || engine.fileTransferEngine.isImageFile(staged.fileName)
            val resolvedIp = if (localIp.value.isNotBlank() && localIp.value != "127.0.0.1") localIp.value else com.example.network.NetworkUtils.getLocalIpAddress(getApplication())

            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentProfile.id,
                senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                senderColor = currentProfile.avatarColor,
                targetRoomOrPeerId = roomId,
                isDirect = false,
                content = caption.ifBlank { staged.fileName },
                timestamp = System.currentTimeMillis(),
                isMine = true,
                messageType = if (isImage) MessageType.IMAGE else MessageType.FILE,
                fileId = staged.fileId,
                fileName = staged.fileName,
                fileSize = staged.fileSize,
                mimeType = staged.mimeType,
                senderIp = resolvedIp,
                localFilePath = staged.localFilePath,
                isDownloaded = true,
                replyToId = reply?.id,
                replyToSender = reply?.senderName,
                replyToText = reply?.content
            )

            // Save to DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderColor = msg.senderColor,
                    targetRoomOrPeerId = msg.targetRoomOrPeerId,
                    isDirect = false,
                    content = msg.content,
                    timestamp = msg.timestamp,
                    isMine = true,
                    messageType = msg.messageType.name,
                    imageBase64 = null,
                    fileId = msg.fileId,
                    fileName = msg.fileName,
                    fileSize = msg.fileSize,
                    mimeType = msg.mimeType,
                    senderIp = msg.senderIp,
                    localFilePath = msg.localFilePath,
                    isDownloaded = true,
                    replyToId = reply?.id,
                    replyToSender = reply?.senderName,
                    replyToText = reply?.content
                )
            )

            // Broadcast to room
            engine.sendRoomMessage(roomId, msg)
        }
    }

    /**
     * Sends a local file directly to a peer via P2P.
     */
    fun sendDirectFile(peer: Peer, uri: android.net.Uri, caption: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val staged = engine.fileTransferEngine.stageFileForSharing(uri) ?: return@launch
            val currentProfile = userProfile.value
            val reply = _replyingToMessage.value
            _replyingToMessage.value = null
            val isImage = staged.mimeType.startsWith("image/") || engine.fileTransferEngine.isImageFile(staged.fileName)
            val resolvedIp = if (localIp.value.isNotBlank() && localIp.value != "127.0.0.1") localIp.value else com.example.network.NetworkUtils.getLocalIpAddress(getApplication())

            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentProfile.id,
                senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                senderColor = currentProfile.avatarColor,
                targetRoomOrPeerId = peer.id,
                isDirect = true,
                content = caption.ifBlank { staged.fileName },
                timestamp = System.currentTimeMillis(),
                isMine = true,
                messageType = if (isImage) MessageType.IMAGE else MessageType.FILE,
                fileId = staged.fileId,
                fileName = staged.fileName,
                fileSize = staged.fileSize,
                mimeType = staged.mimeType,
                senderIp = resolvedIp,
                localFilePath = staged.localFilePath,
                isDownloaded = true,
                replyToId = reply?.id,
                replyToSender = reply?.senderName,
                replyToText = reply?.content
            )

            // Save to DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    senderColor = msg.senderColor,
                    targetRoomOrPeerId = msg.targetRoomOrPeerId,
                    isDirect = true,
                    content = msg.content,
                    timestamp = msg.timestamp,
                    isMine = true,
                    messageType = msg.messageType.name,
                    imageBase64 = null,
                    fileId = msg.fileId,
                    fileName = msg.fileName,
                    fileSize = msg.fileSize,
                    mimeType = msg.mimeType,
                    senderIp = msg.senderIp,
                    localFilePath = msg.localFilePath,
                    isDownloaded = true,
                    replyToId = reply?.id,
                    replyToSender = reply?.senderName,
                    replyToText = reply?.content
                )
            )

            // Send to peer
            engine.sendDirectMessage(peer, msg)
        }
    }

    /**
     * Sends a local file directly using current chat selection (direct peer or room fallback).
     */
    fun sendDirectFile(uri: android.net.Uri, caption: String = "") {
        onUserTyping(false)
        val peer = _currentChatPeer.value
        if (peer != null) {
            sendDirectFile(peer, uri, caption)
        } else {
            val target = _currentChatTarget.value
            if (target.startsWith("peer_")) {
                val peerId = target.removePrefix("peer_")
                val foundPeer = discoveredPeers.value.find { it.id == peerId }
                if (foundPeer != null) {
                    sendDirectFile(foundPeer, uri, caption)
                }
            } else {
                sendInRoomFile(uri, caption)
            }
        }
    }

    /**
     * Downloads a file from the sender peer over local Wi-Fi TCP.
     */
    fun downloadMessageFile(message: ChatMessage) {
        val fileId = message.fileId ?: return
        val senderIp = message.senderIp ?: return
        val fileName = message.fileName ?: "file_${System.currentTimeMillis()}"

        viewModelScope.launch(Dispatchers.IO) {
            val downloadedFile = engine.fileTransferEngine.downloadFileFromPeer(
                messageId = message.id,
                fileId = fileId,
                fileName = fileName,
                expectedSize = message.fileSize,
                senderIp = senderIp,
                mimeType = message.mimeType
            )

            if (downloadedFile != null && downloadedFile.exists()) {
                chatDao.updateMessageFileDownloaded(message.id, downloadedFile.absolutePath)
            }
        }
    }

    /**
     * Cancels an ongoing file download, preserving partial downloaded bytes on disk for resumption.
     */
    fun cancelDownloadFile(message: ChatMessage) {
        engine.fileTransferEngine.cancelDownload(message.id)
    }

    /**
     * Opens a local file or downloaded file using Android Intent chooser.
     */
    fun openMessageFile(message: ChatMessage) {
        val path = message.localFilePath ?: return
        engine.fileTransferEngine.openFile(path, message.mimeType)
    }

    // --- Voice Notes Recording & Playback Operations ---

    /**
     * Starts voice recording for short audio clip.
     */
    fun startVoiceRecording(): Boolean {
        return engine.voiceNoteRecorder.startRecording()
    }

    /**
     * Cancels the active voice recording and discards audio file.
     */
    fun cancelVoiceRecording() {
        engine.voiceNoteRecorder.cancelRecording()
    }

    // Voice Note Preview Draft (U-07)
    private val _voiceDraft = MutableStateFlow<com.example.audio.VoiceNoteRecordResult?>(null)
    val voiceDraft = _voiceDraft.asStateFlow()

    private val _isVoiceDraftPlaying = MutableStateFlow(false)
    val isVoiceDraftPlaying = _isVoiceDraftPlaying.asStateFlow()

    fun pauseAndReviewVoiceRecording() {
        onUserTyping(false)
        val result = engine.voiceNoteRecorder.stopRecording()
        _voiceDraft.value = result
    }

    fun toggleVoiceDraftPlayback() {
        val draft = _voiceDraft.value ?: return
        if (_isVoiceDraftPlaying.value) {
            engine.voiceNotePlayer.stop()
            _isVoiceDraftPlaying.value = false
        } else {
            engine.voiceNotePlayer.play("draft_preview", draft.file.absolutePath)
            _isVoiceDraftPlaying.value = true
        }
    }

    fun cancelVoiceDraft() {
        if (_isVoiceDraftPlaying.value) {
            engine.voiceNotePlayer.stop()
            _isVoiceDraftPlaying.value = false
        }
        _voiceDraft.value?.file?.delete()
        _voiceDraft.value = null
    }

    fun sendVoiceDraft(caption: String = "") {
        val draft = _voiceDraft.value ?: return
        if (_isVoiceDraftPlaying.value) {
            engine.voiceNotePlayer.stop()
            _isVoiceDraftPlaying.value = false
        }
        _voiceDraft.value = null
        sendRecordedVoiceResult(draft, caption)
    }

    /**
     * Stops active recording and sends the voice note directly to peer or room based on chat context.
     */
    fun stopAndSendVoiceNote(caption: String = "") {
        onUserTyping(false)
        val result = engine.voiceNoteRecorder.stopRecording() ?: return
        sendRecordedVoiceResult(result, caption)
    }

    fun stopAndSendInRoomVoiceNote(caption: String = "") {
        stopAndSendVoiceNote(caption)
    }

    /**
     * Internal helper to stage and send a completed VoiceNoteRecordResult with reply metadata.
     */
    private fun sendRecordedVoiceResult(result: com.example.audio.VoiceNoteRecordResult, caption: String = "") {
        val reply = _replyingToMessage.value
        _replyingToMessage.value = null
        val peer = _currentChatPeer.value
        if (peer != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val staged = engine.fileTransferEngine.stageExistingFile(result.file, "audio/m4a")
                val currentProfile = userProfile.value

                val msg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = currentProfile.id,
                    senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                    senderColor = currentProfile.avatarColor,
                    targetRoomOrPeerId = peer.id,
                    isDirect = true,
                    content = caption.ifBlank { "تسجيل صوتي (${result.durationSeconds} ثانية)" },
                    timestamp = System.currentTimeMillis(),
                    isMine = true,
                    messageType = MessageType.VOICE_NOTE,
                    fileId = staged.fileId,
                    fileName = staged.fileName,
                    fileSize = staged.fileSize,
                    mimeType = "audio/m4a",
                    durationSeconds = result.durationSeconds,
                    senderIp = localIp.value,
                    localFilePath = staged.localFilePath,
                    isDownloaded = true,
                    replyToId = reply?.id,
                    replyToSender = reply?.senderName,
                    replyToText = reply?.content
                )

                chatDao.insertMessage(
                    ChatMessageEntity(
                        id = msg.id,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        senderColor = msg.senderColor,
                        targetRoomOrPeerId = msg.targetRoomOrPeerId,
                        isDirect = true,
                        content = msg.content,
                        timestamp = msg.timestamp,
                        isMine = true,
                        messageType = msg.messageType.name,
                        imageBase64 = null,
                        fileId = msg.fileId,
                        fileName = msg.fileName,
                        fileSize = msg.fileSize,
                        mimeType = msg.mimeType,
                        durationSeconds = msg.durationSeconds,
                        senderIp = msg.senderIp,
                        localFilePath = msg.localFilePath,
                        isDownloaded = true,
                        replyToId = reply?.id,
                        replyToSender = reply?.senderName,
                        replyToText = reply?.content
                    )
                )

                engine.sendDirectMessage(peer, msg)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val roomId = _currentChatTarget.value.ifBlank { currentRoom.value }.ifBlank { "general" }
                val staged = engine.fileTransferEngine.stageExistingFile(result.file, "audio/m4a")
                val currentProfile = userProfile.value

                val msg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = currentProfile.id,
                    senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                    senderColor = currentProfile.avatarColor,
                    targetRoomOrPeerId = roomId,
                    isDirect = false,
                    content = caption.ifBlank { "تسجيل صوتي (${result.durationSeconds} ثانية)" },
                    timestamp = System.currentTimeMillis(),
                    isMine = true,
                    messageType = MessageType.VOICE_NOTE,
                    fileId = staged.fileId,
                    fileName = staged.fileName,
                    fileSize = staged.fileSize,
                    mimeType = "audio/m4a",
                    durationSeconds = result.durationSeconds,
                    senderIp = localIp.value,
                    localFilePath = staged.localFilePath,
                    isDownloaded = true,
                    replyToId = reply?.id,
                    replyToSender = reply?.senderName,
                    replyToText = reply?.content
                )

                chatDao.insertMessage(
                    ChatMessageEntity(
                        id = msg.id,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        senderColor = msg.senderColor,
                        targetRoomOrPeerId = msg.targetRoomOrPeerId,
                        isDirect = false,
                        content = msg.content,
                        timestamp = msg.timestamp,
                        isMine = true,
                        messageType = msg.messageType.name,
                        imageBase64 = null,
                        fileId = msg.fileId,
                        fileName = msg.fileName,
                        fileSize = msg.fileSize,
                        mimeType = msg.mimeType,
                        durationSeconds = msg.durationSeconds,
                        senderIp = msg.senderIp,
                        localFilePath = msg.localFilePath,
                        isDownloaded = true,
                        replyToId = reply?.id,
                        replyToSender = reply?.senderName,
                        replyToText = reply?.content
                    )
                )

                engine.sendRoomMessage(roomId, msg)
            }
        }
    }

    /**
     * Toggles playback for a voice note message.
     */
    fun toggleVoiceNotePlayback(message: ChatMessage) {
        val path = message.localFilePath
        if (path != null && File(path).exists()) {
            engine.voiceNotePlayer.togglePlayPause(message.id, path)
        } else if (message.fileId != null && message.senderIp != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val fileId = message.fileId
                val senderIp = message.senderIp
                val fileName = message.fileName ?: "voice_${message.id}.m4a"
                val downloadedFile = engine.fileTransferEngine.downloadFileFromPeer(
                    messageId = message.id,
                    fileId = fileId,
                    fileName = fileName,
                    expectedSize = message.fileSize,
                    senderIp = senderIp,
                    mimeType = message.mimeType
                )
                if (downloadedFile != null && downloadedFile.exists()) {
                    chatDao.updateMessageFileDownloaded(message.id, downloadedFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        engine.voiceNotePlayer.play(message.id, downloadedFile.absolutePath)
                    }
                }
            }
        }
    }

    /**
     * Seeks playback in currently playing voice note.
     */
    fun seekVoiceNote(progress: Float) {
        engine.voiceNotePlayer.seekTo(progress)
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = newContent.trim()
            if (trimmed.isNotBlank()) {
                chatDao.updateMessageContent(messageId, trimmed)
                val msg = chatDao.getMessageById(messageId)
                if (msg != null) {
                    engine.sendEditMessage(
                        messageId = msg.id,
                        targetRoomOrPeerId = msg.targetRoomOrPeerId,
                        isDirect = msg.isDirect,
                        newContent = trimmed
                    )
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = chatDao.getMessageById(messageId)
                if (entity != null && !entity.localFilePath.isNullOrBlank()) {
                    val file = File(entity.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to delete underlying file for message $messageId", e)
            }
            chatDao.deleteMessage(messageId)
        }
    }

    fun clearAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getApplication<android.app.Application>().cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clear app cache", e)
            }
        }
    }

    fun forwardMessage(message: ChatMessage, targetRoomOrPeerId: String, isDirect: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentProfile = userProfile.value

            var fFileId = message.fileId
            var fFileName = message.fileName
            var fFileSize = message.fileSize
            var fMimeType = message.mimeType
            var fLocalFilePath = message.localFilePath

            // Ensure locally downloaded/staged file is staged with a fresh ID in FileTransferEngine
            // so this device can properly serve download requests from the forwarded recipient
            if (!message.localFilePath.isNullOrBlank()) {
                val existingFile = File(message.localFilePath)
                if (existingFile.exists() && existingFile.length() > 0) {
                    val staged = engine.fileTransferEngine.stageExistingFile(existingFile, message.mimeType ?: "*/*")
                    fFileId = staged.fileId
                    fFileName = staged.fileName
                    fFileSize = staged.fileSize
                    fMimeType = staged.mimeType
                    fLocalFilePath = staged.localFilePath
                }
            }

            val newMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentProfile.id,
                senderName = currentProfile.displayName.ifBlank { currentProfile.username },
                senderColor = currentProfile.avatarColor,
                targetRoomOrPeerId = targetRoomOrPeerId,
                isDirect = isDirect,
                content = message.content,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                messageType = message.messageType,
                imageBase64 = message.imageBase64,
                fileId = fFileId,
                fileName = fFileName,
                fileSize = fFileSize,
                mimeType = fMimeType,
                durationSeconds = message.durationSeconds,
                senderIp = localIp.value,
                localFilePath = fLocalFilePath,
                isDownloaded = true
            )

            // Save to DB
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = newMsg.id,
                    senderId = newMsg.senderId,
                    senderName = newMsg.senderName,
                    senderColor = newMsg.senderColor,
                    targetRoomOrPeerId = newMsg.targetRoomOrPeerId,
                    isDirect = isDirect,
                    content = newMsg.content,
                    timestamp = newMsg.timestamp,
                    isMine = true,
                    messageType = newMsg.messageType.name,
                    imageBase64 = newMsg.imageBase64,
                    fileId = newMsg.fileId,
                    fileName = newMsg.fileName,
                    fileSize = newMsg.fileSize,
                    mimeType = newMsg.mimeType,
                    durationSeconds = newMsg.durationSeconds,
                    senderIp = newMsg.senderIp,
                    localFilePath = newMsg.localFilePath,
                    isDownloaded = true
                )
            )

            // Broadcast / Send
            if (isDirect) {
                val peer = discoveredPeers.value.find { it.id == targetRoomOrPeerId }
                if (peer != null) {
                    engine.sendDirectMessage(peer, newMsg)
                }
            } else {
                engine.sendRoomMessage(targetRoomOrPeerId, newMsg)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.voiceNoteRecorder.cancelRecording()
        engine.voiceNotePlayer.stop()
        engine.stop()
    }
}
