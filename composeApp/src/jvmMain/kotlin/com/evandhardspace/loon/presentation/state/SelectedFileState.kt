package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import java.io.File

interface SelectedFileState : State {
    val selectedFileOrDirectory: File?
    val selectedFile: File?

    fun selectFile(file: File?)
}

internal class DefaultSelectedFileState : SelectedFileState {
    override var selectedFileOrDirectory: File? by mutableStateOf(null)
        private set

    override var selectedFile: File? by mutableStateOf(null)
        private set

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