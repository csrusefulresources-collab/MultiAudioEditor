package com.multiaudio.editor.audio

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class EchoParams(
    val delayLeftMs: Int,
    val delayRightMs: Int,
    val feedback: Float,
    val mix: Float,
    val pingPong: Boolean = false,
    val reverb: Boolean = false
)

object EchoPresets {
    val SHORT = EchoParams(120, 135, 0.30f, 0.60f)
    val MEDIUM = EchoParams(350, 385, 0.45f, 0.60f)
    val STEREO = EchoParams(250, 375, 0.50f, 0.70f, pingPong = true)
    val REVERB = EchoParams(0, 0, 0.75f, 0.30f, reverb = true)
}

/** Echo / reverb. Always renders in STEREO (mono audio is converted to stereo first). */
object EchoEffect {

    fun apply(src: PcmAudio, startMs: Long, endMs: Long, p: EchoParams): PcmAudio {
        val a = src.toStereo()
        val sr = a.sampleRate
        val s = a.msToFrame(startMs)
        val e = a.msToFrame(endMs)
        val len = e - s
        if (len <= 0) return a

        val tail = tailFrames(p, sr)
        val n = len + tail
        val inL = FloatArray(n)
        val inR = FloatArray(n)
        for (i in 0 until len) {
            val idx = (s + i) * 2
            inL[i] = a.samples[idx] / 32768f
            inR[i] = a.samples[idx + 1] / 32768f
        }

        val (wetL, wetR) = if (p.reverb) reverb(inL, inR, sr) else echo(inL, inR, p, sr)

        val total = a.frames
        // If the selection reaches the end of the audio, let the echo tail extend the audio.
        val outFrames = if (e >= total) max(total, s + n) else total
        val out = ShortArray(outFrames * 2)
        System.arraycopy(a.samples, 0, out, 0, a.samples.size)
        val limit = min(n, outFrames - s)
        for (i in 0 until limit) {
            val idx = (s + i) * 2
            out[idx] = mixSample(out[idx], wetL[i] * p.mix)
            out[idx + 1] = mixSample(out[idx + 1], wetR[i] * p.mix)
        }
        return PcmAudio(sr, 2, out)
    }

    private fun mixSample(s: Short, w: Float): Short =
        (s + w * 32768f).toInt().coerceIn(-32768, 32767).toShort()

    private fun tailFrames(p: EchoParams, sr: Int): Int {
        if (p.reverb) return 2 * sr
        val d = max(max(p.delayLeftMs, p.delayRightMs), 1) * sr / 1000
        val fb = p.feedback
        val repeats = if (fb < 0.02f) 1 else ceil(ln(0.01) / ln(fb.toDouble())).toInt().coerceIn(1, 12)
        return min((repeats + 1) * d, 6 * sr)
    }

    private fun echo(inL: FloatArray, inR: FloatArray, p: EchoParams, sr: Int): Pair<FloatArray, FloatArray> {
        val n = inL.size
        val dL = max(1, p.delayLeftMs * sr / 1000)
        val dR = max(1, p.delayRightMs * sr / 1000)
        val yL = FloatArray(n)
        val yR = FloatArray(n)
        val fb = p.feedback
        for (i in 0 until n) {
            if (p.pingPong) {
                // mono input bounces: left -> right -> left ...
                val xl = if (i - dL >= 0) (inL[i - dL] + inR[i - dL]) * 0.5f else 0f
                val fl = if (i - dL >= 0) yR[i - dL] else 0f
                val fr = if (i - dR >= 0) yL[i - dR] else 0f
                yL[i] = xl + fb * fl
                yR[i] = fb * fr
            } else {
                val xl = if (i - dL >= 0) inL[i - dL] else 0f
                val xr = if (i - dR >= 0) inR[i - dR] else 0f
                val fl = if (i - dL >= 0) yL[i - dL] else 0f
                val fr = if (i - dR >= 0) yR[i - dR] else 0f
                yL[i] = xl + fb * fl
                yR[i] = xr + fb * fr
            }
        }
        return Pair(yL, yR)
    }

    private fun reverb(inL: FloatArray, inR: FloatArray, sr: Int): Pair<FloatArray, FloatArray> {
        val delays = doubleArrayOf(29.7, 37.1, 41.1, 43.7, 50.3, 56.9)
        fun channel(x: FloatArray, spread: Int): FloatArray {
            val n = x.size
            val sum = FloatArray(n)
            for (dms in delays) {
                val d = (dms * sr / 1000.0).toInt() + spread
                val y = FloatArray(n)
                for (i in d until n) y[i] = x[i - d] + 0.75f * y[i - d]
                for (i in 0 until n) sum[i] += y[i] / delays.size
            }
            val a1 = allpass(sum, (5.0 * sr / 1000).toInt(), 0.5f)
            return allpass(a1, (1.7 * sr / 1000).toInt(), 0.5f)
        }
        return Pair(channel(inL, 0), channel(inR, 23))
    }

    private fun allpass(x: FloatArray, d: Int, g: Float): FloatArray {
        val y = FloatArray(x.size)
        for (i in x.indices) {
            val xd = if (i >= d) x[i - d] else 0f
            val yd = if (i >= d) y[i - d] else 0f
            y[i] = -g * x[i] + xd + g * yd
        }
        return y
    }
}
