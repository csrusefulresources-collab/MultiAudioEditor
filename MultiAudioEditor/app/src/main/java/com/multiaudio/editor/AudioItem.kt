package com.multiaudio.editor

import android.net.Uri
import com.multiaudio.editor.audio.PcmAudio

data class AudioItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val pcm: PcmAudio,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = pcm.durationMs
}

enum class SelectionMode(val label: String) {
    TYPING("Typing"),
    MARKER("Marker")
}

/** Fine-tuning presets: 1 ms ... 30 minutes */
val StepPresets: List<Long> = listOf(
    1L, 5L, 10L, 50L, 100L, 500L,
    1_000L, 5_000L, 10_000L, 30_000L,
    60_000L, 300_000L, 600_000L, 1_800_000L
)

fun stepLabel(ms: Long): String = when {
    ms < 1_000L -> "$ms ms"
    ms < 60_000L -> (ms / 1000).let { if (it == 1L) "1 second" else "$it seconds" }
    else -> (ms / 60_000).let { if (it == 1L) "1 minute" else "$it minutes" }
}
