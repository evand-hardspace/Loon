package com.evandhardspace.loon.chooser

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
import com.evandhardspace.loon.utils.OnEffect

@Composable
fun FileChooserScreen(
    modifier: Modifier = Modifier,
    onDirectorySelected: (path: String) -> Unit,
    viewModel: FileChooserViewModel = viewModel { FileChooserViewModel() },
) {
    OnEffect(viewModel.effect) { effect ->
        when (effect) {
            is FileChooserEffect.FileSelected -> onDirectorySelected(effect.selectedFilePath)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error = state.error
    if (error != null) {
        Dialog(
            onDismissRequest = viewModel::dismissError,
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
                    Button(onClick = viewModel::dismissError) {
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
                onClick = viewModel::choseFile,
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Select Directory")
            }
        }
    }
}