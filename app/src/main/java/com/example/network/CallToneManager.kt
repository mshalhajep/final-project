package com.example.network

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

/**
 * Central call audio-cue engine: outgoing ringback tone, incoming call ringtone with
 * vibration, call-connected chime, and the busy / call-ended cadence.
 *
 * All synthetic tones are generated as raw PCM (no asset files needed) and rendered
 * through an AudioTrack using USAGE_VOICE_COMMUNICATION so they remain audible in the
 * in-communication audio mode the call engines activate.
 */
class CallToneManager(private val context: Context) {

    companion object {
        private const val TAG = "CallToneManager"
        private const val SAMPLE_RATE = 16000

        // International ringback: dual frequency 440Hz + 480Hz, 2s ON / 4s OFF.
        private const val RINGBACK_LOW_HZ = 440.0
        private const val RINGBACK_HIGH_HZ = 480.0

        // Standard busy/ended cadence: 480Hz + 620Hz, 500ms ON / 500ms OFF x3.
        private const val BUSY_LOW_HZ = 480.0
        private const val BUSY_HIGH_HZ = 620.0
    }

    private var ringbackTrack: AudioTrack? = null
    private var incomingRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var chimeThread: Thread? = null

    init {
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun toneAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /**
     * Starts the standard international ringback tone (440Hz + 480Hz pulsed
     * 2 seconds ON, 4 seconds OFF) so the caller hears a real dialing ring.
     */
    @Synchronized
    fun playRingback() {
        stopAll()
        Thread {
            try {
                val onSamples = SAMPLE_RATE * 2
                val offSamples = SAMPLE_RATE * 4
                val cycleSamples = onSamples + offSamples
                val pcm = ShortArray(cycleSamples)
                for (i in 0 until cycleSamples) {
                    if (i < onSamples) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val mixed = 0.5 * sin(2.0 * PI * RINGBACK_LOW_HZ * t) +
                                0.5 * sin(2.0 * PI * RINGBACK_HIGH_HZ * t)
                        pcm[i] = (mixed * Short.MAX_VALUE * 0.35).toInt().toShort()
                    } else {
                        pcm[i] = 0
                    }
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(toneAttributes())
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setLoopPoints(0, cycleSamples - 1, -1)
                track.play()
                ringbackTrack = track
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play ringback tone", e)
            }
        }.start()
    }

    /**
     * Plays the device default call ringtone in a loop plus a repeating vibration
     * pattern (1s on / 1s off) for incoming call notification.
     */
    @Synchronized
    fun playIncomingRingtone() {
        stopAll()
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                if (ringtone != null) {
                    // Ringtone.setLooping exists only on Android 9+ (minSdk is 24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.isLooping = true
                    }
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    ringtone.play()
                    incomingRingtone = ringtone
                }
            }
            vibrateIncomingPattern()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play incoming ringtone", e)
        }
    }

    private fun vibrateIncomingPattern() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 1000, 1000), 0)
                }
            }
        } catch (e: Exception) {
            // Vibration is best-effort
        }
    }

    /** Short ascending 800Hz -> 1200Hz chime (200ms) when the call is connected. */
    @Synchronized
    fun playConnectedChime() {
        stopAll()
        try {
            val samples = (SAMPLE_RATE * 0.22).toInt()
            val pcm = ShortArray(samples)
            var phase = 0.0
            for (i in 0 until samples) {
                val progress = i.toDouble() / samples
                val freq = 800.0 + (400.0 * progress)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val envelope = 0.35 * (1.0 - progress)
                pcm[i] = (sin(phase) * Short.MAX_VALUE * envelope).toInt().toShort()
            }
            chimeThread = Thread {
                var track: AudioTrack? = null
                try {
                    track = AudioTrack.Builder()
                        .setAudioAttributes(toneAttributes())
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcm.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    Thread.sleep(450)
                } catch (_: InterruptedException) {
                } catch (e: Exception) {
                    Log.e(TAG, "Connected chime failed", e)
                } finally {
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: Exception) {
                    }
                }
            }.also { it.start() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play connected chime", e)
        }
    }

    /** Standard busy / call-ended cadence: 480Hz + 620Hz, 500ms on, 500ms off, 3 cycles. */
    @Synchronized
    fun playDisconnectedTone() {
        try {
            val onSamples = (SAMPLE_RATE * 0.5).toInt()
            val offSamples = (SAMPLE_RATE * 0.5).toInt()
            val cycleSamples = onSamples + offSamples
            val pcm = ShortArray(cycleSamples * 3)
            for (i in pcm.indices) {
                if (i % cycleSamples < onSamples) {
                    val t = (i % onSamples).toDouble() / SAMPLE_RATE
                    val mixed = 0.5 * sin(2.0 * PI * BUSY_LOW_HZ * t) +
                            0.5 * sin(2.0 * PI * BUSY_HIGH_HZ * t)
                    pcm[i] = (mixed * Short.MAX_VALUE * 0.3).toInt().toShort()
                } else {
                    pcm[i] = 0
                }
            }
            chimeThread = Thread {
                var track: AudioTrack? = null
                try {
                    track = AudioTrack.Builder()
                        .setAudioAttributes(toneAttributes())
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcm.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    Thread.sleep(3300)
                } catch (_: InterruptedException) {
                } catch (e: Exception) {
                    Log.e(TAG, "Disconnected tone failed", e)
                } finally {
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: Exception) {
                    }
                }
            }.also { it.start() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play disconnected tone", e)
        }
    }

    /** Stops every running tone, ringtone and vibration immediately. */
    @Synchronized
    fun stopAll() {
        chimeThread?.interrupt()
        try {
            ringbackTrack?.pause()
            ringbackTrack?.flush()
            ringbackTrack?.stop()
            ringbackTrack?.release()
        } catch (_: Exception) {
        }
        ringbackTrack = null

        try {
            incomingRingtone?.stop()
        } catch (_: Exception) {
        }
        incomingRingtone = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }
}
