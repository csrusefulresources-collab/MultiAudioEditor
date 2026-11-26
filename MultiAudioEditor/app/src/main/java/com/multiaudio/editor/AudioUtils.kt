package com.multiaudio.editor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object AudioUtils {
    fun queryName(context: Context, uri: Uri): String {
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
}
