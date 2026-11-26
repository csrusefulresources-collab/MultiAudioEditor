package com.multiaudio.editor.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.min

/** Streams in-memory PCM through AudioTrack. Callbacks run on the main thread. */
class PcmPlayer {

    private class Job(val sampleRate: Int) {
        @Volatile var active = true
        @Volatile var frame = 0
    }

    private var job: Job? = null
    private val main = Handler(Looper.getMainLooper())

    fun play(audio: PcmAudio, fromMs: Long, onPosition: (Long) -> Unit, onFinished: () -> Unit) {
        stop()
        val ch = audio.channels
        val sr = audio.sampleRate
        val startFrame = audio.msToFrame(fromMs)
        if (startFrame >= audio.frames) {
            onFinished()
            return
        }
        val mask = if (ch == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuf = AudioTrack.getMinBufferSize(sr, mask, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sr)
                    .setChannelMask(mask)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val j = Job(sr)
        j.frame = startFrame
        job = j

        Thread {
            var finished = false
            try {
                track.play()
                var frame = startFrame
                while (j.active && frame < audio.frames) {
                    val n = min(4096, audio.frames - frame)
                    val written = track.write(audio.samples, frame * ch, n * ch)
                    if (written <= 0) break
                    frame += written / ch
                    val played = min(startFrame + track.playbackHeadPosition, audio.frames)
                    j.frame = played
                    if (j.active) main.post { if (j.active) onPosition(played * 1000L / sr) }
                }
                if (j.active) {
                    val target = audio.frames - startFrame
                    val deadline = System.currentTimeMillis() + 1500
                    while (j.active && track.playbackHeadPosition < target &&
                        System.currentTimeMillis() < deadline
                    ) {
                        Thread.sleep(20)
                    }
                    finished = j.active
                }
            } catch (_: Exception) {
            } finally {
                try { track.stop() } catch (_: Exception) {}
                track.release()
            }
            if (finished) {
                j.active = false
                main.post { onFinished() }
            }
        }.start()
    }

    /** Stops playback and returns the last played position in ms. */
    fun stop(): Long {
        val j = job ?: return 0L
        job = null
        j.active = false
        return j.frame * 1000L / j.sampleRate
    }
}
