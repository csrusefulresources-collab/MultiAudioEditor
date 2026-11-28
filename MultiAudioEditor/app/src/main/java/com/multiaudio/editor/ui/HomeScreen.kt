package com.multiaudio.editor.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiaudio.editor.MainViewModel
import com.multiaudio.editor.SelectionMode
import com.multiaudio.editor.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenSelection: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var showEdit by rememberSaveable { mutableStateOf(false) }

    MessageEffect(viewModel, snackbar)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
        viewModel.importAudios(uris)
    }

    // Final save of the edited audio (asks where to store the WAV file)
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri -> if (uri != null) viewModel.saveWav(uri) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Multi Audio Editor", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Import button + (after editing) Save button on its right
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (viewModel.canImportMore()) {
                                picker.launch(arrayOf("audio/*"))
                            } else {
                                viewModel.showMessage(
                                    "Maximum ${MainViewModel.MAX_AUDIOS} audios import ho sakti hain."
                                )
                            }
                        },
                        enabled = !viewModel.isBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Multiple Import Audios") }

                    if (viewModel.hasEdits) {
                        Button(
                            onClick = {
                                if (viewModel.canSave()) saveLauncher.launch(viewModel.suggestedSaveName())
                            }
                        ) { Text("Save") }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (viewModel.selectedAudio == null) {
                                viewModel.showMessage("Pehle list se koi audio select karein.")
                            } else onOpenSelection()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Selection") }

                    if (showEdit) {
                        Button(onClick = { showEdit = false }, modifier = Modifier.weight(1f)) { Text("Edit") }
                    } else {
                        OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.weight(1f)) { Text("Edit") }
                    }

                    OutlinedButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Settings") }
                }
            }

            // Import progress (with percentage) or generic busy bar
            if (viewModel.importing) {
                item {
                    val p = viewModel.importProgress.coerceIn(0f, 1f)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Importing ${viewModel.importLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                            Text(
                                "${(p * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (viewModel.isBusy) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(viewModel.busyText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (showEdit) {
                item { EditPanel(viewModel) }
            }

            // Heading + list only when something is imported
            if (viewModel.audios.isNotEmpty()) {
                item {
                    Text(
                        "Imported Audios (${viewModel.audios.size}/${MainViewModel.MAX_AUDIOS})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                itemsIndexed(viewModel.audios) { index, audio ->
                    val selected = viewModel.selectedIndex == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectAudio(index) },
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                audio.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Duration: ${formatDuration(audio.durationMs)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (selected) {
                                Text(
                                    "Selected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = viewModel.selectionMode,
            onChange = { viewModel.updateSelectionMode(it) },
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    current: SelectionMode,
    onChange: (SelectionMode) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selection By", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = current.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SelectionMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = { onChange(mode); expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
