package com.evandhardspace.loon.scenes.editor.texteditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.get

class TextEditorGlobalViewModel() : ViewModel() {
    val holders: SnapshotStateMap<String, TextEditorHolder> = mutableStateMapOf<String, TextEditorHolder>()

    val textState: TextFieldValue
        get() = holders[selected]?.textState ?: TextFieldValue()
    val isDirty: Boolean
        get() = holders[selected]?.isDirty ?: false
//    val dirtyFiles: Flow<Map<String, Boolean>> =  snapshotFlow { holders }
//        .map { it.map { (key, value) -> key to value.isDirty }.toMap() }
////        holders.map { (key, value) -> key to value.isDirty}.toMap()

    var selected: String? by mutableStateOf(null)
        private set

    fun changeSelected(newSelected: String?) {
        println("changed to :$newSelected")
        selected = newSelected
    }

    fun addHolder(filePath: String) {
        if (holders[filePath] != null) return
        holders[filePath] = TextEditorHolder(filePath)
    }

    fun removeHolder(filePath: String) {
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
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    init {
        coroutineScope.launch {
            while (true) {
                delay(1_000)
                if (isDirty.not()) {
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

    var isDirty: Boolean by mutableStateOf(false)

    fun updateText(newText: TextFieldValue) {
        if (newText.text != textState.text) {
            isDirty = true
        }
        if(newText.text == initialSnapshot) {
            isDirty = false
        }
        textState = newText
    }

    fun clearText() {
        isDirty = true
        textState = TextFieldValue("")
    }

    fun getCharCount(): Int = textState.text.length

    fun getWordCount(): Int = textState.text
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .size

    fun getLineCount(): Int = textState.text.lines().size

    fun save() {
        try {
            File(selectedPath).writeText(textState.text)
            isDirty = false
        } catch (t: Throwable) {
            println("File saving went wrong: $t")
        }
    }

    fun onClear() {
        coroutineScope.cancel()
    }
}