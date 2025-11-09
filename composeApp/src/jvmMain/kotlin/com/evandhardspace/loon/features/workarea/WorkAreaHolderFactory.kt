package com.evandhardspace.loon.features.workarea

import com.evandhardspace.loon.features.imagearea.ImageAreaHolder
import com.evandhardspace.loon.features.texteditorarea.TextEditorHolder
import java.io.File

class WorkAreaHolderFactory(
    private val textEditorHolderFactory: (File) -> TextEditorHolder = { file ->
        TextEditorHolder(
            selectedFile = file,
        )
    },
    private val imageHolderFactory: (File) -> ImageAreaHolder = { file ->
        ImageAreaHolder(file)
    },
) {
    fun create(file: File): WorkAreaHolder {
        if (file.extension.lowercase() == "png" || file.extension.lowercase() == "jpg") return imageHolderFactory(file)
        if (file.extension == "txt") return textEditorHolderFactory(file)
        return UnsupportedAreaHolder(file.extension)
    }
}
