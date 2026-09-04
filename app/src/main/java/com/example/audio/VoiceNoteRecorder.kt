package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
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
import java.util.UUID

data class VoiceNoteRecordResult(
    val file: File,
    val durationSeconds: Int,
    val amplitudes: List<Float>
)

class VoiceNoteRecorder(private val context: Context) {

    companion object {
        private const val TAG = "VoiceNoteRecorder"
        const val MAX_RECORDING_SECONDS = 120 // 2 minutes max
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var amplitudeJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds = _durationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude = _currentAmplitude.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes = _amplitudes.asStateFlow()

    private val recordedAmplitudesList = mutableListOf<Float>()

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        try {
            val freeSpace = context.cacheDir.usableSpace
            if (freeSpace < 10 * 1024 * 1024) { // < 10 MB
                Log.e(TAG, "Insufficient storage space for voice recording")
                return false
            }
            
            val voiceNotesDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(voiceNotesDir, "voice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = SystemClock.elapsedRealtime()
            recordedAmplitudesList.clear()
            _amplitudes.value = emptyList()
            _durationSeconds.value = 0
            _currentAmplitude.value = 0f
            _isRecording.value = true

            startPollingAmplitudes()
            Log.d(TAG, "Voice recording started: ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording", e)
            cleanup()
            return false
        }
    }

    private fun startPollingAmplitudes() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive && _isRecording.value) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val normalized = (maxAmp / 32767f).coerceIn(0.05f, 1f)
                    _currentAmplitude.value = normalized
                    recordedAmplitudesList.add(normalized)
                    _amplitudes.value = recordedAmplitudesList.takeLast(40)

                    val elapsedMs = SystemClock.elapsedRealtime() - recordingStartTime
                    val secs = (elapsedMs / 1000).toInt()
                    _durationSeconds.value = secs

                    if (secs >= MAX_RECORDING_SECONDS) {
                        break
                    }
                } catch (e: Exception) {
                    // Ignore transient errors
                }
                delay(100)
            }
        }
    }

    fun stopRecording(): VoiceNoteRecordResult? {
        if (!_isRecording.value) return null

        amplitudeJob?.cancel()
        amplitudeJob = null

        val file = currentOutputFile
        val elapsedMs = SystemClock.elapsedRealtime() - recordingStartTime
        val durationSecs = (elapsedMs / 1000).toInt().coerceAtLeast(1)

        val recorder = mediaRecorder
        mediaRecorder = null
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            try {
                recorder?.release()
            } catch (_: Exception) {}
            _isRecording.value = false
            _currentAmplitude.value = 0f
        }

        if (file != null && file.exists() && file.length() > 0) {
            Log.d(TAG, "Voice recording finished: ${file.name}, size=${file.length()} bytes, duration=${durationSecs}s")
            val sampledAmps = sampleAmplitudes(recordedAmplitudesList, 25)
            return VoiceNoteRecordResult(
                file = file,
                durationSeconds = durationSecs,
                amplitudes = sampledAmps
            )
        } else {
            file?.delete()
            return null
        }
    }

    fun cancelRecording() {
        if (!_isRecording.value) return

        amplitudeJob?.cancel()
        amplitudeJob = null

        val recorder = mediaRecorder
        mediaRecorder = null
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling MediaRecorder", e)
        } finally {
            try {
                recorder?.release()
            } catch (_: Exception) {}
            _isRecording.value = false
            _currentAmplitude.value = 0f
            currentOutputFile?.delete()
            currentOutputFile = null
            recordedAmplitudesList.clear()
            _amplitudes.value = emptyList()
            _durationSeconds.value = 0
        }
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        _isRecording.value = false
        currentOutputFile?.delete()
        currentOutputFile = null
    }

    private fun sampleAmplitudes(list: List<Float>, count: Int): List<Float> {
        if (list.isEmpty()) return List(count) { 0.3f }
        if (list.size <= count) return list

        val step = list.size.toFloat() / count
        return (0 until count).map { i ->
            val index = (i * step).toInt().coerceIn(0, list.size - 1)
            list[index]
        }
    }
}
