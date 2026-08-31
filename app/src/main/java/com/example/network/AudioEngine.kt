package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
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
        const val HEADER_SIZE = 40 // senderId (32 bytes max) + roomId (8 bytes max)
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
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = speakerOn
            } catch (e: Exception) {
                Log.e(TAG, "Error setting speakerphone state", e)
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

    private fun configureAudioMode(active: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (active) {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.isSpeakerphoneOn = _isSpeakerOn.value
            } else {
                audioManager?.mode = AudioManager.MODE_NORMAL
                audioManager?.isSpeakerphoneOn = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure audio manager mode", e)
        }
    }

    /**
     * Start playing received audio from UDP socket.
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

                val packetBuffer = ByteArray(FRAME_SIZE + HEADER_SIZE + 128)
                val datagramPacket = DatagramPacket(packetBuffer, packetBuffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(datagramPacket)
                        val length = datagramPacket.length
                        if (length > HEADER_SIZE) {
                            val pcmLength = length - HEADER_SIZE
                            audioTrack?.write(packetBuffer, HEADER_SIZE, pcmLength)
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
     * Start capturing local microphone and transmitting to targets.
     */
    @SuppressLint("MissingPermission")
    fun startAudioRecording(myId: String, currentRoom: String) {
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

                audioRecord?.startRecording()
                _isRecording.value = true

                val pcmBuffer = ByteArray(FRAME_SIZE)
                val sendBuffer = ByteArray(FRAME_SIZE + HEADER_SIZE)

                // Header info
                val idBytes = myId.toByteArray(Charsets.UTF_8).copyOf(32)
                val roomBytes = currentRoom.toByteArray(Charsets.UTF_8).copyOf(8)
                System.arraycopy(idBytes, 0, sendBuffer, 0, 32)
                System.arraycopy(roomBytes, 0, sendBuffer, 32, 8)

                while (isActive) {
                    val bytesRead = audioRecord?.read(pcmBuffer, 0, FRAME_SIZE) ?: -1
                    if (bytesRead > 0) {
                        // Calculate audio amplitude for UI level
                        if (!_isMuted.value && (_isOpenMic.value || _isPushToTalkActive.value || _isRecording.value)) {
                            var sum = 0L
                            for (i in 0 until bytesRead step 2) {
                                val sample = (pcmBuffer[i].toInt() and 0xFF) or (pcmBuffer[i + 1].toInt() shl 8)
                                sum += abs(sample.toShort().toLong())
                            }
                            val avg = sum / (bytesRead / 2)
                            val normalized = (avg / 32767f).coerceIn(0f, 1f)
                            _micLevel.value = normalized

                            // Copy PCM into send buffer
                            System.arraycopy(pcmBuffer, 0, sendBuffer, HEADER_SIZE, bytesRead)

                            // Send to active targets
                            for ((_, target) in activeTargetAddresses) {
                                try {
                                    val packet = DatagramPacket(
                                        sendBuffer,
                                        HEADER_SIZE + bytesRead,
                                        target.first,
                                        target.second
                                    )
                                    sendSocket?.send(packet)
                                } catch (e: Exception) {
                                    // Ignore individual dropped packets
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

    fun startRecording(myId: String, currentRoom: String) {
        startAudioRecording(myId, currentRoom)
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
