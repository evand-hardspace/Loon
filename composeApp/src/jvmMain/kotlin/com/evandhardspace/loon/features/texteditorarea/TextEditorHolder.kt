package com.evandhardspace.loon.features.texteditorarea

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.evandhardspace.loon.features.workarea.WorkAreaHolder
import com.evandhardspace.loon.presentation.state.DirtyFilesSlice
import com.evandhardspace.loon.presentation.state.getSlice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class TextEditorHolder(
    val selectedFile: File,
    private val dirtyFileSlice: DirtyFilesSlice = getSlice(),
) : WorkAreaHolder() {

    init {
        trackRealFile()
    }

    private fun trackRealFile() {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1_000)
                if ((dirtyFileSlice.dirtyStates.find { it.file == selectedFile }?.isDirty ?: false).not()) {
                    textState = textState.copy(text = selectedFile.readText())
                }
            }
        }
    }

    var textState: TextFieldValue by mutableStateOf(
        TextFieldValue(
            selectedFile.readText()
        )
    )
        private set

    val isDirty: Boolean
        get() = dirtyFileSlice.dirtyStates
            .find { it.file == selectedFile }
            ?.isDirty
            ?: false

    private var initialSnapshot: String = textState.text

    override fun init() {
        dirtyFileSlice.add(selectedFile)
    }

    override fun dispose() {
        super.dispose()
        dirtyFileSlice.remove(selectedFile.absolutePath)
    }

    fun updateText(newText: TextFieldValue) {
        if (newText.text != textState.text) {
            updateIsDirty(true)
        }
        if (newText.text == initialSnapshot) {
            updateIsDirty(false)
        }
        textState = newText
    }

    fun clearText() {
        updateText(TextFieldValue(""))
    }

    fun getCharCount(): Int = textState.text.length

    fun getWordCount(): Int = textState.text
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .size

    fun getLineCount(): Int = textState.text.lines().size

    fun save() {
        initialSnapshot = textState.text
        try {
            selectedFile.writeText(textState.text)
            updateIsDirty(false)
        } catch (t: Throwable) {
            println("File saving went wrong: $t")
        }
    }

    private fun updateIsDirty(isDirty: Boolean) {
        dirtyFileSlice.updateIsDirty(selectedFile.absolutePath, isDirty)
    }
}