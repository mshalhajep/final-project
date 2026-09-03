package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioAttributes
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * Modern audio routing: on Android 12+ the deprecated isSpeakerphoneOn flag is
     * replaced by setCommunicationDevice() to switch between earpiece and speaker.
     */
    private fun applyCommunicationDeviceRouting() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speakerOn = _isSpeakerOn.value
                    val targetType = if (speakerOn) {
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    } else {
                        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    val device = am.availableCommunicationDevices.firstOrNull { it.type == targetType }
                    // Some chipsets refuse setCommunicationDevice while a previous
                    // device is still claimed — clear first, then set.
                    try {
                        am.clearCommunicationDevice()
                    } catch (_: Exception) {
                    }
                    if (device != null) {
                        am.setCommunicationDevice(device)
                    } else if (!speakerOn) {
                        am.clearCommunicationDevice()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = _isSpeakerOn.value
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting audio routing", e)
            }
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

    /** Revokes the authorized session — every incoming audio packet is discarded again. */
    fun revokeSession() {
        activeAuthorizedSessionId.set(null)
    }

    fun getAuthorizedSession(): String? = activeAuthorizedSessionId.get()

    private fun configureAudioMode(active: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (active) {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                applyCommunicationDeviceRouting()
            } else {
                audioManager?.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        audioManager?.clearCommunicationDevice()
                    } catch (_: Exception) {
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.isSpeakerphoneOn = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure audio manager mode", e)
        }
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

                        val flags = packetBuffer[HEADER_FLAGS_OFFSET]
                        val encrypted = (flags.toInt() and FLAG_ENCRYPTED.toInt()) != 0
                        val payloadLength = length - HEADER_SIZE

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
                            audioTrack?.write(pcm, 0, pcm.size)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
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
    }

    fun stopAudioPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        stopPlaybackInternal()
    }

    private fun stopPlaybackInternal() {
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
