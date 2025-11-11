package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SelectedFileState : State {
    val selectedFileOrDirectory: File?
    val selectedFile: File?
    val selectedFileAsFlow: Flow<File?>

    fun selectFile(file: File?)
}

internal class DefaultSelectedFileState : SelectedFileState, ViewModel() {
    override var selectedFileOrDirectory: File? by mutableStateOf(null)
        private set

    override var selectedFile: File? by mutableStateOf(null)
        private set
    override val selectedFileAsFlow: Flow<File?> = snapshotFlow { selectedFile }

    override fun selectFile(file: File?) {
        if(file == null) {
            withMutableSnapshot {
                selectedFileOrDirectory = null
                selectedFile = null
            }
            return
        }
        withMutableSnapshot {
            selectedFileOrDirectory = file
            if (file.isDirectory.not()) {
                selectedFile = file
            }
        }
    }

}