package com.evandhardspace.loon.editor.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evandhardspace.loon.editor.EditorState
import java.io.File

@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    state: EditorState,
    onFileSelected: (File) -> Unit,
    onFileClosed: (File) -> Unit,
) {
    if (state.tabs.isEmpty()) {
        Spacer(Modifier.height(20.dp))
        return
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        state.tabs.forEach { file ->
            val selected = file == state.selectedFile
            Column(
                modifier = Modifier
                    .clickable(onClick = { onFileSelected(file) })
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = file.name,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close $file",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                onFileClosed(file)
                            }
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}