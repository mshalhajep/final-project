package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
                _isPlaying.value = false
                _playbackProgress.value = 0f
                _currentPositionMs.value = 0
                _currentlyPlayingId.value = null
                stopProgressPolling()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stop()
                true
            }

            player.start()
            _isPlaying.value = true
            startProgressPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback for $filePath", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause audio", e)
        }
    }

    fun resume() {
        try {
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
        stopProgressPolling()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
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
