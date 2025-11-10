package com.evandhardspace.loon.scene.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.features.filetree.FileTree
import com.evandhardspace.loon.features.imagearea.ImageViewScreen
import com.evandhardspace.loon.features.tab.TabPanel
import com.evandhardspace.loon.features.workarea.WorkAreaViewModel
import com.evandhardspace.loon.features.texteditorarea.TextEditorScreen
import com.evandhardspace.loon.coreutils.ui.SplitPane
import com.evandhardspace.loon.features.imagearea.ImageAreaHolder
import com.evandhardspace.loon.features.tab.TabSelector
import com.evandhardspace.loon.features.tab.TabViewModel
import com.evandhardspace.loon.features.texteditorarea.TextEditorHolder
import com.evandhardspace.loon.features.workarea.UnsupportedAreaHolder
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
import com.evandhardspace.loon.presentation.state.SelectedFileSlice
import com.evandhardspace.loon.presentation.state.clearSlices
import com.evandhardspace.loon.presentation.state.getSlice
import java.io.File

@Composable
fun WorkSpaceScene(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tabViewModel: TabViewModel = viewModel { TabViewModel() }, // TODO get rid on this level
    workAreaViewModel: WorkAreaViewModel = viewModel { WorkAreaViewModel() },
) {
    DisposableEffect(Unit) {
        onDispose {
            clearSlices() // TODO: manage lifecycle
        }
    }
    val selectedFileSlice: SelectedFileSlice = remember { getSlice() }

    SplitPane(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
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
                        selectedFileSlice.selectFile(it)
                        if (it.isDirectory.not()) {
                            tabViewModel.addTab(it)
                        }
                    },
                    onDeleteFile = {
                        tabViewModel.onTabClosed(it)
                    },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        },
        rightContent = {
            Column {
                TabPanel(
                    tabs = tabViewModel.tabs,
                    selectedTab = tabViewModel.selectedTab,
                    onTabClosed = { file ->
                        tabViewModel.onTabClosed(file)
                    },
                    onTabClick = { file ->
                        tabViewModel.addTab(file)
                        selectedFileSlice.selectFile(file)
                    },
                )
                when (val holder = workAreaViewModel.currentHolder) {
                    is ImageAreaHolder -> {
                        ImageViewScreen(
                            modifier = Modifier.fillMaxSize(),
                            imageFile = holder.selectedFile,
                        )
                    }

                    is TextEditorHolder -> {
                        TextEditorScreen(
                            holder = holder,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is UnsupportedAreaHolder -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\"${holder.unsupportedExtension}\" file type is not supported.",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "File is not selected",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    else -> error("Not supported area holder")
                }
            }
        }
    )

    TabSelector(
        modifier = Modifier.fillMaxSize(),
        tabs = tabViewModel.tabs,
        selectedTabFile = tabViewModel.selectedTab,
        onClose = selectedFileSlice::selectFile,
    )
}

