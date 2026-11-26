package com.multiaudio.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.multiaudio.editor.audio.EchoParams

@Composable
fun TimeInputDialog(
    title: String,
    durationMs: Long,
    initialMs: Long,
    onDismiss: () -> Unit,
    onOk: (Long) -> Unit
) {
    // Dynamic boxes: < 1 hour => no Hours, < 1 minute => no Hours and no Minutes
    val showH = durationMs >= 3_600_000L
    val showM = durationMs >= 60_000L

    var h by remember { mutableStateOf((initialMs / 3_600_000L).toString()) }
    var m by remember {
        mutableStateOf((if (showH) (initialMs / 60_000L) % 60 else initialMs / 60_000L).toString())
    }
    var s by remember { mutableStateOf(((initialMs / 1000L) % 60).toString()) }
    var ms by remember { mutableStateOf((initialMs % 1000L).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val fields = buildList<Triple<String, String, (String) -> Unit>> {
        if (showH) add(Triple<String, String, (String) -> Unit>("Hours", h, { h = it }))
        if (showM) add(Triple<String, String, (String) -> Unit>("Minutes", m, { m = it }))
        add(Triple<String, String, (String) -> Unit>("Seconds", s, { s = it }))
        add(Triple<String, String, (String) -> Unit>("Milliseconds", ms, { ms = it }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.chunked(2).forEach { rowFields ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowFields.forEach { (label, value, setter) ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = { v ->
                                    val maxLen = if (label == "Milliseconds") 3 else 2
                                    if (v.length <= maxLen && v.all { c -> c.isDigit() }) setter(v)
                                },
                                label = { Text(label) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = {
                val hh = if (showH) h.toLongOrNull() ?: 0L else 0L
                val mm = if (showM) m.toLongOrNull() ?: 0L else 0L
                val ss = s.toLongOrNull() ?: 0L
                val mss = ms.toLongOrNull() ?: 0L
                val total = hh * 3_600_000L + mm * 60_000L + ss * 1000L + mss
                when {
                    mm >= 60 || ss >= 60 -> error = "Minutes aur Seconds 0 se 59 tak hon."
                    total > durationMs -> error = "Time audio ki duration se zyada hai."
                    else -> onOk(total)
                }
            }) { Text("OK") }
        }
    )
}

@Composable
fun CustomEchoDialog(onDismiss: () -> Unit, onApply: (EchoParams) -> Unit) {
    var delay by remember { mutableFloatStateOf(300f) }
    var feedback by remember { mutableFloatStateOf(0.4f) }
    var mix by remember { mutableFloatStateOf(0.6f) }
    var spread by remember { mutableFloatStateOf(30f) }
    var pingPong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Echo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Delay: ${delay.toInt()} ms")
                Slider(value = delay, onValueChange = { delay = it }, valueRange = 20f..1000f)
                Text("Feedback (repeats): ${(feedback * 100).toInt()}%")
                Slider(value = feedback, onValueChange = { feedback = it }, valueRange = 0f..0.9f)
                Text("Echo volume (mix): ${(mix * 100).toInt()}%")
                Slider(value = mix, onValueChange = { mix = it }, valueRange = 0f..1f)
                Text("Stereo spread (Right delay +): ${spread.toInt()} ms")
                Slider(value = spread, onValueChange = { spread = it }, valueRange = 0f..200f)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ping-pong (Left <-> Right)")
                    Switch(checked = pingPong, onCheckedChange = { pingPong = it })
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = {
                onApply(
                    EchoParams(
                        delayLeftMs = delay.toInt(),
                        delayRightMs = (delay + spread).toInt(),
                        feedback = feedback,
                        mix = mix,
                        pingPong = pingPong
                    )
                )
            }) { Text("Apply") }
        }
    )
}
