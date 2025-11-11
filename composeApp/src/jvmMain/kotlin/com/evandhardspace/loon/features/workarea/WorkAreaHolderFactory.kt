package com.evandhardspace.loon.features.workarea

import com.evandhardspace.loon.features.imagearea.ImageAreaHolder
import com.evandhardspace.loon.features.texteditorarea.KotlinTextEditorHolder
import com.evandhardspace.loon.features.texteditorarea.TextEditorHolder
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import java.io.File

class WorkAreaHolderFactory(
    private val dirtyFileState: DirtyFilesState,
    private val textEditorHolderFactory: (File) -> TextEditorHolder = { file ->
        TextEditorHolder(
            selectedFile = file,
            dirtyFileState = dirtyFileState,
        )
    },
    private val kotlinTextEditorHolderFactory: (File) -> KotlinTextEditorHolder = { file ->
        KotlinTextEditorHolder(
            selectedFile = file,
            dirtyFileState = dirtyFileState,
        )
    },
    private val imageHolderFactory: (File) -> ImageAreaHolder = { file ->
        ImageAreaHolder(file)
    },
) {
    fun create(file: File): WorkAreaHolder {
        if (file.extension.lowercase() == "png" || file.extension.lowercase() == "jpg") return imageHolderFactory(file)
        if (file.extension == "txt") return textEditorHolderFactory(file)
        if(file.extension == "kt" || file.extension == "kts") return kotlinTextEditorHolderFactory(file)
        return UnsupportedAreaHolder(file.extension)
    }
}
