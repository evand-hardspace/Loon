package com.evandhardspace.loon.features.scene.editor
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class TabViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun onFileSelected(file: File) {
        _state.update { currentState ->
            // Always update treeSelectedFile for tree highlighting
            val newTreeSelected = file

            // Only update tabs and selectedFile if it's a file, not a directory
            if (file.isFile) {
                if (file in currentState.tabs) {
                    currentState.copy(
                        selectedFile = file,
                        treeSelectedFile = newTreeSelected,
                    )
                } else {
                    currentState.copy(
                        tabs = currentState.tabs + file,
                        selectedFile = file,
                        treeSelectedFile = newTreeSelected,
                    )
                }
            } else {
                // For directories, only update tree selection, keep current tab
                currentState.copy(
                    treeSelectedFile = newTreeSelected,
                )
            }
        }
    }

    fun onTabClosed(file: File) {
        if (file !in state.value.tabs) return
        _state.update {
            val newTabs = it.tabs - file
            val newDirtyFiles = it.dirtyFiles - file.absolutePath
            it.copy(
                tabs = newTabs,
                selectedFile = if (newTabs.contains(it.selectedFile)) it.selectedFile else newTabs.lastOrNull(),
                dirtyFiles = newDirtyFiles,
            )
        }
    }

    fun setFileDirty(filePath: String, isDirty: Boolean) {
        _state.update {
            val newDirtyFiles = if (isDirty) {
                it.dirtyFiles + filePath
            } else {
                it.dirtyFiles - filePath
            }
            it.copy(dirtyFiles = newDirtyFiles)
        }
    }
}

@Immutable
data class EditorState(
    val tabs: List<File> = emptyList(),
    val selectedFile: File? = null,
    val treeSelectedFile: File? = null, // Separate tracking for tree UI
    val dirtyFiles: Set<String> = emptySet(), // Track dirty file paths
)