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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evandhardspace.loon.editor.EditorState

@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    state: EditorState,
    onFileSelected: (String) -> Unit,
    onFileClosed: (String) -> Unit,
) {
    if (state.tabs.isEmpty()) {
        Spacer(Modifier.height(20.dp))
        return
    }
    Column(modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            state.tabs.forEach { fileName ->
                val selected = fileName == state.selectedFile
                Tab(
                    selected = fileName == state.selectedFile,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    onClick = { onFileSelected(fileName) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = fileName,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close $fileName",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    onFileClosed(fileName)
                                }
                        )
                    }
                }
            }
        }
    }
}