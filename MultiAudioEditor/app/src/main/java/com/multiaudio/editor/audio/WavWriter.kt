package com.multiaudio.editor.audio

import android.content.Context
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object WavWriter {
    fun write(context: Context, uri: Uri, pcm: PcmAudio) {
        val raw = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("File open nahi ho saki")
        raw.use {
            val os = BufferedOutputStream(it, 1 shl 16)
            val ch = pcm.channels
            val sr = pcm.sampleRate
            val dataSize = pcm.samples.size * 2
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray()); h.putInt(36 + dataSize)
            h.put("WAVE".toByteArray()); h.put("fmt ".toByteArray()); h.putInt(16)
            h.putShort(1); h.putShort(ch.toShort()); h.putInt(sr)
            h.putInt(sr * ch * 2); h.putShort((ch * 2).toShort()); h.putShort(16)
            h.put("data".toByteArray()); h.putInt(dataSize)
            os.write(h.array())

            val buf = ByteBuffer.allocate(1 shl 16).order(ByteOrder.LITTLE_ENDIAN)
            var i = 0
            while (i < pcm.samples.size) {
                buf.clear()
                val n = min(buf.capacity() / 2, pcm.samples.size - i)
                for (k in 0 until n) buf.putShort(pcm.samples[i + k])
                os.write(buf.array(), 0, n * 2)
                i += n
            }
            os.flush()
        }
    }
}
