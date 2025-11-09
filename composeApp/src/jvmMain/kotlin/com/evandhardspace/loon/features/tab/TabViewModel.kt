package com.evandhardspace.loon.features.tab

import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.presentation.state.SelectedFileSlice
import com.evandhardspace.loon.presentation.state.TabsSlice
import com.evandhardspace.loon.presentation.state.getSlice
import java.io.File

class TabViewModel(
    private val selectedFileSlice: SelectedFileSlice = getSlice(),
    private val tabsState: TabsSlice = getSlice(),
) : ViewModel() {
    val tabs
        get() = tabsState.tabs

    val selectedTab
        get() = selectedFileSlice.selectedFile

    fun addTab(file: File) {
        selectedFileSlice.selectFile(file)
        if (file in tabs) return
        tabsState.addTab(file)
    }

    fun onTabClosed(file: File) {
        if (file !in tabs) return
        val selectedFile = selectedFileSlice.selectedFile
        tabsState.removeTab(file)
        selectedFileSlice.selectFile(
            if (selectedFile in tabs) selectedFile else tabs.lastOrNull()
        )
    }
}