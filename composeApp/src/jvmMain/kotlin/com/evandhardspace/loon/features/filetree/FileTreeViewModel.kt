package com.evandhardspace.loon.features.filetree

import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.FileState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import java.io.File

class FileTreeViewModel(
    private val selectedFileState: SelectedFileState,
    private val dirtyFilesState: DirtyFilesState,
    private val fileState: FileState,
) : ViewModel() {

    fun initialize(root: File) {
        fileState.initialize(root)
    }

    fun refreshRootNode() {
        fileState.rootNode?.let { fileState.refreshNode(it) }
    }

    fun selectFile(file: File) {
        selectedFileState.selectFile(file)
    }

    val selectedFileOrDirectory: File?
        get() = selectedFileState.selectedFileOrDirectory

    fun isFileDirty(file: String): Boolean =
        dirtyFilesState.dirtyStates.find { it.file.absolutePath == file }?.isDirty ?: false

}