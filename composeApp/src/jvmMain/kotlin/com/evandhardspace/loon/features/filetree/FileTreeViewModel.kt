package com.evandhardspace.loon.features.filetree

import androidx.lifecycle.ViewModel
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.FileNode
import com.evandhardspace.loon.presentation.state.FileTreeState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import java.io.File

class FileTreeViewModel(
    private val selectedFileState: SelectedFileState,
    private val dirtyFilesState: DirtyFilesState,
    private val fileTreeState: FileTreeState,
) : ViewModel() {

    private lateinit var root: File

    val rootNode: FileNode?
        get() = fileTreeState.rootNode

    fun initialize(root: File) {
        this.root = root
        fileTreeState.initialize(root)
    }

    fun refreshRootNode() {
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
            fileTreeState.deleteNode(target)
        }

        return success
    }

    fun onNodeClick(node: FileNode) {
        fileTreeState.toggleNode(node)
    }

    val selectedFileOrDirectory: File?
        get() = selectedFileState.selectedFileOrDirectory

    fun isFileDirty(file: String): Boolean =
        dirtyFilesState.dirtyFiles.find { it.file.absolutePath == file }?.isDirty ?: false

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