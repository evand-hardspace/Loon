package com.evandhardspace.loon.scenes.filechooser

import com.evandhardspace.loon.coreutils.EffectViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class FileChooserViewModel : EffectViewModel<FileChooserEffect>() {

    private val _state = MutableStateFlow(FileChooserState())
    val state = _state.asStateFlow()

    fun onAction(
        action: FileChooserAction,
    ) {
        when (action) {
            is FileChooserAction.ChoseFile -> choseFile(action.selectedDirectoryPath)
            FileChooserAction.DismissError -> dismissError()
        }
    }

    private fun choseFile(selectedDirectoryPath: Result<String>) {
        selectedDirectoryPath
            .onSuccess { path ->
                FileChooserEffect.FileSelected(
                    selectedFilePath = path,
                ).sendAsync()
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        error = error.message,
                    )
                }
            }
    }

    private fun dismissError() {
        _state.update {
            it.copy(
                error = null,
            )
        }
    }
}
