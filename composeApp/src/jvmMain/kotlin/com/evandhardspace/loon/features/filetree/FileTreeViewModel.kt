package com.evandhardspace.loon.features.filetree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evandhardspace.loon.features.vcs.GitFileStatus
import com.evandhardspace.loon.features.vcs.GitRepository
import com.evandhardspace.loon.features.vcs.GitWatcher
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.FileNode
import com.evandhardspace.loon.presentation.state.FileTreeState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds

class FileTreeViewModel(
    private val selectedFileState: SelectedFileState,
    private val dirtyFilesState: DirtyFilesState,
    private val fileTreeState: FileTreeState,
    private val gitRepository: GitRepository,
    private val gitWatcher: GitWatcher,
) : ViewModel() {

    private lateinit var root: File
    private var fileWatcherJob: Job? = null

    val rootNode: FileNode?
        get() = fileTreeState.rootNode

    private val _gitStatus = MutableStateFlow(GitFileStatus())
    val gitStatus = _gitStatus.asStateFlow()

    fun initialize(root: File) {
        this.root = root
        fileTreeState.initialize(root)
        val watchService = FileSystems.getDefault().newWatchService()
        root.toPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        ) // todo: move to presentation layer

        fileWatcherJob?.cancel()
        fileWatcherJob = viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    val key = watchService.take()
                    key.pollEvents()
                    key.reset()
                    refreshRootNode()
                }
        }

        gitWatcher
            .watchGitChanges()
            .filterNotNull()
            .onEach { status ->
                _gitStatus.update { status }
            }
            .launchIn(viewModelScope)
    }

    fun stageFile(relativePath: String) {
        gitRepository.stageFile(relativePath)
    }

    fun unstageFile(relativePath: String) {
        gitRepository.unstageFile(relativePath)
    }

    private fun refreshRootNode() {
        fileTreeState.rootNode?.let { fileTreeState.refreshNode(it) }
    }

    fun selectFile(file: File) {
        selectedFileState.selectFile(file)
    }

    fun createFolder(relativeTo: File, name: String) {
        val newFolder = createFolderRelativeTo(relativeTo, name)
        fileTreeState.addNode(relativeTo, newFolder, name)
    }

    fun createFile(relativeTo: File, name: String) {
        val newFile = createFileRelativeTo(relativeTo, name)
        fileTreeState.addNode(relativeTo, newFile, name)
    }

    fun deleteFile(target: File): Boolean {
        val success = deleteFileOrDirectory(root, target)

        if(success) {
            fileTreeState.deleteFile(target)
        }

        return success
    }

    fun onNodeClick(node: FileNode) {
        fileTreeState.toggleNode(node)
    }

    val selectedFileOrDirectory: File?
        get() = selectedFileState.selectedFileOrDirectory

    fun isFileDirty(file: String): Boolean = dirtyFilesState.isFileDirty(file)

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
    if (target == root) return false
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