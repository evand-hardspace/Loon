package com.evandhardspace.loon.features.texteditorarea

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.evandhardspace.loon.features.workarea.WorkAreaHolder
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class KotlinTextEditorHolder(
    val selectedFile: File,
    private val dirtyFileState: DirtyFilesState,
) : WorkAreaHolder() {

    init {
        trackRealFile()
    }

    private fun trackRealFile() {
        coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1_000)
                if ((dirtyFileState.dirtyFiles.find { it.file == selectedFile }?.isDirty ?: false).not()) {
                    val newText = selectedFile.readText()
                    textState = textState.copy(
                        annotatedString = highlightKotlinSyntax(newText),
                        selection = textState.selection
                    )
                }
            }
        }
    }

    var textState: TextFieldValue by mutableStateOf(
        TextFieldValue(
            annotatedString = highlightKotlinSyntax(selectedFile.readText())
        )
    )
        private set

    val isDirty: Boolean
        get() = dirtyFileState.dirtyFiles
            .find { it.file == selectedFile }
            ?.isDirty
            ?: false

    private var initialSnapshot: String = textState.text

    override fun init() {
        dirtyFileState.add(selectedFile)
    }

    override fun dispose() {
        super.dispose()
        dirtyFileState.remove(selectedFile.absolutePath)
    }

    fun updateText(newText: TextFieldValue) {
        val oldText = textState.text
        val oldSelection = textState.selection

        // Check if we're adding a character
        if (newText.text.length > oldText.length) {
            val diff = newText.text.length - oldText.length
            if (diff == 1) {
                val insertPos = newText.selection.start - 1
                if (insertPos >= 0 && insertPos < newText.text.length) {
                    val insertedChar = newText.text[insertPos]

                    // Handle Enter key with auto-indentation
                    if (insertedChar == '\n') {
                        // Find the start of the previous line
                        val lineStart = oldText.lastIndexOf('\n', insertPos - 1) + 1
                        val previousLine = oldText.substring(lineStart, insertPos)

                        // Calculate current indentation
                        val currentIndent = previousLine.takeWhile { it == ' ' || it == '\t' }

                        // Check if previous line ends with opening bracket
                        val trimmedPrevLine = previousLine.trimEnd()
                        val needsExtraIndent = trimmedPrevLine.endsWith('{') || trimmedPrevLine.endsWith('(')

                        // Check if cursor is between brackets
                        val nextCharIsClosing = insertPos < oldText.length &&
                                (oldText[insertPos] == '}' || oldText[insertPos] == ')')

                        val indentToAdd = if (needsExtraIndent) {
                            currentIndent + "    " // Add 4 spaces for new indentation level
                        } else {
                            currentIndent // Keep same indentation
                        }

                        val textWithIndent = if (needsExtraIndent && nextCharIsClosing) {
                            // Between brackets: add indented line and closing bracket line
                            newText.text.substring(0, insertPos + 1) +
                                    indentToAdd +
                                    "\n" + currentIndent +
                                    newText.text.substring(insertPos + 1)
                        } else {
                            // Normal case: just add indentation
                            newText.text.substring(0, insertPos + 1) +
                                    indentToAdd +
                                    newText.text.substring(insertPos + 1)
                        }

                        val cursorPos = insertPos + 1 + indentToAdd.length
                        val highlighted = highlightKotlinSyntax(textWithIndent)
                        textState = TextFieldValue(
                            annotatedString = highlighted,
                            selection = androidx.compose.ui.text.TextRange(cursorPos)
                        )
                        if (textWithIndent != initialSnapshot) updateIsDirty(true)
                        if (textWithIndent == initialSnapshot) updateIsDirty(false)
                        return
                    }

                    // Auto-close brackets
                    when (insertedChar) {
                        '(' -> {
                            val textWithBracket = newText.text.substring(0, insertPos + 1) + ')' +
                                    newText.text.substring(insertPos + 1)
                            val highlighted = highlightKotlinSyntax(textWithBracket)
                            textState = TextFieldValue(
                                annotatedString = highlighted,
                                selection = androidx.compose.ui.text.TextRange(insertPos + 1)
                            )
                            if (textWithBracket != initialSnapshot) updateIsDirty(true)
                            if (textWithBracket == initialSnapshot) updateIsDirty(false)
                            return
                        }
                        '{' -> {
                            val textWithBracket = newText.text.substring(0, insertPos + 1) + '}' +
                                    newText.text.substring(insertPos + 1)
                            val highlighted = highlightKotlinSyntax(textWithBracket)
                            textState = TextFieldValue(
                                annotatedString = highlighted,
                                selection = androidx.compose.ui.text.TextRange(insertPos + 1)
                            )
                            if (textWithBracket != initialSnapshot) updateIsDirty(true)
                            if (textWithBracket == initialSnapshot) updateIsDirty(false)
                            return
                        }
                    }
                }
            }
        }
        // Check if we're deleting a character
        else if (newText.text.length < oldText.length) {
            val diff = oldText.length - newText.text.length
            if (diff == 1) {
                val deletePos = newText.selection.start
                if (deletePos >= 0 && deletePos < oldText.length) {
                    val deletedChar = oldText[deletePos]

                    // Check if we should auto-delete closing bracket
                    if ((deletedChar == '(' && deletePos + 1 < oldText.length && oldText[deletePos + 1] == ')') ||
                        (deletedChar == '{' && deletePos + 1 < oldText.length && oldText[deletePos + 1] == '}')) {

                        // Check if this closing bracket was auto-added (not originally in initialSnapshot)
                        val textWithoutBoth = newText.text.substring(0, deletePos) +
                                newText.text.substring(deletePos + 1)
                        val highlighted = highlightKotlinSyntax(textWithoutBoth)
                        textState = TextFieldValue(
                            annotatedString = highlighted,
                            selection = androidx.compose.ui.text.TextRange(deletePos)
                        )
                        if (textWithoutBoth != initialSnapshot) updateIsDirty(true)
                        if (textWithoutBoth == initialSnapshot) updateIsDirty(false)
                        return
                    }
                }
            }
        }

        if (newText.text != textState.text) {
            updateIsDirty(true)
        }
        if (newText.text == initialSnapshot) {
            updateIsDirty(false)
        }

        // Apply syntax highlighting
        textState = newText.copy(
            annotatedString = highlightKotlinSyntax(newText.text)
        )
    }

    fun clearText() {
        updateText(TextFieldValue(annotatedString = AnnotatedString("")))
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
        dirtyFileState.updateIsDirty(selectedFile.absolutePath, isDirty)
    }

    private fun highlightKotlinSyntax(text: String): AnnotatedString {
        val keywords = setOf(
            "package", "import", "class", "interface", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "return", "break",
            "continue", "object", "companion", "data", "sealed", "enum",
            "abstract", "open", "override", "final", "private", "protected",
            "public", "internal", "in", "out", "by", "get", "set",
            "try", "catch", "finally", "throw", "throws", "is", "as",
            "this", "super", "null", "true", "false", "const", "lateinit",
            "suspend", "inline", "noinline", "crossinline", "reified",
            "operator", "infix", "tailrec", "external", "annotation",
            "vararg", "expect", "actual", "init", "constructor"
        )

        val keywordColor = Color(0xFFCC7832) // Orange
        val stringColor = Color(0xFF6A8759) // Green
        val commentColor = Color(0xFF808080) // Gray
        val annotationColor = Color(0xFFC6B724) // Gray
        val numberColor = Color(0xFF6897BB) // Blue

        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    // Multi-line comment
                    text.startsWith("/*", i) -> {
                        val end = text.indexOf("*/", i + 2)
                        val commentEnd = if (end != -1) end + 2 else text.length
                        withStyle(SpanStyle(color = commentColor)) {
                            append(text.substring(i, commentEnd))
                        }
                        i = commentEnd
                    }
                    // Single-line comment
                    text.startsWith("//", i) -> {
                        val end = text.indexOf('\n', i).let { if (it == -1) text.length else it }
                        withStyle(SpanStyle(color = commentColor)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    // Single-line comment
                    text.startsWith("@", i) -> {
                        val end = text.substring(i).indexOfFirst { it == ' ' || it == '\n' }.let { if (it == -1) text.length else it }
                        withStyle(SpanStyle(color = annotationColor)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    // String literal
                    text[i] == '"' -> {
                        val start = i
                        i++
                        while (i < text.length && text[i] != '"') {
                            if (text[i] == '\\' && i + 1 < text.length) i++
                            i++
                        }
                        if (i < text.length) i++
                        withStyle(SpanStyle(color = stringColor)) {
                            append(text.substring(start, i))
                        }
                    }
                    // Character literal
                    text[i] == '\'' -> {
                        val start = i
                        i++
                        while (i < text.length && text[i] != '\'') {
                            if (text[i] == '\\' && i + 1 < text.length) i++
                            i++
                        }
                        if (i < text.length) i++
                        withStyle(SpanStyle(color = stringColor)) {
                            append(text.substring(start, i))
                        }
                    }
                    // Number
                    text[i].isDigit() -> {
                        val start = i
                        while (i < text.length && (text[i].isDigit() || text[i] in ".xXfFdDlL")) i++
                        withStyle(SpanStyle(color = numberColor)) {
                            append(text.substring(start, i))
                        }
                    }
                    // Identifier or keyword
                    text[i].isLetter() || text[i] == '_' -> {
                        val start = i
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                        val word = text.substring(start, i)
                        if (word in keywords) {
                            withStyle(SpanStyle(color = keywordColor)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}