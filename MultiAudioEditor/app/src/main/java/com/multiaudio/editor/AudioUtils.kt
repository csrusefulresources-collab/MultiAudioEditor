package com.multiaudio.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

object AudioUtils {

    fun readAudioItem(context: Context, uri: Uri): AudioItem {
        return AudioItem(
            uri = uri,
            name = queryName(context, uri),
            durationMs = queryDuration(context, uri)
        )
    }

    private fun queryName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val n = c.getString(idx)
                        if (!n.isNullOrBlank()) return n
                    }
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: "Unknown audio"
    }

    private fun queryDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
