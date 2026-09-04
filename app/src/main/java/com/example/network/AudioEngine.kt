package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioEngine"
        // High-definition wideband audio (16kHz PCM 16-bit Mono)
        const val SAMPLE_RATE = 16000
        const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SIZE = 640 // 20ms at 16kHz 16-bit mono = 320 samples = 640 bytes

        // Packet header: senderId (32) + sessionKey (8) + flags (1) + reserved (7).
        // The sessionKey must equal the receiver's activeAuthorizedSessionId or the
        // packet is silently discarded (zero pre-accept audio leakage).
        const val HEADER_SENDER_ID_SIZE = 32
        const val HEADER_SESSION_SIZE = 8
        const val HEADER_FLAGS_OFFSET = HEADER_SENDER_ID_SIZE + HEADER_SESSION_SIZE
        const val HEADER_SIZE = 48
        const val FLAG_ENCRYPTED: Byte = 0x01
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null

    // RULE 1: audio playback is gated on an explicitly authorized call/room session.
    // Packets whose session header does not match are silently discarded.
    private val activeAuthorizedSessionId = AtomicReference<String?>(null)

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel = _micLevel.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _isOpenMic = MutableStateFlow(false)
    val isOpenMic = _isOpenMic.asStateFlow()

    private val _isPushToTalkActive = MutableStateFlow(false)
    val isPushToTalkActive = _isPushToTalkActive.asStateFlow()

    // Targets to stream audio to (Peer ID -> Pair(IP, Port))
    private val activeTargetAddresses = ConcurrentHashMap<String, Pair<InetAddress, Int>>()

    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost: $focusChange")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
            }
        }
    }

    // Mixer queues per sender (senderId -> Queue of PCM frames)
    private val incomingAudioQueues = ConcurrentHashMap<String, ConcurrentLinkedQueue<ByteArray>>()
    private val lastHeardSenderMap = ConcurrentHashMap<String, Long>()

    init {
        try {
            sendSocket = DatagramSocket()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create audio send socket", e)
        }
    }

    private val _callVolume = MutableStateFlow(1.0f)
    val callVolume = _callVolume.asStateFlow()

    fun setCallVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _callVolume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        setSpeakerOn(newState)
    }

    fun setSpeakerOn(speakerOn: Boolean) {
        _isSpeakerOn.value = speakerOn
        applyCommunicationDeviceRouting()
    }

    private fun requestAudioFocus(am: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                am.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request audio focus", e)
        }
    }

    private fun abandonAudioFocus(am: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon audio focus", e)
        }
    }

    /**
     * Modern audio routing: on Android 12+ the deprecated isSpeakerphoneOn flag is
     * replaced by setCommunicationDevice() to switch between earpiece, speaker,
     * bluetooth sco headset, or wired headset.
     */
    private fun applyCommunicationDeviceRouting() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val speakerOn = _isSpeakerOn.value
                val available = audioManager.availableCommunicationDevices
                val targetDevice = if (speakerOn) {
                    available.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                } else {
                    // Priority when speaker is OFF:
                    // 1. Bluetooth SCO / BLE headset / Hearing aid
                    // 2. Wired headset / headphones
                    // 3. Built-in earpiece
                    val btDevice = available.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_HEARING_AID
                    }
                    val wiredDevice = available.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    }
                    btDevice ?: wiredDevice ?: available.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                }
                try {
                    audioManager.clearCommunicationDevice()
                } catch (_: Exception) {
                }
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                }
            } else {
                @Suppress("DEPRECATION")
                val speakerOn = _isSpeakerOn.value
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = speakerOn
                if (!speakerOn) {
                    try {
                        @Suppress("DEPRECATION")
                        audioManager.startBluetoothSco()
                        @Suppress("DEPRECATION")
                        audioManager.isBluetoothScoOn = true
                    } catch (_: Exception) {}
                } else {
                    try {
                        @Suppress("DEPRECATION")
                        audioManager.stopBluetoothSco()
                        @Suppress("DEPRECATION")
                        audioManager.isBluetoothScoOn = false
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio routing", e)
        }
    }

    fun toggleOpenMic() {
        val newState = !_isOpenMic.value
        _isOpenMic.value = newState
    }

    fun setOpenMic(open: Boolean) {
        _isOpenMic.value = open
    }

    fun setPushToTalk(active: Boolean) {
        _isPushToTalkActive.value = active
    }

    fun updateTarget(id: String, address: InetAddress, port: Int = NetworkUtils.AUDIO_PORT) {
        activeTargetAddresses[id] = Pair(address, port)
    }

    fun removeTarget(id: String) {
        activeTargetAddresses.remove(id)
    }

    fun clearTargets() {
        activeTargetAddresses.clear()
    }

    /**
     * Authorizes playback of packets belonging to [sessionId] (call id / room voice key).
     * Any audio packet carrying a different session header is silently discarded.
     */
    fun authorizeSession(sessionId: String) {
        activeAuthorizedSessionId.set(sessionId.take(HEADER_SESSION_SIZE))
        startAudioPlayback()
    }

    /** Revokes the authorized session — stops playback and drops incoming audio packets. */
    fun revokeSession() {
        activeAuthorizedSessionId.set(null)
        stopAudioPlayback()
    }

    fun getAuthorizedSession(): String? = activeAuthorizedSessionId.get()

    private fun configureAudioMode(active: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (active) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                requestAudioFocus(audioManager)
                applyCommunicationDeviceRouting()
            } else {
                abandonAudioFocus(audioManager)
                audioManager.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        audioManager.clearCommunicationDevice()
                    } catch (_: Exception) {
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    try {
                        @Suppress("DEPRECATION")
                        audioManager.stopBluetoothSco()
                        @Suppress("DEPRECATION")
                        audioManager.isBluetoothScoOn = false
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure audio manager mode", e)
        }
    }

    /**
     * Software Audio Mixer for multi-speaker group calls.
     * Takes 16-bit Mono Little-Endian PCM frames from simultaneous speakers,
     * computes signed 16-bit integer sums with clipping/clamping protection [-32768, 32767].
     */
    private fun mixPcmFrames(frames: List<ByteArray>): ByteArray {
        if (frames.isEmpty()) return ByteArray(FRAME_SIZE)
        if (frames.size == 1) return frames[0]
        val sampleCount = FRAME_SIZE / 2 // 320 samples
        val out = ByteArray(FRAME_SIZE)
        for (i in 0 until sampleCount) {
            var sum = 0
            val byteOffset = i * 2
            for (frame in frames) {
                if (byteOffset + 1 < frame.size) {
                    val sample = (frame[byteOffset].toInt() and 0xFF) or (frame[byteOffset + 1].toInt() shl 8)
                    sum += sample.toShort().toInt()
                }
            }
            val clamped = sum.coerceIn(-32768, 32767)
            out[byteOffset] = (clamped and 0xFF).toByte()
            out[byteOffset + 1] = ((clamped shr 8) and 0xFF).toByte()
        }
        return out
    }

    /**
     * Start playing received audio from UDP socket.
     * RULE 1: the loop strictly discards any packet whose session header does not
     * match activeAuthorizedSessionId — no audio can play before a call is accepted
     * or a room voice session is explicitly joined.
     */
    fun startAudioPlayback() {
        if (playbackJob?.isActive == true) return

        configureAudioMode(true)

        playbackJob = scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_OUT,
                    AUDIO_FORMAT
                )
                val bufferSize = (minBufferSize * 4).coerceAtLeast(FRAME_SIZE * 8)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_OUT)
                    .build()

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(_callVolume.value)
                audioTrack?.play()

                receiveSocket?.close()
                receiveSocket = DatagramSocket(NetworkUtils.AUDIO_PORT).apply {
                    reuseAddress = true
                    receiveBufferSize = 256 * 1024
                }

                // Mixer playback loop: pulls 1 frame from each active speaker queue, mixes, and writes to AudioTrack
                val playbackLoopJob = launch {
                    val framesToMix = mutableListOf<ByteArray>()
                    while (isActive) {
                        framesToMix.clear()
                        val now = System.currentTimeMillis()
                        val iterator = incomingAudioQueues.entries.iterator()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            val q = entry.value
                            val frame = q.poll()
                            if (frame != null) {
                                framesToMix.add(frame)
                                lastHeardSenderMap[entry.key] = now
                            } else {
                                val lastHeard = lastHeardSenderMap[entry.key] ?: 0L
                                if (now - lastHeard > 5000L) {
                                    iterator.remove()
                                    lastHeardSenderMap.remove(entry.key)
                                }
                            }
                        }

                        if (framesToMix.isEmpty()) {
                            delay(15)
                            continue
                        }

                        val mixed = mixPcmFrames(framesToMix)
                        audioTrack?.write(mixed, 0, mixed.size)
                    }
                }

                val packetBuffer = ByteArray(HEADER_SIZE + FRAME_SIZE + 512)
                val datagramPacket = DatagramPacket(packetBuffer, packetBuffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(datagramPacket)
                        val length = datagramPacket.length
                        if (length <= HEADER_SIZE) continue

                        // RULE 1 gate: silent discard for unauthorized sessions
                        val authorizedSession = activeAuthorizedSessionId.get() ?: continue
                        val packetSession = String(
                            packetBuffer,
                            HEADER_SENDER_ID_SIZE,
                            HEADER_SESSION_SIZE,
                            Charsets.UTF_8
                        ).trimEnd { it == '\u0000' }
                        if (packetSession != authorizedSession) continue

                        val senderId = String(
                            packetBuffer,
                            0,
                            HEADER_SENDER_ID_SIZE,
                            Charsets.UTF_8
                        ).trimEnd { it == '\u0000' }

                        val flags = packetBuffer[HEADER_FLAGS_OFFSET]
                        val encrypted = (flags.toInt() and FLAG_ENCRYPTED.toInt()) != 0

                        val pcm: ByteArray? = if (encrypted) {
                            LocalCryptoEngine.decrypt(
                                packetBuffer.copyOfRange(HEADER_SIZE, length)
                            )
                        } else {
                            // Legacy/foreign plaintext packets are rejected too:
                            // every authorized packet on this network is encrypted.
                            null
                        }

                        if (pcm != null && pcm.isNotEmpty()) {
                            val queue = incomingAudioQueues.getOrPut(senderId) {
                                ConcurrentLinkedQueue()
                            }
                            // Bounded jitter queue: drop oldest if buffer exceeds 4 frames (80ms)
                            while (queue.size >= 4) {
                                queue.poll()
                            }
                            queue.offer(pcm)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
                playbackLoopJob.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback loop error", e)
            } finally {
                stopPlaybackInternal()
            }
        }
    }

    fun startPlayback() {
        startAudioPlayback()
    }

    /**
     * Start capturing local microphone and transmitting encrypted frames to targets.
     * @param sessionId 8-char session key stamped into every packet header; receivers
     *        only play frames whose key matches their authorized session.
     */
    @SuppressLint("MissingPermission")
    fun startAudioRecording(myId: String, currentRoom: String, sessionId: String = currentRoom.take(HEADER_SESSION_SIZE)) {
        if (recordingJob?.isActive == true) return

        configureAudioMode(true)

        recordingJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    AUDIO_FORMAT
                )
                val bufferSize = (minBufferSize * 4).coerceAtLeast(FRAME_SIZE * 8)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    AUDIO_FORMAT,
                    bufferSize
                )

                val audioSessionId = audioRecord?.audioSessionId ?: 0
                if (audioSessionId != 0) {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    }
                    if (AutomaticGainControl.isAvailable()) {
                        gainControl = AutomaticGainControl.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    }
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    audioRecord?.startRecording()
                    _isRecording.value = true
                } else {
                    Log.e(TAG, "RECORD_AUDIO permission not granted, cannot start recording")
                    return@launch
                }

                val pcmBuffer = ByteArray(FRAME_SIZE)
                val frameBuffer = ByteArray(HEADER_SIZE + FRAME_SIZE + LocalCryptoEngine.NONCE_SIZE + 16)

                // Header info
                val idBytes = myId.toByteArray(Charsets.UTF_8).copyOf(HEADER_SENDER_ID_SIZE)
                val sessionBytes = sessionId.take(HEADER_SESSION_SIZE)
                    .toByteArray(Charsets.UTF_8)
                    .copyOf(HEADER_SESSION_SIZE)
                System.arraycopy(idBytes, 0, frameBuffer, 0, HEADER_SENDER_ID_SIZE)
                System.arraycopy(sessionBytes, 0, frameBuffer, HEADER_SENDER_ID_SIZE, HEADER_SESSION_SIZE)
                frameBuffer[HEADER_FLAGS_OFFSET] = FLAG_ENCRYPTED

                while (isActive) {
                    val bytesRead = audioRecord?.read(pcmBuffer, 0, FRAME_SIZE) ?: -1
                    if (bytesRead > 0) {
                        if (!_isMuted.value && (_isOpenMic.value || _isPushToTalkActive.value || _isRecording.value)) {
                            // Calculate audio amplitude for UI level
                            var sum = 0L
                            for (i in 0 until bytesRead step 2) {
                                val sample = (pcmBuffer[i].toInt() and 0xFF) or (pcmBuffer[i + 1].toInt() shl 8)
                                sum += abs(sample.toShort().toLong())
                            }
                            val avg = sum / (bytesRead / 2)
                            val normalized = (avg / 32767f).coerceIn(0f, 1f)
                            _micLevel.value = normalized

                            // RULE 5: AES-256-GCM encryption of the raw PCM frame
                            val cipherPayload = LocalCryptoEngine.encrypt(pcmBuffer.copyOf(bytesRead))
                            if (HEADER_SIZE + cipherPayload.size <= frameBuffer.size) {
                                System.arraycopy(cipherPayload, 0, frameBuffer, HEADER_SIZE, cipherPayload.size)

                                for ((_, target) in activeTargetAddresses) {
                                    try {
                                        val packet = DatagramPacket(
                                            frameBuffer,
                                            HEADER_SIZE + cipherPayload.size,
                                            target.first,
                                            target.second
                                        )
                                        sendSocket?.send(packet)
                                    } catch (e: Exception) {
                                        // Ignore individual dropped packets
                                    }
                                }
                            }
                        } else {
                            _micLevel.value = 0f
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio record error", e)
            } finally {
                stopRecordingInternal()
            }
        }
    }

    fun startRecording(myId: String, currentRoom: String, sessionId: String = currentRoom.take(HEADER_SESSION_SIZE)) {
        startAudioRecording(myId, currentRoom, sessionId)
    }

    fun stopAudioRecording() {
        recordingJob?.cancel()
        recordingJob = null
        stopRecordingInternal()
    }

    private fun stopRecordingInternal() {
        _isRecording.value = false
        _micLevel.value = 0f
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        echoCanceler?.release()
        echoCanceler = null
        noiseSuppressor?.release()
        noiseSuppressor = null
        gainControl?.release()
        gainControl = null

        // If playback is not active either, restore NORMAL mode
        if (playbackJob == null || playbackJob?.isActive != true) {
            configureAudioMode(false)
        }
    }

    fun stopAudioPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        stopPlaybackInternal()
    }

    private fun stopPlaybackInternal() {
        incomingAudioQueues.clear()
        lastHeardSenderMap.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
        try {
            receiveSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        receiveSocket = null

        // If recording is not active either, restore NORMAL mode
        if (!_isRecording.value) {
            configureAudioMode(false)
        }
    }

    fun stopAllAudio() {
        revokeSession()
        stopAudioRecording()
        stopAudioPlayback()
        configureAudioMode(false)
    }

    fun release() {
        stopAllAudio()
        sendSocket?.close()
        sendSocket = null
    }
}
