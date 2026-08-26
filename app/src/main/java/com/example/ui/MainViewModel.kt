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
import com.example.model.ActiveCall
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.model.Peer
import com.example.model.RoomInfo
import com.example.model.RoomInvitation
import com.example.model.UserProfile
import com.example.model.UserPresenceStatus
import com.example.network.LocalP2PEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val engine = LocalP2PEngine(application.applicationContext)

    private val db = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "offline_p2p_chat.db"
    ).fallbackToDestructiveMigration().build()

    private val chatDao = db.chatDao()

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
    val playingVoiceMessageId = engine.voiceNotePlayer.currentlyPlayingId
    val isPlayingVoiceNote = engine.voiceNotePlayer.isPlaying
    val voiceNoteProgress = engine.voiceNotePlayer.playbackProgress
    val voiceNoteCurrentMs = engine.voiceNotePlayer.currentPositionMs
    val voiceNoteDurationMs = engine.voiceNotePlayer.durationMs

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
                        isDownloaded = msg.isDownloaded
                    )
                )

                // Auto-download small voice notes from peer for instant listening
                if (msg.messageType == MessageType.VOICE_NOTE && msg.fileId != null && msg.senderIp != null) {
                    downloadMessageFile(msg)
                }
            }
        }

        // Listen for incoming room invitations
        viewModelScope.launch(Dispatchers.IO) {
            engine.incomingRoomInvites.collectLatest { invite ->
                _activeRoomInvitation.value = invite
            }
        }

        // Observe messages for active room and current chat target
        observeCurrentChatMessages()
        observeActiveRoomMessages()
    }

    private fun observeCurrentChatMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentChatTarget.collectLatest { targetId ->
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
                            isDownloaded = it.isDownloaded || it.isMine
                        )
                    }
                }
            }
        }
    }

    private fun observeActiveRoomMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            engine.currentRoom.collectLatest { roomId ->
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
                            isDownloaded = it.isDownloaded || it.isMine
                        )
                    }
                }
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
                            isPrivate = it.isPrivate
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
            val profile = UserProfile(
                id = user.userId,
                username = user.username,
                displayName = user.displayName,
                avatarColor = user.avatarColor,
                userStatus = userStatus,
                statusMessage = user.statusMessage,
                bio = user.bio,
                isLoggedIn = true
            )
            engine.setUserProfile(profile)
            _authState.value = AuthState.LoggedIn(profile)
            engine.start()
        }
    }

    fun register(usernameInput: String, passwordInput: String, displayNameInput: String, avatarColor: Long) {
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
            val newUser = UserAccountEntity(
                userId = userId,
                username = username,
                displayName = displayName,
                passwordHash = passwordHash,
                avatarColor = avatarColor,
                userStatus = UserPresenceStatus.ONLINE.name,
                statusMessage = "متصل محلياً ومستعد للحديث",
                bio = "مستخدم في تطبيق LocalConnect",
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

    fun joinRoom(roomId: String) {
        val targetRoom = _rooms.value.find { it.id == roomId }
        if (targetRoom != null) {
            val currentOccupancy = discoveredPeers.value.count { it.currentRoom == roomId } + 1
            if (currentOccupancy > targetRoom.maxCapacity) {
                _roomJoinError.value = "الغرفة ممتلئة (${targetRoom.maxCapacity}/${targetRoom.maxCapacity} عضو)"
                return
            }
        }
        _roomJoinError.value = null
        engine.setRoom(roomId)
        _currentChatTarget.value = roomId
        _currentChatIsDirect.value = false
        _currentChatPeer.value = null
    }

    fun clearRoomJoinError() {
        _roomJoinError.value = null
    }

    fun createRoom(name: String, description: String, capacity: Int = 10, isPrivate: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "room_" + UUID.randomUUID().toString().substring(0, 6)
            val currentProfile = userProfile.value
            val newRoomEntity = RoomEntity(
                id = id,
                name = name,
                description = description,
                iconName = "meeting_room",
                isDefault = false,
                creatorId = currentProfile.id,
                creatorName = currentProfile.displayName.ifBlank { currentProfile.username },
                maxCapacity = capacity,
                isPrivate = isPrivate
            )
            chatDao.insertRoom(newRoomEntity)
            joinRoom(id)
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
            engine.audioEngine.startRecording(userProfile.value.id, currentRoom.value)
            engine.audioEngine.startPlayback()
            engine.audioEngine.setOpenMic(true)
        } else {
            engine.audioEngine.setOpenMic(false)
            engine.audioEngine.setPushToTalk(false)
            engine.audioEngine.stopAllAudio()
        }
    }

    fun openDirectChat(peer: Peer) {
        _currentChatTarget.value = peer.id
        _currentChatIsDirect.value = true
        _currentChatPeer.value = peer
        _selectedTab.value = AppTab.CHAT
    }

    fun openRoomChat(roomId: String) {
        _currentChatTarget.value = roomId
        _currentChatIsDirect.value = false
        _currentChatPeer.value = null
        _selectedTab.value = AppTab.CHAT
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

    fun updateProfile(
        name: String,
        color: Long,
        status: String = "",
        bio: String = "",
        userStatus: UserPresenceStatus = userProfile.value.userStatus
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value
            val updated = current.copy(
                displayName = name,
                avatarColor = color,
                userStatus = userStatus,
                statusMessage = status.ifBlank { current.statusMessage },
                bio = bio.ifBlank { current.bio }
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
                    bio = bio.ifBlank { activeUser.bio }
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

    // --- Calling Actions ---

    fun startCall(peer: Peer, isVideo: Boolean) {
        engine.startCall(peer, isVideo)
    }

    fun acceptCall() {
        engine.acceptCall()
    }

    fun endCall() {
        engine.endCall()
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
        viewModelScope.launch(Dispatchers.IO) {
            val targetId = _currentChatTarget.value
            val isDirect = _currentChatIsDirect.value
            val currentProfile = userProfile.value

            val imageBase64 = bitmap?.let { bmp ->
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }

            val messageType = if (bitmap != null) MessageType.IMAGE else MessageType.TEXT

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
                messageType = messageType,
                imageBase64 = imageBase64
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
                    imageBase64 = msg.imageBase64
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
        viewModelScope.launch(Dispatchers.IO) {
            val roomId = currentRoom.value
            val currentProfile = userProfile.value

            val imageBase64 = bitmap?.let { bmp ->
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }

            val messageType = if (bitmap != null) MessageType.IMAGE else MessageType.TEXT

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
                messageType = messageType,
                imageBase64 = imageBase64
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
                    imageBase64 = msg.imageBase64
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
            val isImage = staged.mimeType.startsWith("image/")

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
                senderIp = localIp.value,
                localFilePath = staged.localFilePath,
                isDownloaded = true
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
                    isDownloaded = true
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
            val isImage = staged.mimeType.startsWith("image/")

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
                senderIp = localIp.value,
                localFilePath = staged.localFilePath,
                isDownloaded = true
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
                    isDownloaded = true
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
                senderIp = senderIp
            )

            if (downloadedFile != null && downloadedFile.exists()) {
                chatDao.updateMessageFileDownloaded(message.id, downloadedFile.absolutePath)
            }
        }
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

    /**
     * Stops active recording and sends the voice note into the current active room.
     */
    fun stopAndSendInRoomVoiceNote(caption: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val result = engine.voiceNoteRecorder.stopRecording() ?: return@launch
            val roomId = currentRoom.value
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
                isDownloaded = true
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
                    isDownloaded = true
                )
            )

            engine.sendRoomMessage(roomId, msg)
        }
    }

    /**
     * Stops active recording and sends the voice note directly to peer or room based on chat context.
     */
    fun stopAndSendVoiceNote(caption: String = "") {
        onUserTyping(false)
        val peer = _currentChatPeer.value
        if (peer != null) {
            stopAndSendDirectVoiceNote(peer, caption)
        } else {
            val target = _currentChatTarget.value
            if (target.startsWith("peer_")) {
                val peerId = target.removePrefix("peer_")
                val foundPeer = discoveredPeers.value.find { it.id == peerId }
                if (foundPeer != null) {
                    stopAndSendDirectVoiceNote(foundPeer, caption)
                } else {
                    stopAndSendInRoomVoiceNote(caption)
                }
            } else {
                stopAndSendInRoomVoiceNote(caption)
            }
        }
    }

    /**
     * Stops active recording and sends the voice note directly to a specific peer.
     */
    fun stopAndSendDirectVoiceNote(peer: Peer, caption: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val result = engine.voiceNoteRecorder.stopRecording() ?: return@launch
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
                isDownloaded = true
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
                    isDownloaded = true
                )
            )

            engine.sendDirectMessage(peer, msg)
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
                    senderIp = senderIp
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

    override fun onCleared() {
        super.onCleared()
        engine.voiceNoteRecorder.cancelRecording()
        engine.voiceNotePlayer.stop()
        engine.stop()
    }
}
