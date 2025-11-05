package com.evandhardspace.loon.scene.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.features.filetree.FileTree
import com.evandhardspace.loon.features.filetree.ImageViewScreen
import com.evandhardspace.loon.features.tab.TabPanel
import com.evandhardspace.loon.features.texteditor.TextEditorGlobalViewModel
import com.evandhardspace.loon.features.texteditor.TextEditorScreen
import com.evandhardspace.loon.coreutils.ui.SplitPane
import com.evandhardspace.loon.features.tab.TabViewModel
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.getState
import java.io.File

@Composable
fun EditorScene(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tabViewModel: TabViewModel = viewModel { TabViewModel() }, // TODO get rid on this level
    textEditorViewModel: TextEditorGlobalViewModel = viewModel { TextEditorGlobalViewModel() },
) {
    val state by tabViewModel.state.collectAsStateWithLifecycle()
    val selectedFileState: SelectedFileState = remember { getState() }

    SplitPane(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                            event.isMetaPressed &&
                            event.key == Key.W -> {
                        selectedFileState.selectedFile?.let {
                            tabViewModel.onTabClosed(it)
                            textEditorViewModel.removeHolder(it.path)
                        }
                        true
                    }

                    else -> false
                }
            },
        leftContent = {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(20.dp),
                ) {
                    Icon(
                        modifier = Modifier,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                FileTree(
                    root = File(selectedPath),
                    onFileSelect = {
                        selectedFileState.selectFile(it)
                        if (it.isDirectory.not()) {
                            tabViewModel.addTab(it)
                        }
                        if (it.isFile) {
                            textEditorViewModel.addHolder(it)
                        }
                    },
                    onDeleteFile = {
                        tabViewModel.onTabClosed(it)
                        textEditorViewModel.removeHolder(it.path)
                    },
                )
            }
        },
        rightContent = {
            Column {
                TabPanel(
                    state = state,
                    onTabClosed = { file ->
                        tabViewModel.onTabClosed(file)
                        textEditorViewModel.removeHolder(file.path)
                    },
                    onTabClick = { file ->
                        tabViewModel.addTab(file)
                        selectedFileState.selectFile(file)
                    },
                )
                if (selectedFileState.selectedFile?.extension?.lowercase()
                        ?.let { it == "png" || it == "jpg" } == true
                ) {
                    ImageViewScreen(
                        modifier = Modifier.fillMaxSize(),
                        imageFile = selectedFileState.selectedFile,
                    )
                } else {
                    TextEditorScreen(
                        viewModel = textEditorViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

