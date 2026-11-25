package com.multiaudio.editor

import android.net.Uri

data class AudioItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long
)

enum class SelectionMode(val label: String) {
    TYPING("Typing"),
    MARKER("Marker")
}
