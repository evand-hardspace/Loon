package com.evandhardspace.loon.scene.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evandhardspace.loon.coreutils.ui.SplitPane
import com.evandhardspace.loon.features.filetree.FileTree
import com.evandhardspace.loon.features.filetree.FileTreeViewModel
import com.evandhardspace.loon.features.imagearea.ImageAreaHolder
import com.evandhardspace.loon.features.imagearea.ImageViewScreen
import com.evandhardspace.loon.features.tab.TabPanel
import com.evandhardspace.loon.features.tab.TabSelector
import com.evandhardspace.loon.features.tab.TabViewModel
import com.evandhardspace.loon.features.terminal.Terminal
import com.evandhardspace.loon.features.texteditorarea.KotlinTextEditorHolder
import com.evandhardspace.loon.features.texteditorarea.KotlinTextEditorScreen
import com.evandhardspace.loon.features.texteditorarea.TextEditorHolder
import com.evandhardspace.loon.features.texteditorarea.TextEditorScreen
import com.evandhardspace.loon.features.vcs.GitRepository
import com.evandhardspace.loon.features.vcs.GitWatcher
import com.evandhardspace.loon.features.workarea.UnsupportedAreaHolder
import com.evandhardspace.loon.features.workarea.WorkAreaViewModel
import com.evandhardspace.loon.presentation.state.ProvideStates
import com.evandhardspace.loon.presentation.state.get
import com.evandhardspace.loon.presentation.state.viewModelWithState
import java.io.File

@Composable
fun WorkSpaceScene(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    ProvideStates {
        val fileTreeViewModel = viewModelWithState {
            val gitRepo = GitRepository(selectedPath)
            val gitWatcher = GitWatcher(gitRepo, selectedPath)
            FileTreeViewModel(
                selectedFileState = get(),
                dirtyFilesState = get(),
                fileTreeState = get(),
                gitRepository = gitRepo,
                gitWatcher = gitWatcher,
            )
        }

        val tabViewModel: TabViewModel = viewModelWithState {
            TabViewModel(
                selectedFileState = get(),
                tabsState = get(),
                fileTreeState = get(),
                dirtyFilesState = get(),
            )
        }
        val workAreaViewModel: WorkAreaViewModel = viewModelWithState {
            WorkAreaViewModel(
                selectedFileState = get(),
                dirtyFileState = get(),
                tabsState = get(),
            )
        }

        SplitPane(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background),
            leftContent = {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    var isOnBackEnabled by remember { mutableStateOf(true) }
                    IconButton(
                        onClick = {
                            isOnBackEnabled = false
                            onBack()
                        },
                        enabled = isOnBackEnabled,
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
                        fileTreeViewModel = fileTreeViewModel,
                        tabViewModel = tabViewModel,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            },
            rightContent = {
                Column {
                    TabPanel(
                        tabs = tabViewModel.tabs,
                        selectedTab = tabViewModel.selectedTab,
                        root = selectedPath,
                        gitStatus = fileTreeViewModel.gitStatus.collectAsStateWithLifecycle().value,
                        onTabClosed = { file ->
                            tabViewModel.onTabClosed(file)
                        },
                        isDirty = tabViewModel::isTabDirty,
                        onTabClick = { file ->
                            tabViewModel.addTab(file)
                            fileTreeViewModel.selectFile(file)
                        },
                    )
                    when (val holder = workAreaViewModel.currentHolder) {
                        is ImageAreaHolder -> {
                            ImageViewScreen(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                imageFile = holder.selectedFile,
                            )
                        }

                        is TextEditorHolder -> {
                            TextEditorScreen(
                                holder = holder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }

                        is KotlinTextEditorHolder -> {
                            KotlinTextEditorScreen(
                                holder = holder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }

                        is UnsupportedAreaHolder -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "\"${holder.unsupportedExtension}\" file type is not supported.",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "File is not selected",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        else -> error("Not supported area holder")
                    }

                    var isExpanded by remember { mutableStateOf(false) }
                    Terminal(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if(isExpanded) {
                                    Modifier.height(200.dp)
                                } else Modifier.height(20.dp)
                            ),
                        startDirectory = selectedPath,
                        onToggleVisibility = { isExpanded = it }
                    )
                }
            }
        )

        TabSelector(
            modifier = Modifier.fillMaxSize(),
            tabs = tabViewModel.tabs,
            selectedTabFile = tabViewModel.selectedTab,
            onClose = fileTreeViewModel::selectFile,
        )
    }
}

