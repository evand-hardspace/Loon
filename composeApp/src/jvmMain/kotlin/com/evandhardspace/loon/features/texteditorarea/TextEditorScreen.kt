package com.evandhardspace.loon.features.texteditorarea

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
import com.evandhardspace.loon.keyhandler.mute
import org.jetbrains.skiko.Cursor

@Composable
fun TextEditorScreen(
    holder: TextEditorHolder,
    modifier: Modifier = Modifier,
) {
    TextEditorContent(
        isDirty = holder.isDirty,
        save = holder::save,
        textState = holder.textState,
        getCharCount = holder::getCharCount,
        getWordCount = holder::getWordCount,
        getLineCount = holder::getLineCount,
        updateText = holder::updateText,
        modifier = modifier,
    )
}

@Composable
fun TextEditorContent(
    isDirty: Boolean,
    modifier: Modifier = Modifier,
    save: () -> Unit,
    textState: TextFieldValue,
    getCharCount: () -> Int,
    getWordCount: () -> Int,
    getLineCount: () -> Int,
    updateText: (TextFieldValue) -> Unit,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    handleKeyEvent<AppKeyEvent.Save>(
        name = "text_editor",
        muteKeyEvents = {
            mute<AppKeyEvent.Enter>()
        }
    ) {
        save()
        true
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
            Column(
                modifier = Modifier
                    .background(Color(0xFF181818))
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .verticalScroll(
                        verticalScrollState,
                        enabled = false
                    ), // Use same scroll state, disable direct scrolling
                horizontalAlignment = Alignment.End
            ) {
                repeat(getLineCount()) { index ->
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

            // Scrollable text editor container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF181818))
            ) {
                BasicTextField(
                    value = textState,
                    onValueChange = updateText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .padding(horizontal = 4.dp, vertical = 12.dp),
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

                // Vertical scrollbar
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(verticalScrollState),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        hoverColor = MaterialTheme.colorScheme.onBackground,
                        thickness = 6.dp,
                    ),
                )

                // Horizontal scrollbar
                HorizontalScrollbar(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    adapter = rememberScrollbarAdapter(horizontalScrollState),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        hoverColor = MaterialTheme.colorScheme.onBackground,
                        thickness = 6.dp,
                    ),
                )
            }
        }

        // Bottom status bar
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