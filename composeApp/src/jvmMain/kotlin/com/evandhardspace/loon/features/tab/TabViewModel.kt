package com.evandhardspace.loon.features.tab

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.TabsState
import com.evandhardspace.loon.presentation.state.getState
import java.io.File

class TabViewModel(
    private val selectedFileState: SelectedFileState = getState(),
    private val tabsState: TabsState = getState(),
) : ViewModel() {
    val tabs
        get() = tabsState.tabs

    val selectedTab
        get() = selectedFileState.selectedFile

    fun addTab(file: File) {
        selectedFileState.selectFile(file)
        if (file in tabs) return
        tabsState.addTab(file)
    }

    fun onTabClosed(file: File) {
        if (file !in tabs) return
        val selectedFile = selectedFileState.selectedFile
        tabsState.removeTab(file)
        selectedFileState.selectFile(
            if (selectedFile in tabs) selectedFile else tabs.lastOrNull()
        )
    }
}