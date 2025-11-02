package com.evandhardspace.loon.editor

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.editor.filetree.FileTree
import com.evandhardspace.loon.editor.filetree.ImageViewScreen
import com.evandhardspace.loon.editor.tab.TabPanel
import com.evandhardspace.loon.editor.texteditor.TextEditorGlobalViewModel
import com.evandhardspace.loon.editor.texteditor.TextEditorScreen
import com.evandhardspace.loon.ui.SplitPane
import java.io.File

@Composable
fun EditorScreen(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tabViewModel: TabViewModel = viewModel { TabViewModel() },
    textEditorViewModel: TextEditorGlobalViewModel = viewModel { TextEditorGlobalViewModel() },
) {
    val state by tabViewModel.state.collectAsStateWithLifecycle()

    val selectedFile = state.selectedFile?.path
    LaunchedEffect(selectedFile) {
        textEditorViewModel.changeSelected(selectedFile)
    }

    SplitPane(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
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
                        if (it.isFile) {
                            tabViewModel.onFileSelected(it)
                            textEditorViewModel.addHolder(it.path)
                        }
                    },
                    onDeleteFile = {
                        tabViewModel.onTabClosed(it)
                        textEditorViewModel.removeHolder(it.path)
                    },
                    selectedFile = textEditorViewModel.selected?.let { File(it) },
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
                        save = textEditorViewModel::save,
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

