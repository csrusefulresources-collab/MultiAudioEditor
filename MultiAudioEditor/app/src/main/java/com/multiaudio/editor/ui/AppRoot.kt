package com.multiaudio.editor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.multiaudio.editor.MainViewModel

enum class Screen { HOME, SELECTION, EDIT }

@Composable
fun AppRoot(viewModel: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(
            viewModel = viewModel,
            onOpenSelection = { screen = Screen.SELECTION },
            onOpenEdit = { screen = Screen.EDIT }
        )
        Screen.SELECTION -> PlaceholderScreen(
            title = "Selection (${viewModel.selectionMode.label})",
            audio = viewModel.selectedAudio,
            note = "Yeh screen agle step mein banegi.",
            onBack = { screen = Screen.HOME }
        )
        Screen.EDIT -> PlaceholderScreen(
            title = "Edit",
            audio = viewModel.selectedAudio,
            note = "Yeh screen agle step mein banegi.",
            onBack = { screen = Screen.HOME }
        )
    }
}
