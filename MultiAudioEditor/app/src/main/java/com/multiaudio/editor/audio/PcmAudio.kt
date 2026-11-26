package com.multiaudio.editor.audio

import kotlin.math.min

/** 16-bit interleaved PCM audio held in memory. All edits return a NEW PcmAudio. */
class PcmAudio(val sampleRate: Int, val channels: Int, val samples: ShortArray) {

    val frames: Int get() = samples.size / channels
    val durationMs: Long get() = frames * 1000L / sampleRate

    fun msToFrame(ms: Long): Int =
        (ms * sampleRate / 1000L).coerceIn(0L, frames.toLong()).toInt()

    /** Keep only [startMs, endMs). */
    fun slice(startMs: Long, endMs: Long): PcmAudio {
        val s = msToFrame(startMs)
        val e = msToFrame(endMs).coerceAtLeast(s)
        return PcmAudio(sampleRate, channels, samples.copyOfRange(s * channels, e * channels))
    }

    /** Remove [startMs, endMs). */
    fun delete(startMs: Long, endMs: Long): PcmAudio {
        val s = msToFrame(startMs)
        val e = msToFrame(endMs).coerceAtLeast(s)
        val out = ShortArray(samples.size - (e - s) * channels)
        System.arraycopy(samples, 0, out, 0, s * channels)
        System.arraycopy(samples, e * channels, out, s * channels, samples.size - e * channels)
        return PcmAudio(sampleRate, channels, out)
    }

    /** Insert [clip] at [atMs] (clip is converted to this audio's format first). */
    fun insert(clip: PcmAudio, atMs: Long): PcmAudio {
        val c = clip.convertTo(sampleRate, channels)
        val at = msToFrame(atMs) * channels
        val out = ShortArray(samples.size + c.samples.size)
        System.arraycopy(samples, 0, out, 0, at)
        System.arraycopy(c.samples, 0, out, at, c.samples.size)
        System.arraycopy(samples, at, out, at + c.samples.size, samples.size - at)
        return PcmAudio(sampleRate, channels, out)
    }

    fun toStereo(): PcmAudio = if (channels == 2) this else convertTo(sampleRate, 2)

    fun convertTo(rate: Int, ch: Int): PcmAudio {
        if (rate == sampleRate && ch == channels) return this
        var cur = this
        if (cur.channels != ch) cur = cur.changeChannels(ch)
        if (cur.sampleRate != rate) cur = cur.resample(rate)
        return cur
    }

    private fun changeChannels(ch: Int): PcmAudio {
        val n = frames
        val out = ShortArray(n * ch)
        if (channels == 1) {
            for (i in 0 until n) for (c in 0 until ch) out[i * ch + c] = samples[i]
        } else if (ch == 1) {
            for (i in 0 until n) {
                var sum = 0
                for (c in 0 until channels) sum += samples[i * channels + c].toInt()
                out[i] = (sum / channels).toShort()
            }
        } else {
            for (i in 0 until n) for (c in 0 until ch) {
                out[i * ch + c] = samples[i * channels + min(c, channels - 1)]
            }
        }
        return PcmAudio(sampleRate, ch, out)
    }

    private fun resample(rate: Int): PcmAudio {
        val n = frames
        if (n == 0) return PcmAudio(rate, channels, ShortArray(0))
        val newN = (n.toLong() * rate / sampleRate).toInt()
        val out = ShortArray(newN * channels)
        val ratio = sampleRate.toDouble() / rate
        for (i in 0 until newN) {
            val pos = i * ratio
            val i0 = min(pos.toInt(), n - 1)
            val i1 = min(i0 + 1, n - 1)
            val frac = (pos - i0).toFloat()
            for (c in 0 until channels) {
                val a = samples[i0 * channels + c].toInt()
                val b = samples[i1 * channels + c].toInt()
                out[i * channels + c] = (a + (b - a) * frac).toInt().toShort()
            }
        }
        return PcmAudio(rate, channels, out)
    }
}
