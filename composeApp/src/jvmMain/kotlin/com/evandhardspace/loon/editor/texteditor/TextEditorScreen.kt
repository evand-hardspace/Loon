package com.evandhardspace.loon.editor.texteditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    if (selectedPath == null || File(selectedPath).let { file ->
            file.isFile.not() || file.extension != "txt"
        }) {
        Text("Selected item is not a file: $selectedPath")
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with title and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Text Editor",
                style = MaterialTheme.typography.h5
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = save) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save file changes",
                        tint = if (isDirty) Color.Red else Color.Green
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text editor area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = 4.dp
        ) {
            BasicTextField(
                value = textState,
                onValueChange = updateText,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            text = "Start typing...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            backgroundColor = Color(0xFFF5F5F5)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Characters", getCharCount())
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                StatItem("Words", getWordCount())
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                StatItem("Lines", getLineCount())
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color.Gray
        )
    }
}
