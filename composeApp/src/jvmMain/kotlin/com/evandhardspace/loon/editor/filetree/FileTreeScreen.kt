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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds

enum class CreateType {
    FILE, FOLDER
}

@Composable
fun FileTree(
    root: File,
    modifier: Modifier = Modifier,
    onDeleteFile: (File) -> Unit,
    selectedFile: File?,
    onFileSelect: (File) -> Unit,
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf(CreateType.FILE) }
    var newFileName by remember { mutableStateOf("") }

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

    // Check if file exists
    val targetDir = selectedFile?.takeIf { it.isDirectory }
        ?: selectedFile?.parentFile
        ?: root
    val fileExists = File(targetDir, newFileName).exists()
    val fileNameIsBlank = newFileName.isBlank()

    // Name input dialog
    if (showNameDialog) {
        Dialog(onDismissRequest = {
            showNameDialog = false
            newFileName = ""
        }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (createType == CreateType.FILE) "New File Name" else "New Folder Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = {
                            Text(
                                text = if (createType == CreateType.FILE) "File name" else "Folder name",
                            )
                        },
                        isError = fileExists,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (fileExists) {
                        Text(
                            text = if (createType == CreateType.FILE) "File already exists" else "Folder already exists",
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
                        TextButton(onClick = {
                            showNameDialog = false
                            newFileName = ""
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank() && !fileExists) {
                                    if (createType == CreateType.FILE) {
                                        createFileRelativeTo(selectedFile ?: root, newFileName)
                                    } else {
                                        createFolderRelativeTo(selectedFile ?: root, newFileName)
                                    }
                                    refreshTrigger++
                                    showNameDialog = false
                                    newFileName = ""
                                }
                            },
                            enabled = !fileNameIsBlank && !fileExists
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedFile != null) {
        Dialog(onDismissRequest = {
            showDeleteDialog = false
        }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Delete ${if (selectedFile.isDirectory) "Folder" else "File"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Are you sure you want to delete \"${selectedFile.name}\"?${if (selectedFile.isDirectory) " This will delete all contents." else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            showDeleteDialog = false
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                deleteFileOrDirectory(root,selectedFile)
                                onDeleteFile(selectedFile)
                                refreshTrigger++
                                showDeleteDialog = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                            event.isMetaPressed &&
                            event.key == Key.Backspace &&
                            selectedFile != null -> {
                        showDeleteDialog = true
                        true
                    }
                    // Cmd+N for new file
                    event.type == KeyEventType.KeyDown &&
                            event.isMetaPressed &&
                            event.key == Key.N  ->{
                        createType = CreateType.FILE
                        newFileName = "untitled.txt"
                        showNameDialog = true
                        true
                    }
                    // Cmd+Shift+N for new folder
                    event.type == KeyEventType.KeyDown &&
                            event.isMetaPressed &&
                            event.isShiftPressed &&
                            event.key == Key.N  ->{
                        createType = CreateType.FOLDER
                        newFileName = "untitled"
                        showNameDialog = true
                        true
                    }

                    else -> false
                }
            }
    ) {
        item {
            FileNode(
                file = root,
                root = root,
                refreshTrigger = refreshTrigger,
                selectedFile = selectedFile,
                onFileSelect = onFileSelect,
                onCreateFile = {
                    createType = CreateType.FILE
                    newFileName = "untitled.txt"
                    showNameDialog = true
                },
                onCreateFolder = {
                    createType = CreateType.FOLDER
                    newFileName = "untitled"
                    showNameDialog = true
                },
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
    root: File,
    file: File,
    refreshTrigger: Int,
    selectedFile: File?,
    onFileSelect: (File) -> Unit,
    level: Int = 0,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onDeleteFile: (File) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    val isSelected = selectedFile?.absolutePath == file.absolutePath

    // Recalculate children when refreshTrigger changes
    val children by remember(refreshTrigger, file.absolutePath) {
        mutableStateOf(file.listFiles()?.sortedBy { it.name } ?: emptyList())
    }

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("New File") {
                    onCreateFile()
                },
                ContextMenuItem("New Folder") {
                    onCreateFolder()
                },
                ContextMenuItem("Delete") {
                    deleteFileOrDirectory(root, file)
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
                    onFileSelect(file)
                    if (file.isDirectory) {
                        expanded = !expanded
                    }
                }
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isHovered -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.background
                    }
                )
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
                tint = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )
            Text(
                text = file.name.ifEmpty { file.path },
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )
        }
    }

    if (expanded && file.isDirectory) {
        Column {
            for (child in children) {
                FileNode(
                    file = child,
                    root = root,
                    refreshTrigger = refreshTrigger,
                    selectedFile = selectedFile,
                    onFileSelect = onFileSelect,
                    level = level + 1,
                    onCreateFile = onCreateFile,
                    onCreateFolder = onCreateFolder,
                    onDeleteFile = onDeleteFile,
                )
            }
        }
    }
}

fun createFileRelativeTo(selected: File, newFileName: String): File {
    val targetDir = selected.takeIf { it.isDirectory }
        ?: selected.parentFile
        ?: error("Selected file has no parent")

    val newFile = File(targetDir, newFileName)
    if (!newFile.exists()) {
        newFile.createNewFile()
        println("Created: ${newFile.absolutePath}")
    } else {
        println("File already exists: ${newFile.absolutePath}")
    }
    return newFile
}

fun createFolderRelativeTo(selected: File, newFolderName: String): File {
    val targetDir = selected.takeIf { it.isDirectory }
        ?: selected.parentFile
        ?: error("Selected file has no parent")

    val newFolder = File(targetDir, newFolderName)
    if (!newFolder.exists()) {
        newFolder.mkdirs()
        println("Created folder: ${newFolder.absolutePath}")
    } else {
        println("Folder already exists: ${newFolder.absolutePath}")
    }
    return newFolder
}

fun deleteFileOrDirectory(root: File, target: File): Boolean {
    if (target == root) return false // TODO
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