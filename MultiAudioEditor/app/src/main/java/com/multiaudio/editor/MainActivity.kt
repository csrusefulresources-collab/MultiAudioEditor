package com.multiaudio.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.multiaudio.editor.ui.AppRoot
import com.multiaudio.editor.ui.MultiAudioEditorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiAudioEditorTheme {
                AppRoot(viewModel)
            }
        }
    }
}
