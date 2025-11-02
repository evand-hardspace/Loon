package com.evandhardspace.loon.editor.filetree

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.window.Dialog
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
    var showNameDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    // Recalculate children when refreshTrigger changes
    val children by remember(refreshTrigger, file.absolutePath) {
        mutableStateOf(file.listFiles()?.sortedBy { it.name } ?: emptyList())
    }

    // Check if file exists
    val targetDir = if (file.isDirectory) file else file.parentFile
    val fileExists = targetDir?.let { File(it, newFileName).exists() } ?: false
    val fileNameIsBlank = newFileName.isBlank()

    // Name input dialog
    if (showNameDialog) {
        Dialog(onDismissRequest = { showNameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "New File Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = {
                            Text(
                                text = "File name",
                            )
                        },
                        isError = fileExists,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (fileExists) {
                        Text(
                            text = "File already exists",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                showNameDialog = false
                                newFileName = ""
                            },
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank() && !fileExists) {
                                    createFileRelativeTo(file, newFileName)
                                    onCreateFile()
                                    showNameDialog = false
                                    newFileName = ""
                                }
                            },
                            enabled = !fileNameIsBlank && !fileExists,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("New File") {
                    newFileName = "untitled.txt"
                    showNameDialog = true
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