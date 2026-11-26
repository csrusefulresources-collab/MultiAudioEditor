package com.multiaudio.editor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.multiaudio.editor.MainViewModel

enum class Screen { HOME, SELECTION }

@Composable
fun AppRoot(viewModel: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(
            viewModel = viewModel,
            onOpenSelection = { screen = Screen.SELECTION }
        )
        Screen.SELECTION -> SelectionScreen(
            vm = viewModel,
            onBack = { screen = Screen.HOME }
        )
    }
}
