package com.evandhardspace.loon.scenes.editor.texteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skiko.Cursor
import java.io.File

@Composable
fun TextEditorScreen(
    selectedPath: String?,
    isDirty: Boolean,
    modifier: Modifier = Modifier,
    save: () -> Unit,
    textState: TextFieldValue,
    getCharCount: () -> Int,
    getWordCount: () -> Int,
    getLineCount: () -> Int,
    updateText: (TextFieldValue) -> Unit,
) {
    when {
        selectedPath == null -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "File is not selected",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            return
        }

        File(selectedPath).let { file ->
            file.isFile.not() || file.extension != "txt"
        } -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Selected item is not a text or image file.",
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            return
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181818))
    ) {
        // Editor area with line numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val lineCount = getLineCount()
            Column(
                modifier = Modifier
                    .background(Color(0xFF181818))
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                repeat(lineCount) { index ->
                    Text(
                        text = "${index + 1}",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF4A4A4A),
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }
            }

            // Subtle divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2A2A2A))
            )

            // Text editor
            BasicTextField(
                value = textState,
                onValueChange = updateText,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF181818))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            (event.isCtrlPressed || event.isMetaPressed) && // Meta = Command on macOS
                            event.key == Key.S
                        ) {
                            save()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE6E6E6),
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    Box {
                        if (textState.text.isEmpty()) {
                            Text(
                                text = "Start typing...",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF4A4A4A)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Bottom status bar - more minimal
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF202020),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBarItem("Ln ${getLineCount()}")
                    StatusBarItem("${getCharCount()} chars")
                    StatusBarItem("${getWordCount()} words")
                }
                IconButton(
                    onClick = save,
                    enabled = isDirty,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save file changes",
                        tint = if (isDirty) Color(0xFF5C9FFF) else Color(0xFF6B6B6B),
                        modifier = Modifier
                            .size(18.dp)
                            .then(
                                if (isDirty) {
                                    Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBarItem(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8A8A8A),
            fontWeight = FontWeight.Normal
        )
    )
}