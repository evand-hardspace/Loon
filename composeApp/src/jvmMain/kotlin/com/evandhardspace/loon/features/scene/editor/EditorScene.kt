package com.evandhardspace.loon.features.scene.editor

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import java.io.File

@Composable
fun EditorScene(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tabViewModel: TabViewModel = viewModel { TabViewModel() },
    textEditorViewModel: TextEditorGlobalViewModel = viewModel { TextEditorGlobalViewModel() },
) {
    val state by tabViewModel.state.collectAsStateWithLifecycle()

    val selectedFile = state.selectedFile
    LaunchedEffect(selectedFile) {
        textEditorViewModel.changeSelected(selectedFile?.path)
    }
    LaunchedEffect(textEditorViewModel.isDirty, selectedFile) {
        selectedFile?.let { file ->
            tabViewModel.setFileDirty(file.absolutePath, textEditorViewModel.isDirty)
        }
    }

    SplitPane(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                            event.isMetaPressed &&
                            event.key == Key.W -> {
                        state.selectedFile?.let {
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
                        tabViewModel.onFileSelected(it)
                        if (it.isFile) {
                            textEditorViewModel.addHolder(it.path)
                        }
                    },
                    onDeleteFile = {
                        tabViewModel.onTabClosed(it)
                        textEditorViewModel.removeHolder(it.path)
                    },
                    selectedFile = state.treeSelectedFile, // Use treeSelectedFile instead
                )
            }
        },
        rightContent = {
            Column {
                TabPanel(
                    state = state,
                    onFileClosed = { file ->
                        tabViewModel.onTabClosed(file)
                        textEditorViewModel.removeHolder(file.path)
                    },
                    onFileSelected = { file ->
                        tabViewModel.onFileSelected(file)
                        if (file.extension.lowercase() != "png" && file.extension.lowercase() != "jpg") {
                            textEditorViewModel.changeSelected(file.path)
                        }
                    },
                )
                if (state.selectedFile?.extension?.lowercase()?.let { it == "png" || it == "jpg" } == true) {
                    ImageViewScreen(
                        modifier = Modifier.fillMaxSize(),
                        imageFile = state.selectedFile,
                    )
                } else {
                    TextEditorScreen(
                        selectedPath = textEditorViewModel.selected,
                        isDirty = textEditorViewModel.isDirty,
                        save = {
                            textEditorViewModel.save()
                            state.selectedFile?.let { file ->
                                tabViewModel.setFileDirty(file.absolutePath, false)
                            }
                        },
                        textState = textEditorViewModel.textState,
                        getCharCount = textEditorViewModel::getCharCount,
                        getWordCount = textEditorViewModel::getWordCount,
                        getLineCount = textEditorViewModel::getLineCount,
                        updateText = {
                            textEditorViewModel.updateText(it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

