package com.evandhardspace.loon.features.filetree

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.evandhardspace.loon.presentation.state.CreateType
import com.evandhardspace.loon.presentation.state.DirtyFilesSlice
import com.evandhardspace.loon.presentation.state.FileNode
import com.evandhardspace.loon.presentation.state.FileSlice
import com.evandhardspace.loon.presentation.state.SelectedFileSlice
import com.evandhardspace.loon.presentation.state.getSlice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds

@Composable
fun FileTree(
    root: File,
    modifier: Modifier = Modifier,
    onDeleteFile: (File) -> Unit,
    onFileSelect: (File) -> Unit,
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf(CreateType.FILE) }
    var newFileName by remember { mutableStateOf("") }

    val fileSlice: FileSlice = remember { getSlice() }
    val dirtyFilesSlice: DirtyFilesSlice = remember { getSlice() }
    val selectedFileSlice: SelectedFileSlice = remember { getSlice() }

    LaunchedEffect(root) {
        fileSlice.initialize(root)
    }

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
                fileSlice.rootNode?.let { fileSlice.refreshNode(it) }
            }
        }
    }

    val selectedFile = selectedFileSlice.selectedFileOrDirectory
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
                Column(modifier = Modifier.padding(16.dp)) {
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
                            Text(if (createType == CreateType.FILE) "File name" else "Folder name")
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
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                                        fileSlice.createFile(selectedFile ?: root, newFileName)
                                    } else {
                                        fileSlice.createFolder(selectedFile ?: root, newFileName)
                                    }
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
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                fileSlice.findNode(selectedFile)?.let { node ->
                                    fileSlice.deleteNode(node)
                                    onDeleteFile(selectedFile)
                                }
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
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
        modifier = modifier.onKeyEvent { event ->
            when {
                event.type == KeyEventType.KeyDown &&
                        event.isMetaPressed &&
                        event.key == Key.Backspace &&
                        selectedFile != null -> {
                    showDeleteDialog = true
                    true
                }
                event.type == KeyEventType.KeyDown &&
                        event.isMetaPressed &&
                        event.key == Key.N -> {
                    createType = CreateType.FILE
                    newFileName = "untitled.txt"
                    showNameDialog = true
                    true
                }
                event.type == KeyEventType.KeyDown &&
                        event.isMetaPressed &&
                        event.isShiftPressed &&
                        event.key == Key.N -> {
                    createType = CreateType.FOLDER
                    newFileName = "untitled"
                    showNameDialog = true
                    true
                }
                else -> false
            }
        }
    ) {
        fileSlice.rootNode?.let { root ->
            item {
                FileNodeView(
                    node = root,
                    fileSlice = fileSlice,
                    isDirty = { file -> dirtyFilesSlice.dirtyStates.find { it.file.absolutePath == file }?.isDirty ?: false },
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
                        showDeleteDialog = true
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileNodeView(
    node: FileNode,
    fileSlice: FileSlice,
    isDirty: (path: String) -> Boolean,
    selectedFile: File?,
    onFileSelect: (File) -> Unit,
    level: Int = 0,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onDeleteFile: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }
    val isSelected = selectedFile?.absolutePath == node.file.absolutePath

    ContextMenuArea(
        items = {
            if (!isSelected) {
                onFileSelect(node.file)
            }
            listOf(
                ContextMenuItem("New File") { onCreateFile() },
                ContextMenuItem("New Folder") { onCreateFolder() },
                ContextMenuItem("Delete") { onDeleteFile() },
            )
        }
    ) {
        Row(
            modifier = Modifier
                .padding(start = (level * 16).dp)
                .fillMaxWidth()
                .clickable {
                    if (node.file.isDirectory) {
                        if (isSelected) {
                            fileSlice.toggleNode(node)
                        } else {
                            onFileSelect(node.file)
                        }
                    } else {
                        onFileSelect(node.file)
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
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (node.file.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 2.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            val icon = when {
                node.file.isDirectory && node.isExpanded -> Icons.Default.FolderOpen
                node.file.isDirectory -> Icons.Default.Folder
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = node.file.name.ifEmpty { node.file.path },
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
            )
            if (isDirty(node.file.absolutePath)) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                        )
                        .size(6.dp)
                )
            }
        }
    }

    if (node.isExpanded && node.file.isDirectory) {
        Column {
            for (child in node.children) {
                FileNodeView(
                    node = child,
                    fileSlice = fileSlice,
                    isDirty = isDirty,
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