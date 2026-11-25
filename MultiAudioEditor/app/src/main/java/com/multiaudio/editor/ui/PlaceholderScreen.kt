package com.multiaudio.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiaudio.editor.AudioItem
import com.multiaudio.editor.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, audio: AudioItem?, note: String, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (audio != null) {
                Text(audio.name, style = MaterialTheme.typography.titleMedium)
                Text("Duration: ${formatDuration(audio.durationMs)}")
            }
            Text(note)
            Button(onClick = onBack) { Text("Back to Home") }
        }
    }
}
