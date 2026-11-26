package com.multiaudio.editor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multiaudio.editor.MainViewModel
import com.multiaudio.editor.StepPresets
import com.multiaudio.editor.stepLabel
import kotlinx.coroutines.delay

@Composable
fun MessageEffect(vm: MainViewModel, snackbar: SnackbarHostState) {
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
}

/** Pill button that supports single tap and double tap. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TapButton(
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick),
        shape = RoundedCornerShape(50),
        color = if (highlighted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 12.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Single tap = one step.
 * Double tap = keeps stepping continuously; tap once to stop (also stops at the limit).
 */
@Composable
fun RepeatButton(label: String, modifier: Modifier = Modifier, onStep: () -> Boolean) {
    var repeating by remember { mutableStateOf(false) }
    val step by rememberUpdatedState(onStep)

    LaunchedEffect(repeating) {
        if (repeating) {
            while (true) {
                if (!step()) {
                    repeating = false
                    break
                }
                delay(120)
            }
        }
    }

    TapButton(
        label = if (repeating) "Running...\nTap to stop" else label,
        modifier = modifier,
        highlighted = repeating,
        onDoubleClick = { repeating = true },
        onClick = { if (repeating) repeating = false else step() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCombo(index: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = stepLabel(StepPresets[index]),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StepPresets.forEachIndexed { i, ms ->
                DropdownMenuItem(
                    text = { Text(stepLabel(ms)) },
                    onClick = { onChange(i); expanded = false }
                )
            }
        }
    }
}
