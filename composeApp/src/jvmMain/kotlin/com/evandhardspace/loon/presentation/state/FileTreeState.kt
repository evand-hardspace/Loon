package com.evandhardspace.loon.presentation.state


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

interface FileTreeState : State {
    val rootNode: FileNode?
    val deletedFile: Flow<File>
    fun initialize(root: File)
    fun addNode(relativeTo: File, newFile: File, name: String)
    fun deleteFile(file: File)
    fun refreshNode(node: FileNode)
    fun findNode(file: File): MutableFileNode?
    fun expandNode(node: FileNode)
    fun collapseNode(node: FileNode)
    fun toggleNode(node: FileNode)
}

internal class DefaultFileTreeState : FileTreeState, ViewModel() {
    override var rootNode: MutableFileNode? by mutableStateOf(null)
        private set

    private val _deletedFile = MutableSharedFlow<File>()
    override val deletedFile: Flow<File> = _deletedFile.asSharedFlow()

    override fun initialize(root: File) {
        rootNode = buildFileNode(root, isExpanded = true)
    }

    private fun buildFileNode(file: File, isExpanded: Boolean = false): MutableFileNode {
        val children = if (file.isDirectory && isExpanded) {
            file.listFiles()
                ?.filterNot { it.isDirectory && it.name == ".git" }
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

    override fun addNode(relativeTo: File, newFile: File, name: String) {
        findNode(relativeTo)?.let { parentNode ->
            val parent = if (parentNode.file.isDirectory) parentNode else findNode(
                parentNode.file.parentFile ?: error("no parent file")
            )
            parent?.let {
                val newNode = MutableFileNode(newFile, isExpanded = false)
                it.addChild(newNode, sort = true)
            }
        }
    }

    override fun deleteFile(file: File) {
        val root = rootNode ?: return
        if (file == root.file) return

        findParentNode(root, file)?.removeChild(file)
        viewModelScope.launch {
            _deletedFile.emit(file)
        }
    }

    override fun refreshNode(node: FileNode) {
        if (node !is MutableFileNode) return
        if (!node.file.isDirectory) return

        val currentChildren = node.children.toList()
        val actualFiles = node.file.listFiles()
            ?.filterNot { it.isDirectory && it.name == ".git" }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()

        // Remove deleted children
        currentChildren.forEach { child ->
            if (!child.file.exists()) {
                node.removeChild(child.file)
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

    fun removeChild(child: File) {
        _children.removeIf { it.file == child }
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
