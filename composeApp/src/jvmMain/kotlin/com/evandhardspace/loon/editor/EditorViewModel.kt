package com.evandhardspace.loon.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun onFileSelected(fileName: String) {
        if(fileName in state.value.tabs) {
            _state.update {
                it.copy(
                    selectedFile = fileName
                )
            }
        } else {
            _state.update {
                it.copy(
                    tabs = it.tabs + fileName,
                    selectedFile = fileName,
                )
            }
        }
    }

    fun onFileClosed(fileName: String) {
        if(fileName !in state.value.tabs) return
        _state.update {
            val newTabs = it.tabs - fileName
            it.copy(
                tabs = newTabs,
                selectedFile = if (newTabs.contains(it.selectedFile)) it.selectedFile else newTabs.lastOrNull()
            )
        }
    }
}

@Immutable
data class EditorState(
    val tabs: List<String> = emptyList(),
    val selectedFile: String? = null,
)