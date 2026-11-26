package com.multiaudio.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multiaudio.editor.audio.AudioDecoder
import com.multiaudio.editor.audio.EchoEffect
import com.multiaudio.editor.audio.EchoParams
import com.multiaudio.editor.audio.PcmAudio
import com.multiaudio.editor.audio.PcmPlayer
import com.multiaudio.editor.audio.WavWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

class MainViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val MAX_AUDIOS = 4
        const val SKIP_MS = 10_000L
        private const val PREFS = "multi_audio_prefs"
        private const val KEY_MODE = "selection_mode"
    }

    private class EditResult(
        val pcm: PcmAudio,
        val startMs: Long,
        val endMs: Long,
        val clip: PcmAudio? = null
    )

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val player = PcmPlayer()
    private var nextId = 1L

    var audios by mutableStateOf<List<AudioItem>>(emptyList())
        private set
    var selectedIndex by mutableStateOf<Int?>(null)
        private set
    var selectionMode by mutableStateOf(
        runCatching { SelectionMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(SelectionMode.MARKER)
    )
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var busyText by mutableStateOf("")
        private set
    var clipboard by mutableStateOf<PcmAudio?>(null)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    val selectedAudio: AudioItem?
        get() = audios.getOrNull(selectedIndex ?: -1)

    fun clearMessage() { message = null }
    fun showMessage(text: String) { message = text }

    // ---------------- Home: settings / import / select ----------------

    fun updateSelectionMode(mode: SelectionMode) {
        selectionMode = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun canImportMore(): Boolean = audios.size < MAX_AUDIOS

    fun selectAudio(index: Int) {
        stopPlayback()
        selectedIndex = index
        positionMs = 0L
    }

    fun importAudios(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (isBusy) { message = "Thora intezar karein..."; return }
        val free = MAX_AUDIOS - audios.size
        if (free <= 0) {
            message = "Maximum $MAX_AUDIOS audios import ho sakti hain."
            return
        }
        val fresh = uris.distinct().filter { u -> audios.none { it.uri == u } }
        if (fresh.isEmpty()) {
            message = "Yeh audio pehle se import ho chuki hai."
            return
        }
        val toAdd = fresh.take(free)
        val skipped = fresh.size - toAdd.size

        isBusy = true
        busyText = "Audio import ho rahi hai..."
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val added = mutableListOf<AudioItem>()
            val failed = mutableListOf<String>()
            for (u in toAdd) {
                val name = withContext(Dispatchers.IO) { AudioUtils.queryName(ctx, u) }
                try {
                    val pcm = withContext(Dispatchers.IO) { AudioDecoder.decode(ctx, u) }
                    added += AudioItem(nextId++, u, name, pcm, 0L, pcm.durationMs)
                } catch (t: Throwable) {
                    failed += name
                }
            }
            audios = audios + added
            val parts = mutableListOf<String>()
            if (skipped > 0) parts += "Limit $MAX_AUDIOS hai, $skipped audio(s) import nahi hui."
            if (failed.isNotEmpty()) parts += "Decode nahi ho saki: ${failed.joinToString()}"
            message = if (parts.isEmpty()) null else parts.joinToString("\n")
            isBusy = false
        }
    }

    // ---------------- Playback ----------------

    fun togglePlay() {
        val a = selectedAudio ?: return
        if (isPlaying) {
            positionMs = min(player.stop(), a.durationMs)
            isPlaying = false
            return
        }
        var from = positionMs
        if (from >= a.durationMs - 50) from = 0L
        positionMs = from
        startPlay(a, from)
    }

    private fun startPlay(a: AudioItem, from: Long) {
        isPlaying = true
        player.play(
            a.pcm, from,
            onPosition = { positionMs = it },
            onFinished = { isPlaying = false; positionMs = a.durationMs }
        )
    }

    fun stopPlayback() {
        val p = player.stop()
        if (isPlaying) {
            positionMs = min(p, selectedAudio?.durationMs ?: p)
            isPlaying = false
        }
    }

    fun seekTo(ms: Long) {
        val a = selectedAudio ?: return
        positionMs = ms.coerceIn(0L, a.durationMs)
        if (isPlaying) startPlay(a, positionMs)
    }

    fun rewind() = seekTo(positionMs - SKIP_MS)
    fun fastForward() = seekTo(positionMs + SKIP_MS)

    private fun movePlayhead(ms: Long) {
        stopPlayback()
        positionMs = ms
    }

    override fun onCleared() {
        player.stop()
    }

    // ---------------- Markers ----------------

    private fun updateSelected(transform: (AudioItem) -> AudioItem) {
        val i = selectedIndex ?: return
        audios = audios.toMutableList().also { it[i] = transform(it[i]) }
    }

    fun setStartMarker(ms: Long, movePlayhead: Boolean = false) {
        val a = selectedAudio ?: return
        val v = ms.coerceIn(0L, a.durationMs)
        updateSelected { it.copy(startMs = v, endMs = maxOf(it.endMs, v)) }
        if (movePlayhead) movePlayhead(v)
    }

    fun setEndMarker(ms: Long, movePlayhead: Boolean = false) {
        val a = selectedAudio ?: return
        val v = ms.coerceIn(0L, a.durationMs)
        updateSelected { it.copy(endMs = v, startMs = minOf(it.startMs, v)) }
        if (movePlayhead) movePlayhead(v)
    }

    fun setStartFromPlayhead() = setStartMarker(positionMs)
    fun setEndFromPlayhead() = setEndMarker(positionMs)

    /** Returns true if it can keep going (limit not reached) - used by the repeat button. */
    fun nudgeStart(delta: Long): Boolean {
        val a = selectedAudio ?: return false
        val target = a.startMs + delta
        val v = target.coerceIn(0L, a.endMs)
        if (v != a.startMs) {
            updateSelected { it.copy(startMs = v) }
            movePlayhead(v)
        }
        return v == target
    }

    fun nudgeEnd(delta: Long): Boolean {
        val a = selectedAudio ?: return false
        val target = a.endMs + delta
        val v = target.coerceIn(a.startMs, a.durationMs)
        if (v != a.endMs) {
            updateSelected { it.copy(endMs = v) }
            movePlayhead(v)
        }
        return v == target
    }

    fun moveStartToBeginning() = setStartMarker(0L, true)
    fun moveFinishToEnd() {
        val a = selectedAudio ?: return
        setEndMarker(a.durationMs, true)
    }

    // ---------------- Edit operations ----------------

    private fun requireSelection(needRange: Boolean = true): AudioItem? {
        val a = selectedAudio
        if (a == null) { message = "Pehle list se audio select karein."; return null }
        if (needRange && a.endMs <= a.startMs) {
            message = "Pehle Selection mein Start aur End marker set karein."
            return null
        }
        return a
    }

    private fun runEdit(okMessage: String, needRange: Boolean = true, work: (AudioItem) -> EditResult) {
        if (isBusy) return
        val a = requireSelection(needRange) ?: return
        stopPlayback()
        isBusy = true
        busyText = "Processing..."
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.Default) { work(a) }
                val idx = audios.indexOfFirst { it.id == a.id }
                if (idx >= 0) {
                    audios = audios.toMutableList().also {
                        it[idx] = it[idx].copy(pcm = res.pcm, startMs = res.startMs, endMs = res.endMs)
                    }
                }
                if (res.clip != null) clipboard = res.clip
                positionMs = min(res.startMs, res.pcm.durationMs)
                message = okMessage
            } catch (t: Throwable) {
                message = if (t is IllegalStateException) t.message else "Edit fail ho gaya: ${t.message ?: t.javaClass.simpleName}"
            }
            isBusy = false
        }
    }

    fun trim() = runEdit("Trim ho gaya.") { a ->
        val p = a.pcm.slice(a.startMs, a.endMs)
        if (p.frames == 0) throw IllegalStateException("Selection bohat chhoti hai.")
        EditResult(p, 0L, p.durationMs)
    }

    fun deleteSelection() = runEdit("Selected hissa delete ho gaya.") { a ->
        val p = a.pcm.delete(a.startMs, a.endMs)
        if (p.frames == 0) throw IllegalStateException("Poora audio delete nahi kiya ja sakta.")
        EditResult(p, 0L, p.durationMs)
    }

    fun copySelection() {
        val a = requireSelection() ?: return
        val clip = a.pcm.slice(a.startMs, a.endMs)
        clipboard = clip
        message = "Copy ho gaya (${formatDuration(clip.durationMs)})."
    }

    fun cutSelection() = runEdit("Cut ho gaya.") { a ->
        val clip = a.pcm.slice(a.startMs, a.endMs)
        val p = a.pcm.delete(a.startMs, a.endMs)
        if (p.frames == 0) throw IllegalStateException("Poora audio cut nahi kiya ja sakta.")
        EditResult(p, 0L, p.durationMs, clip)
    }

    fun paste() {
        val clip = clipboard
        if (clip == null) { message = "Clipboard khali hai. Pehle Copy ya Cut karein."; return }
        runEdit("Paste ho gaya.", needRange = false) { a ->
            val c = clip.convertTo(a.pcm.sampleRate, a.pcm.channels)
            val p = a.pcm.insert(c, a.startMs)
            EditResult(p, a.startMs, min(p.durationMs, a.startMs + c.durationMs))
        }
    }

    fun applyEcho(params: EchoParams, name: String) =
        runEdit("$name lag gaya (Stereo mode).") { a ->
            val p = EchoEffect.apply(a.pcm, a.startMs, a.endMs, params)
            EditResult(p, a.startMs, min(a.endMs, p.durationMs))
        }

    // ---------------- Save ----------------

    fun suggestedSaveName(): String {
        val n = selectedAudio?.name ?: "audio"
        return n.substringBeforeLast('.', n) + "_edited.wav"
    }

    fun saveWav(uri: Uri) {
        val a = selectedAudio ?: return
        if (isBusy) return
        stopPlayback()
        isBusy = true
        busyText = "Save ho raha hai..."
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            message = try {
                withContext(Dispatchers.IO) { WavWriter.write(ctx, uri, a.pcm) }
                "Save ho gaya (WAV)."
            } catch (t: Throwable) {
                "Save fail: ${t.message ?: t.javaClass.simpleName}"
            }
            isBusy = false
        }
    }
}
