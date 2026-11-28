package com.multiaudio.editor.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import java.nio.ShortBuffer

/** Decodes any Android-supported audio file (mp3, aac, m4a, wav, ogg, flac...) to 16-bit PCM. */
object AudioDecoder {

    private class ShortList {
        var data = ShortArray(1 shl 18)
        var size = 0
        fun append(sb: ShortBuffer) {
            val n = sb.remaining()
            if (size + n > data.size) data = data.copyOf(maxOf(data.size * 2, size + n))
            sb.get(data, size, n)
            size += n
        }
        fun toArray(): ShortArray = data.copyOf(size)
    }

    fun decode(context: Context, uri: Uri, onProgress: (Float) -> Unit = {}): PcmAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalStateException("Audio track nahi mila")
        }
        extractor.selectTrack(trackIndex)

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
            format.getLong(MediaFormat.KEY_DURATION) else 0L
        var lastPct = -1

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        val list = ShortList()
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val st = extractor.sampleTime
                            codec.queueInputBuffer(inIdx, 0, size, st, 0)
                            if (durationUs > 0 && st >= 0) {
                                val pct = ((st * 100) / durationUs).toInt().coerceIn(0, 99)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct / 100f)
                                }
                            }
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    if (info.size > 0) {
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        list.append(buf.order(ByteOrder.nativeOrder()).asShortBuffer())
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = codec.outputFormat
                    if (nf.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                        sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                        channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
            extractor.release()
        }

        var arr = list.toArray()
        if (channels > 2) { // keep it simple: first two channels
            val fr = arr.size / channels
            val out = ShortArray(fr * 2)
            for (i in 0 until fr) {
                out[2 * i] = arr[i * channels]
                out[2 * i + 1] = arr[i * channels + 1]
            }
            arr = out
            channels = 2
        }
        if (arr.size < channels) throw IllegalStateException("Audio khali hai")
        return PcmAudio(sampleRate, channels, arr)
    }
}
