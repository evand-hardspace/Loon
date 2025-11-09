package com.evandhardspace.loon.presentation.state


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

interface FileState : State {
    val rootNode: FileNode?
    fun initialize(root: File)
    fun createFile(relativeTo: File, name: String): File?
    fun createFolder(relativeTo: File, name: String): File?
    fun deleteNode(node: FileNode): Boolean
    fun refreshNode(node: FileNode)
    fun findNode(file: File): MutableFileNode?
    fun expandNode(node: FileNode)
    fun collapseNode(node: FileNode)
    fun toggleNode(node: FileNode)
}

internal class DefaultFileState : FileState {
    override var rootNode: MutableFileNode? by mutableStateOf(null)
        private set

    override fun initialize(root: File) {
        rootNode = buildFileNode(root, isExpanded = true)
    }

    private fun buildFileNode(file: File, isExpanded: Boolean = false): MutableFileNode {
        val children = if (file.isDirectory && isExpanded) {
            file.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                ?.map { buildFileNode(it, isExpanded = false) }
                ?: emptyList()
        } else {
            emptyList()
        }

        return MutableFileNode(
            file = file,
            isExpanded = isExpanded,
            children = children
        )
    }

    override fun createFile(relativeTo: File, name: String): File {
        val newFile = createFileRelativeTo(relativeTo, name)
        findNode(relativeTo)?.let { parentNode ->
            val parent = if (parentNode.file.isDirectory) parentNode else findNode(
                parentNode.file.parentFile ?: error("no parent file")
            )
            parent?.let {
                val newNode = MutableFileNode(newFile, isExpanded = false)
                it.addChild(newNode, sort = true)
            }
        }
        return newFile
    }

    override fun createFolder(relativeTo: File, name: String): File {
        val newFolder = createFolderRelativeTo(relativeTo, name)
        findNode(relativeTo)?.let { parentNode ->
            val parent = if (parentNode.file.isDirectory) parentNode else findNode(parentNode.file.parentFile ?: error("No parent file"))
            parent?.let {
                val newNode = MutableFileNode(newFolder, isExpanded = false)
                it.addChild(newNode)
            }
        }
        return newFolder
    }

    override fun deleteNode(node: FileNode): Boolean {
        val root = rootNode ?: return false
        if (node == root) return false

        val success = deleteFileOrDirectory(root.file, node.file)
        if (success) {
            findParentNode(root, node.file)?.removeChild(node)
        }
        return success
    }

    override fun refreshNode(node: FileNode) {
        if (node !is MutableFileNode) return
        if (!node.file.isDirectory) return

        val currentChildren = node.children.toList()
        val actualFiles = node.file.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()

        // Remove deleted children
        currentChildren.forEach { child ->
            if (!child.file.exists()) {
                node.removeChild(child)
            }
        }

        // Add new children
        actualFiles.forEach { file ->
            if (currentChildren.none { it.file == file }) {
                node.addChild(MutableFileNode(file, isExpanded = false), sort = false)
            }
        }

        node.sortChildren()
    }

    override fun findNode(file: File): MutableFileNode? {
        return findNodeRecursive(rootNode, file)
    }

    private fun findNodeRecursive(node: MutableFileNode?, target: File): MutableFileNode? {
        if (node == null) return null
        if (node.file == target) return node
        for (child in node.children) {
            findNodeRecursive(child, target)?.let { return it }
        }
        return null
    }

    private fun findParentNode(root: MutableFileNode, target: File): MutableFileNode? {
        for (child in root.children) {
            if (child.file == target) return root
            findParentNode(child, target)?.let { return it }
        }
        return null
    }

    override fun expandNode(node: FileNode) {
        if (node is MutableFileNode && node.file.isDirectory) {
            node.isExpanded = true
            if (node.children.isEmpty()) {
                refreshNode(node)
            }
        }
    }

    override fun collapseNode(node: FileNode) {
        if (node is MutableFileNode && node.file.isDirectory) {
            node.isExpanded = false
        }
    }

    override fun toggleNode(node: FileNode) {
        if (node is MutableFileNode && node.file.isDirectory) {
            node.toggleExpanded()
            if (node.isExpanded && node.children.isEmpty()) {
                refreshNode(node)
            }
        }
    }
}

interface FileNode {
    val file: File
    val isExpanded: Boolean
    val children: List<FileNode>
}

class MutableFileNode(
    override val file: File,
    isExpanded: Boolean = false,
    children: List<MutableFileNode> = emptyList(),
) : FileNode {
    override var isExpanded by mutableStateOf(isExpanded)

    private val _children = mutableStateListOf<MutableFileNode>().apply { addAll(children) }
    override val children: List<MutableFileNode> = _children

    fun toggleExpanded() {
        isExpanded = !isExpanded
    }


    fun addChild(child: MutableFileNode, sort: Boolean = true) {
        if (!_children.contains(child)) {
            _children.add(child)
            if (sort) sortChildren()
        }
    }

    fun removeChild(child: FileNode) {
        _children.remove(child)
    }

    fun sortChildren() {
        val sorted = _children.sortedWith(compareBy({ !it.file.isDirectory }, { it.file.name }))
        _children.clear()
        _children.addAll(sorted)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileNode
        return file == other.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}

enum class CreateType {
    FILE, FOLDER
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