package com.evandhardspace.loon.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.editor.filetree.FileTree
import com.evandhardspace.loon.editor.tab.TabPanel
import com.evandhardspace.loon.utils.ui.SplitPane
import java.io.File

@Composable
fun EditorScreen(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel { EditorViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SplitPane(
        modifier = modifier,
        leftContent = {
            Column {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                FileTree(
                    root = File(selectedPath),
                    onFileClick = { viewModel.onFileSelected(it.name) }
                )
            }
        },
        rightContent = {
            Column {
                TabPanel(
                    state = state,
                    onFileClosed = { viewModel.onFileClosed(it) },
                    onFileSelected = { viewModel.onFileSelected(it) },
                )
                Text("Editor: ${state.selectedFile}")
            }
        }
    )
}

