package com.evandhardspace.loon.chooser

import com.evandhardspace.loon.utils.EffectViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.swing.JFileChooser

class FileChooserViewModel(
    val chooser: JFileChooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
    },
) : EffectViewModel<FileChooserEffect>() {

    private val _state = MutableStateFlow(FileChooserState())
    val state = _state.asStateFlow()

    fun choseFile() {
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            if (chooser.selectedFile.isDirectory.not()) {
                _state.update {
                    it.copy(
                        error = "Only directories are supported now",
                    )
                }
            } else {
                FileChooserEffect.FileSelected(
                    selectedFilePath = chooser.selectedFile.absolutePath,
                ).sendAsync()
            }
        }
    }

    fun dismissError() {
        _state.update {
            it.copy(
                error = null,
            )
        }
    }
}

data class FileChooserState(
    val error: String? = null,
)

sealed interface FileChooserEffect {
    data class FileSelected(val selectedFilePath: String) : FileChooserEffect
}