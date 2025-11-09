package com.evandhardspace.loon.features.filetree

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.evandhardspace.loon.dialog.AppDialog
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
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
    var showNameDialogFile: File? by remember { mutableStateOf(null) }
    var showDeleteDialogFile: File? by remember { mutableStateOf(null) }
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
        ) // todo: move to presentation layer

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
    val targetDir = showNameDialogFile?.takeIf { it.isDirectory }
        ?: showNameDialogFile?.parentFile
        ?: root
    val fileExists = File(targetDir, newFileName).exists()
    val fileNameIsBlank = newFileName.isBlank()

    handleKeyEvent<AppKeyEvent.New>("filetree") { event ->
        if (event.isShiftPressed) {
            createType = CreateType.FOLDER
            newFileName = "untitled"
        } else {
            createType = CreateType.FILE
            newFileName = "untitled.txt"
        }
        showNameDialogFile = selectedFile ?: root
        true
    }

    handleKeyEvent<AppKeyEvent.Remove>("filetree") {
        showDeleteDialogFile = selectedFileSlice.selectedFileOrDirectory
        true
    }

    val newFileDialogClick = {
        if (newFileName.isNotBlank() && !fileExists) {
            if (createType == CreateType.FILE) {
                fileSlice.createFile(showNameDialogFile ?: root, newFileName)
            } else {
                fileSlice.createFolder(showNameDialogFile ?: root, newFileName)
            }
            showNameDialogFile = null
            newFileName = ""
        }
    }

    if (showNameDialogFile != null) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AppDialog(
            onDismissRequest = {
                showNameDialogFile = null
                newFileName = ""
            },
            onSubmitAction = newFileDialogClick,
        ) {
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
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
                            showNameDialogFile = null
                            newFileName = ""
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = newFileDialogClick,
                            enabled = !fileNameIsBlank && !fileExists
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    showDeleteDialogFile?.let { file ->
        val deleteFileDialogClick = {
            fileSlice.findNode(file)?.let { node ->
                fileSlice.deleteNode(node)
                onDeleteFile(file)
            }
            showDeleteDialogFile = null
        }
        AppDialog(
            onDismissRequest = { showDeleteDialogFile = null },
            onSubmitAction = deleteFileDialogClick,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Delete ${if (file.isDirectory) "Folder" else "File"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Are you sure you want to delete \"${file.name}\"?${if (file.isDirectory) " This will delete all contents." else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showDeleteDialogFile = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = deleteFileDialogClick,
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

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("New File") {
                    createType = CreateType.FILE
                    newFileName = "untitled.txt"
                    showNameDialogFile = root
                },
                ContextMenuItem("New Folder") {
                    createType = CreateType.FOLDER
                    newFileName = "untitled"
                    showNameDialogFile = root
                },
            )
        }
    ) {
        BoxWithConstraints {
            LazyColumn(
                modifier = modifier.then(
                    if (maxWidth < 200.dp) Modifier.horizontalScroll(rememberScrollState()) else Modifier
                )
                    .width(maxWidth.coerceAtLeast(200.dp))
            ) {
                fileSlice.rootNode?.let { root ->
                    item {
                        FileNodeView(
                            node = root,
                            fileSlice = fileSlice,
                            isDirty = { file ->
                                dirtyFilesSlice.dirtyStates.find { it.file.absolutePath == file }?.isDirty ?: false
                            },
                            selectedFile = selectedFile,
                            onFileSelect = onFileSelect,
                            level = 0,
                            onCreateFile = { file ->
                                createType = CreateType.FILE
                                newFileName = "untitled.txt"
                                showNameDialogFile = file
                            },
                            onCreateFolder = { folder ->
                                createType = CreateType.FOLDER
                                newFileName = "untitled"
                                showNameDialogFile = folder
                            },
                            onDeleteFile = { file ->
                                showDeleteDialogFile = file
                            },
                            isRoot = true,
                        )
                    }
                }
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
    onCreateFile: (File) -> Unit,
    onCreateFolder: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    isRoot: Boolean = false,
) {
    var isHovered by remember { mutableStateOf(false) }
    val isSelected = selectedFile?.absolutePath == node.file.absolutePath

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("New File") { onCreateFile(node.file) },
                ContextMenuItem("New Folder") { onCreateFolder(node.file) },
            ).let { items ->
                if (!isRoot) {
                    items + ContextMenuItem("Delete") { onDeleteFile(node.file) }
                } else {
                    items
                }
            }
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
            verticalAlignment = Alignment.Top,
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
                text = node.file.name.ifEmpty { node.file.path } + if (isDirty(node.file.absolutePath)) Typography.bullet else "",
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                    isRoot = false,
                )
            }
        }
    }
}