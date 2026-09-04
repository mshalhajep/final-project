package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VoiceNotePlayer(private val context: Context) {

    companion object {
        private const val TAG = "VoiceNotePlayer"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume if needed
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId = _currentlyPlayingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    /**
     * Applies the current playback speed (1.0x / 1.5x / 2.0x) — supported natively
     * by MediaPlayer on Android 6+ through PlaybackParams.
     */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 3.0f)
        try {
            mediaPlayer?.let { player ->
                player.playbackParams = player.playbackParams.setSpeed(_playbackSpeed.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set playback speed", e)
        }
    }

    /** Cycles 1.0x -> 1.5x -> 2.0x -> 1.0x for the voice note speed pill. */
    fun cyclePlaybackSpeed() {
        setPlaybackSpeed(
            when (_playbackSpeed.value) {
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
        )
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                am.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request audio focus in VoiceNotePlayer", e)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon audio focus in VoiceNotePlayer", e)
        }
    }

    fun togglePlayPause(messageId: String, filePath: String) {
        if (_currentlyPlayingId.value == messageId && _isPlaying.value) {
            pause()
        } else if (_currentlyPlayingId.value == messageId && mediaPlayer != null) {
            resume()
        } else {
            play(messageId, filePath)
        }
    }

    fun play(messageId: String, filePath: String) {
        stop()

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Audio file does not exist: $filePath")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
            }

            mediaPlayer = player
            _currentlyPlayingId.value = messageId
            _durationMs.value = player.duration.coerceAtLeast(1000)
            _playbackProgress.value = 0f
            _currentPositionMs.value = 0

            player.setOnCompletionListener {
                abandonAudioFocus()
                _isPlaying.value = false
                _playbackProgress.value = 0f
                _currentPositionMs.value = 0
                _currentlyPlayingId.value = null
                stopProgressPolling()
                try {
                    it.release()
                } catch (_: Exception) {}
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }
            }

            player.setOnErrorListener { p, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                abandonAudioFocus()
                try { p.release() } catch (_: Exception) {}
                stop()
                true
            }

            requestAudioFocus()
            player.start()
            _isPlaying.value = true
            // Re-apply the chosen speed to freshly created players
            if (_playbackSpeed.value != 1.0f) {
                setPlaybackSpeed(_playbackSpeed.value)
            }
            startProgressPolling()
        } catch (e: Exception) {
            abandonAudioFocus()
            Log.e(TAG, "Failed to start audio playback for $filePath", e)
            stop()
        }
    }

    fun pause() {
        try {
            abandonAudioFocus()
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause audio", e)
        }
    }

    fun resume() {
        try {
            requestAudioFocus()
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume audio", e)
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        try {
            val targetMs = (progress * _durationMs.value).toInt().coerceIn(0, _durationMs.value)
            player.seekTo(targetMs)
            _currentPositionMs.value = targetMs
            _playbackProgress.value = progress.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek audio", e)
        }
    }

    fun stop() {
        val p = mediaPlayer
        mediaPlayer = null
        abandonAudioFocus()
        stopProgressPolling()
        try {
            if (p?.isPlaying == true) {
                p.stop()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            try {
                p?.release()
            } catch (_: Exception) {}
            _isPlaying.value = false
            _currentlyPlayingId.value = null
            _playbackProgress.value = 0f
            _currentPositionMs.value = 0
            _durationMs.value = 0
        }
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        val current = player.currentPosition
                        val total = player.duration.coerceAtLeast(1)
                        _currentPositionMs.value = current
                        _playbackProgress.value = (current.toFloat() / total).coerceIn(0f, 1f)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(50)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }
}
