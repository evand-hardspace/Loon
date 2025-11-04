package com.evandhardspace.loon.features.scene.filechooser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.coreutils.OnEffect
import com.evandhardspace.loon.presentation.FileChooser

@Composable
fun FileChooserScene(
    modifier: Modifier = Modifier,
    onDirectorySelected: (path: String) -> Unit,
) {
    val viewModel: FileChooserViewModel = viewModel { FileChooserViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnEffect(viewModel.effect) { effect ->
        when (effect) {
            is FileChooserEffect.FileSelected -> onDirectorySelected(effect.selectedFilePath)
        }
    }
    FileChooserContent(
        modifier = modifier,
        state = state,
        perform = viewModel::onAction,
    )
}

@Composable
internal fun FileChooserContent(
    state: FileChooserState,
    perform: FileChooserAction.() -> Unit,
    modifier: Modifier = Modifier,
) {


    val error = state.error
    if (error != null) {
        Dialog(
            onDismissRequest = { FileChooserAction.DismissError.perform() },
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error)
                    Button(onClick = { FileChooserAction.DismissError.perform() }) {
                        Text("Ok")
                    }
                }
            }
        }
    }
    Surface(
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {
                    FileChooser()
                        .selectDirectory()
                        .let { FileChooserAction.ChoseFile(it) }
                        .perform()
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Select Directory")
            }
        }
    }
}