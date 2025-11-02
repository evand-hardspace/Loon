package com.evandhardspace.loon.editor.filetree

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun FileTree(
    root: File,
    modifier: Modifier = Modifier,
    onFileClick: (File) -> Unit = {},
) {
    LazyColumn(modifier = modifier) {
        item {
            FileNode(file = root, onFileClick = onFileClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileNode(
    file: File,
    level: Int = 0,
    onFileClick: (File) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(start = (level * 16).dp)
            .clickable {
                if (file.isDirectory) {
                    expanded = !expanded
                } else {
                    onFileClick(file)
                }
            }
            .padding(vertical = 2.dp)
    ) {
        val icon = when {
            file.isDirectory && expanded -> Icons.Default.FolderOpen
            file.isDirectory -> Icons.Default.Folder
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp),
            tint = MaterialTheme.colorScheme.onBackground,
            )
        Text(
            text = file.name.ifEmpty { file.path },
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    if (expanded && file.isDirectory) {
        val children = remember(file) { file.listFiles()?.sortedBy { it.name } ?: emptyList() }
        Column {
            for (child in children) {
                FileNode(file = child, level = level + 1, onFileClick = onFileClick)
            }
        }
    }
}