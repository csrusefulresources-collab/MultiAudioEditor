package com.multiaudio.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiaudio.editor.MainViewModel
import com.multiaudio.editor.audio.EchoPresets
import com.multiaudio.editor.formatDuration

/** Shown on the Home screen when the "Edit" button is tapped. */
@Composable
fun EditPanel(vm: MainViewModel) {
    var showEcho by rememberSaveable { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    val a = vm.selectedAudio
    val enabled = !vm.isBusy

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edit", style = MaterialTheme.typography.titleSmall)
            Text(
                if (a == null) "Audio select nahi hai."
                else "${a.name}\nSelection: ${formatDuration(a.startMs)}  ->  ${formatDuration(a.endMs)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Clipboard: " + (vm.clipboard?.let { formatDuration(it.durationMs) } ?: "khali"),
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditBtn("Trim", enabled) { vm.trim() }
                EditBtn("Delete", enabled) { vm.deleteSelection() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditBtn("Copy", enabled) { vm.copySelection() }
                EditBtn("Cut", enabled) { vm.cutSelection() }
                EditBtn("Paste", enabled && vm.clipboard != null) { vm.paste() }
            }
            Button(onClick = { showEcho = !showEcho }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showEcho) "Echo  (hide)" else "Echo")
            }
            if (showEcho) {
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EchoBtn("Short Echo", enabled) { vm.applyEcho(EchoPresets.SHORT, "Short Echo") }
                    EchoBtn("Medium Echo", enabled) { vm.applyEcho(EchoPresets.MEDIUM, "Medium Echo") }
                    EchoBtn("Stereo Echo", enabled) { vm.applyEcho(EchoPresets.STEREO, "Stereo Echo") }
                    EchoBtn("Reverb Echo", enabled) { vm.applyEcho(EchoPresets.REVERB, "Reverb Echo") }
                    EchoBtn("Custom Echo", enabled) { showCustom = true }
                }
            }
        }
    }

    if (showCustom) {
        CustomEchoDialog(
            onDismiss = { showCustom = false },
            onApply = { params ->
                showCustom = false
                vm.applyEcho(params, "Custom Echo")
            }
        )
    }
}

@Composable
private fun RowScope.EditBtn(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) { Text(text) }
}

@Composable
private fun EchoBtn(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
