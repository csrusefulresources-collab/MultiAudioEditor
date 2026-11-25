package com.multiaudio.editor

/** Format: HH:MM:SS:mmm  (e.g. 00:03:25:480) */
fun formatDuration(ms: Long): String {
    val total = if (ms < 0) 0 else ms
    val h = total / 3_600_000
    val m = (total / 60_000) % 60
    val s = (total / 1000) % 60
    val milli = total % 1000
    return "%02d:%02d:%02d:%03d".format(h, m, s, milli)
}
