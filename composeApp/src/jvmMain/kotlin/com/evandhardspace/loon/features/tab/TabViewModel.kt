package com.evandhardspace.loon.features.tab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evandhardspace.loon.presentation.state.FileTreeState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.TabsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class TabViewModel(
    private val selectedFileState: SelectedFileState,
    private val tabsState: TabsState,
    fileTreeState: FileTreeState,
) : ViewModel() {

    init {
        fileTreeState.deletedFile
            .onEach { file ->
                onTabClosed(file)
            }
            .launchIn(viewModelScope)
    }

    val tabs
        get() = tabsState.tabs

    val selectedTab
        get() = selectedFileState.selectedFile

    fun addTab(file: File) {
        viewModelScope.launch {
            if (file.isDirectory) return@launch
            selectedFileState.selectFile(file)
            if (file in tabs) return@launch
            tabsState.addTab(file)
        }
    }

    fun onTabClosed(file: File) {
        viewModelScope.launch {
            if (file !in tabs) return@launch
            tabsState.removeTab(file)
            val selectedFile = selectedFileState.selectedFile
            selectedFileState.selectFile(
                if (selectedFile in tabs) selectedFile else tabs.lastOrNull()
            )
        }
    }
}