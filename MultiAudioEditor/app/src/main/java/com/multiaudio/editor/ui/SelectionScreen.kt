package com.multiaudio.editor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multiaudio.editor.AudioItem
import com.multiaudio.editor.MainViewModel
import com.multiaudio.editor.SelectionMode
import com.multiaudio.editor.StepPresets
import com.multiaudio.editor.formatDuration
import com.multiaudio.editor.stepLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(vm: MainViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(vm, snackbar)

    DisposableEffect(Unit) { onDispose { vm.stopPlayback() } }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri -> if (uri != null) vm.saveWav(uri) }

    val audio = vm.selectedAudio

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selection (${vm.selectionMode.label})") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        if (audio == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Koi audio select nahi hai.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name + total duration
                Text(audio.name, style = MaterialTheme.typography.titleMedium)
                Text("Total Duration: ${formatDuration(audio.durationMs)}")

                // Playhead
                var dragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }
                Text(
                    "Current Position: ${formatDuration(if (dragging) dragValue.toLong() else vm.positionMs)}",
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = if (dragging) dragValue else vm.positionMs.toFloat(),
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = { dragging = false; vm.seekTo(dragValue.toLong()) },
                    valueRange = 0f..audio.durationMs.toFloat().coerceAtLeast(1f)
                )

                HorizontalDivider()

                if (vm.selectionMode == SelectionMode.MARKER) {
                    MarkerSection(vm, audio)
                } else {
                    TypingSection(vm, audio)
                }

                HorizontalDivider()

                // Jump buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.moveStartToBeginning() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) { Text("Move Start to Beginning", fontSize = 12.sp, textAlign = TextAlign.Center) }
                    OutlinedButton(
                        onClick = { vm.moveFinishToEnd() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) { Text("Move Finish to End", fontSize = 12.sp, textAlign = TextAlign.Center) }
                }

                // Universal media controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.rewind() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("<< Rewind\n10 sec", fontSize = 12.sp, textAlign = TextAlign.Center) }
                    Button(
                        onClick = { vm.togglePlay() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text(if (vm.isPlaying) "Pause" else "Play", fontSize = 14.sp) }
                    OutlinedButton(
                        onClick = { vm.fastForward() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("Fast Forward\n10 sec >>", fontSize = 12.sp, textAlign = TextAlign.Center) }
                }

                Spacer(Modifier.padding(2.dp))

                Button(
                    onClick = { saveLauncher.launch(vm.suggestedSaveName()) },
                    enabled = !vm.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save") }
            }
        }
    }
}

// ------------------------- Marker mode -------------------------

@Composable
private fun MarkerSection(vm: MainViewModel, a: AudioItem) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MarkerBlock(
            title = "Start Marker",
            valueMs = a.startMs,
            setLabel = "Set Start Marker",
            onSet = { vm.setStartFromPlayhead() },
            onNudge = { vm.nudgeStart(it) }
        )
        MarkerBlock(
            title = "End Marker",
            valueMs = a.endMs,
            setLabel = "Set End Marker",
            onSet = { vm.setEndFromPlayhead() },
            onNudge = { vm.nudgeEnd(it) }
        )
    }
}

@Composable
private fun MarkerBlock(
    title: String,
    valueMs: Long,
    setLabel: String,
    onSet: () -> Unit,
    onNudge: (Long) -> Boolean
) {
    var stepIndex by remember { mutableIntStateOf(6) } // default: 1 second
    val step = StepPresets[stepIndex]
    val label = stepLabel(step)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(formatDuration(valueMs), fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        Button(onClick = onSet, modifier = Modifier.fillMaxWidth()) { Text(setLabel) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepCombo(stepIndex, { stepIndex = it }, Modifier.weight(1.3f))
            RepeatButton("Decrease\n$label", Modifier.weight(1f)) { onNudge(-step) }
            RepeatButton("Increase\n$label", Modifier.weight(1f)) { onNudge(step) }
        }
    }
}

// ------------------------- Typing mode -------------------------

@Composable
private fun TypingSection(vm: MainViewModel, a: AudioItem) {
    var editing by remember { mutableStateOf(0) } // 1 = start, 2 = end

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TypingRow("Start Time", a.startMs, "Enter Start Duration", { editing = 1 }) {
            vm.showMessage("Typing box kholne ke liye button par double-tap karein.")
        }
        TypingRow("End Time", a.endMs, "Enter End Duration", { editing = 2 }) {
            vm.showMessage("Typing box kholne ke liye button par double-tap karein.")
        }
    }

    if (editing == 1) {
        TimeInputDialog(
            title = "Enter Start Time",
            durationMs = a.durationMs,
            initialMs = a.startMs,
            onDismiss = { editing = 0 },
            onOk = { vm.setStartMarker(it, true); editing = 0 }
        )
    } else if (editing == 2) {
        TimeInputDialog(
            title = "Enter End Time",
            durationMs = a.durationMs,
            initialMs = a.endMs,
            onDismiss = { editing = 0 },
            onOk = { vm.setEndMarker(it, true); editing = 0 }
        )
    }
}

@Composable
private fun TypingRow(
    title: String,
    valueMs: Long,
    buttonLabel: String,
    onOpen: () -> Unit,
    onSingleTap: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(formatDuration(valueMs), fontWeight = FontWeight.Bold)
            TapButton(
                label = buttonLabel,
                modifier = Modifier.weight(1f),
                onDoubleClick = onOpen,
                onClick = onSingleTap
            )
        }
    }
}
