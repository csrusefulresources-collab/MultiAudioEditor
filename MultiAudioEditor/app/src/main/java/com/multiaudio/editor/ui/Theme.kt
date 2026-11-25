package com.multiaudio.editor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF00897B)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF4DB6AC)
)

@Composable
fun MultiAudioEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
