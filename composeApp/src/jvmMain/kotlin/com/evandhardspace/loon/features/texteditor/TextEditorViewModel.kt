package com.evandhardspace.loon.features.texteditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.DirtyFilesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.get

class TextEditorGlobalViewModel(
    private val dirtyFileState: DirtyFilesState = DirtyFilesState(),
) : ViewModel() {
    val holders: SnapshotStateMap<String, TextEditorHolder> = mutableStateMapOf()

    val textState: TextFieldValue
        get() = holders[selected]?.textState ?: TextFieldValue()
    val isDirty: Boolean
        get() = selected?.let { s ->
            dirtyFileState.state
                .find { it.file.absolutePath == s }
                ?.isDirty
                ?: false
        } ?: false

    var selected: String? by mutableStateOf(null)
        private set

    fun changeSelected(newSelected: String?) {
        selected = newSelected
    }

    fun addHolder(filePath: String) {
        if (holders[filePath] != null) return
        dirtyFileState.add(File(filePath))
        holders[filePath] = TextEditorHolder(
            selectedPath = filePath,
            updateIsDirty = { isDirty ->
                dirtyFileState.updateIsDirty(filePath, isDirty)
            },
            isDirty = { dirtyFileState.state.find { it.file.absolutePath == filePath }?.isDirty ?: false }
        )
    }

    fun removeHolder(filePath: String) {
        dirtyFileState.remove(filePath)
        val holder = holders.remove(filePath)
        holder?.onClear()
    }

    fun updateText(newText: TextFieldValue) {
        holders[selected]?.updateText(newText)
    }

    fun clearText() {
        holders[selected]?.clearText()
    }

    fun save() {
        holders[selected]?.save()
    }

    fun getCharCount(): Int = holders[selected]?.getCharCount() ?: 0

    fun getWordCount(): Int = holders[selected]?.getWordCount() ?: 0

    fun getLineCount(): Int = holders[selected]?.getLineCount() ?: 0
}

class TextEditorHolder(
    private val selectedPath: String,
    private val updateIsDirty: (Boolean) -> Unit,
    private val isDirty: () -> Boolean,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    init {
        coroutineScope.launch {
            while (true) {
                delay(1_000)
                if (isDirty().not()) {
                    textState = textState.copy(text = File(selectedPath).readText())
                }
            }
        }
    }

    var textState: TextFieldValue by mutableStateOf(
        TextFieldValue(
            File(selectedPath).readText()
        )
    )
        private set

    private var initialSnapshot: String = textState.text

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
            File(selectedPath).writeText(textState.text)
            updateIsDirty(false)
        } catch (t: Throwable) {
            println("File saving went wrong: $t")
        }
    }

    fun onClear() {
        coroutineScope.cancel()
    }
}