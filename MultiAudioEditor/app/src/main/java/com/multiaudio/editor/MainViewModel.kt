package com.multiaudio.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val MAX_AUDIOS = 4
        private const val PREFS = "multi_audio_prefs"
        private const val KEY_MODE = "selection_mode"
    }

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var audios by mutableStateOf<List<AudioItem>>(emptyList())
        private set

    /** Index of the audio the user tapped in the Home list (null = none). */
    var selectedIndex by mutableStateOf<Int?>(null)
        private set

    var selectionMode by mutableStateOf(
        runCatching { SelectionMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(SelectionMode.MARKER)
    )
        private set

    var message by mutableStateOf<String?>(null)
        private set

    val selectedAudio: AudioItem?
        get() = selectedIndex?.let { audios.getOrNull(it) }

    fun clearMessage() {
        message = null
    }

    fun showMessage(text: String) {
        message = text
    }

    fun selectAudio(index: Int) {
        selectedIndex = index
    }

    fun updateSelectionMode(mode: SelectionMode) {
        selectionMode = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun canImportMore(): Boolean = audios.size < MAX_AUDIOS

    fun importAudios(uris: List<Uri>) {
        if (uris.isEmpty()) return
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

        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val items = withContext(Dispatchers.IO) {
                toAdd.map { AudioUtils.readAudioItem(ctx, it) }
            }
            audios = audios + items
            message = if (skipped > 0) {
                "Limit $MAX_AUDIOS hai, $skipped audio(s) import nahi hui."
            } else null
        }
    }
}
