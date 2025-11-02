package com.evandhardspace.loon.editor.filetree

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import kotlin.random.Random

@Composable
fun FileTree(
    root: File,
    modifier: Modifier = Modifier,
    onFileClick: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
) {
    var refreshTrigger by remember { mutableStateOf(0) }

    // Watch for external changes
    LaunchedEffect(root) {
        val watchService = FileSystems.getDefault().newWatchService()
        root.toPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        withContext(Dispatchers.IO) {
            while (true) {
                val key = watchService.take()
                key.pollEvents()
                key.reset()
                refreshTrigger++
            }
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            FileNode(
                file = root,
                refreshTrigger = refreshTrigger,
                onFileClick = onFileClick,
                onCreateFile = { refreshTrigger++ },
                onDeleteFile = {
                    onDeleteFile(it)
                    refreshTrigger++
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileNode(
    file: File,
    refreshTrigger: Int,
    level: Int = 0,
    onFileClick: (File) -> Unit,
    onCreateFile: () -> Unit,
    onDeleteFile: (File) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    // Recalculate children when refreshTrigger changes
    val children by remember(refreshTrigger, file.absolutePath) {
        mutableStateOf(file.listFiles()?.sortedBy { it.name } ?: emptyList())
    }

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("New File") {
                    createFileRelativeTo(file, "${Random.nextInt()}.txt")
                    onCreateFile()
                },
                ContextMenuItem("Delete") {
                    deleteFileOrDirectory(file)
                    onDeleteFile(file)
                },
            )
        }
    ) {
        Row(
            modifier = Modifier
                .padding(start = (level * 16).dp)
                .fillMaxWidth()
                .clickable {
                    if (file.isDirectory) {
                        expanded = !expanded
                    } else {
                        onFileClick(file)
                    }
                }
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .background(if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
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
                color = if (isHovered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }

    if (expanded && file.isDirectory) {
        Column {
            for (child in children) {
                FileNode(
                    file = child,
                    refreshTrigger = refreshTrigger,
                    level = level + 1,
                    onFileClick = onFileClick,
                    onCreateFile = onCreateFile,
                    onDeleteFile = onDeleteFile,
                )
            }
        }
    }
}

fun createFileRelativeTo(selected: File, newFileName: String): File? {
    val targetDir = if (selected.isDirectory) {
        selected
    } else {
        selected.parentFile ?: error("Selected file has no parent")
    }

    val newFile = File(targetDir, newFileName)
    if (!newFile.exists()) {
        newFile.createNewFile()
        println("Created: ${newFile.absolutePath}")
    } else {
        println("File already exists: ${newFile.absolutePath}")
    }
    return newFile
}

fun deleteFileOrDirectory(target: File): Boolean {
    return if (target.exists()) {
        if (target.isDirectory) {
            target.deleteRecursively()
        } else {
            target.delete()
        }
    } else {
        false
    }
}