package com.evandhardspace.loon.features.tab

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.getState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class TabViewModel(
    private val selectedFileState: SelectedFileState = getState(),
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun addTab(file: File) {
        selectedFileState.selectFile(file)
        if (file in state.value.tabs) return
        _state.update {
            val newTabs = it.tabs + file
            it.copy(
                tabs = newTabs,
            )
        }
    }

    fun onTabClosed(file: File) {
        if (file !in state.value.tabs) return
        val selectedFile = selectedFileState.selectedFile
        val newTabs = _state.value.tabs - file
        selectedFileState.selectFile(
            if (newTabs.contains(selectedFile)) selectedFile else newTabs.lastOrNull()
        )
        _state.update {
            val newTabs = it.tabs - file
            it.copy(
                tabs = newTabs,
            )
        }
    }
}

@Immutable
data class EditorState(
    val tabs: List<File> = emptyList(),
)