package com.evandhardspace.loon.scenes.filechooser

internal data class FileChooserState(
    val error: String? = null,
)

internal sealed interface FileChooserAction {
    data class ChoseFile(val selectedDirectoryPath: Result<String>): FileChooserAction
    data object DismissError: FileChooserAction
}

internal sealed interface FileChooserEffect {
    data class FileSelected(val selectedFilePath: String) : FileChooserEffect
}