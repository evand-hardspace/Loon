package com.evandhardspace.loon.features.filetree

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evandhardspace.loon.dialog.AppDialog
import com.evandhardspace.loon.features.tab.TabViewModel
import com.evandhardspace.loon.features.vcs.GitFileStatus
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
import com.evandhardspace.loon.presentation.state.*
import java.io.File
import kotlin.text.Typography
import kotlin.text.ifEmpty
import kotlin.text.isBlank
import kotlin.text.isNotBlank

@Composable
fun FileTree(
    root: File,
    fileTreeViewModel: FileTreeViewModel,
    tabViewModel: TabViewModel,
    modifier: Modifier = Modifier,
) {
    var showNameDialogFile: File? by remember { mutableStateOf(null) }
    var showDeleteDialogFile: File? by remember { mutableStateOf(null) }
    var createType by remember { mutableStateOf(CreateType.FILE) }
    var newFileName by remember { mutableStateOf("") }

    LaunchedEffect(root) {
        fileTreeViewModel.initialize(root)
    }

    val gitStatus by fileTreeViewModel.gitStatus.collectAsStateWithLifecycle()

    val selectedFile = fileTreeViewModel.selectedFileOrDirectory
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
        showDeleteDialogFile = fileTreeViewModel.selectedFileOrDirectory
        true
    }

    val newFileDialogClick = {
        if (newFileName.isNotBlank() && !fileExists) {
            if (createType == CreateType.FILE) {
                fileTreeViewModel.createFile(showNameDialogFile ?: root, newFileName)
            } else {
                fileTreeViewModel.createFolder(showNameDialogFile ?: root, newFileName)
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
            fileTreeViewModel.deleteFile(file)
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
                fileTreeViewModel.rootNode?.let { root ->
                    item {
                        FileNodeView(
                            node = root,
                            gitStatus = gitStatus,
                            toggleNode = fileTreeViewModel::onNodeClick,
                            isDirty = fileTreeViewModel::isFileDirty,
                            selectedFile = selectedFile,
                            onFileSelect = {
                                tabViewModel.addTab(it)
                                fileTreeViewModel.selectFile(it)
                            },
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
                            root = root.file.absolutePath,
                            onStage = fileTreeViewModel::stageFile,
                            onUnStage = fileTreeViewModel::unstageFile,
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
    root: String,
    gitStatus: GitFileStatus,
    toggleNode: (FileNode) -> Unit,
    isDirty: (path: String) -> Boolean,
    onStage: (relativePath: String) -> Unit,
    onUnStage: (relativePath: String) -> Unit,
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
            buildList {
                ContextMenuItem("New File") { onCreateFile(node.file) }.let(::add)
                ContextMenuItem("New Folder") { onCreateFolder(node.file) }.let(::add)
                if(node.file.isDirectory.not() && node.file.relativeToRoot(root) in gitStatus.untracked) {
                    ContextMenuItem("Stage File") { onStage(node.file.relativeToRoot(root)) }.let(::add)
                }
                if(node.file.isDirectory.not() && node.file.relativeToRoot(root) in gitStatus.added) {
                    ContextMenuItem("Unstage File") { onUnStage(node.file.relativeToRoot(root)) }.let(::add)
                }
            }.let { items ->
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
                            toggleNode(node)
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
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    when (node.file.relativeToRoot(root)) {
                        in gitStatus.added -> Color(0xFF7CDE73)
                        in gitStatus.modified -> Color(0xFF6087C6)
                        in gitStatus.untracked -> Color(0xFFC67070)
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                },
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
                    gitStatus = gitStatus,
                    toggleNode= toggleNode,
                    isDirty = isDirty,
                    selectedFile = selectedFile,
                    onFileSelect = onFileSelect,
                    level = level + 1,
                    onCreateFile = onCreateFile,
                    onCreateFolder = onCreateFolder,
                    onDeleteFile = onDeleteFile,
                    isRoot = false,
                    onStage = onStage,
                    onUnStage = onUnStage,
                    root = root,
                )
            }
        }
    }
}

fun File.relativeToRoot(rootPath: String): String =
    this.absolutePath.removePrefix(rootPath + File.separator)
