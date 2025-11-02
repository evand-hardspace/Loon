package com.evandhardspace.loon.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun onFileSelected(file: File) {
        if (file in state.value.tabs) {
            _state.update {
                it.copy(
                    selectedFile = file
                )
            }
        } else {
            _state.update {
                it.copy(
                    tabs = it.tabs + file,
                    selectedFile = file,
                )
            }
        }
    }

    fun onTabClosed(file: File) {
        if (file !in state.value.tabs) return
        _state.update {
            val newTabs = it.tabs - file
            it.copy(
                tabs = newTabs,
                selectedFile = if (newTabs.contains(it.selectedFile)) it.selectedFile else newTabs.lastOrNull()
            )
        }
    }
}

@Immutable
data class EditorState(
    val tabs: List<File> = emptyList(),
    val selectedFile: File? = null,
)