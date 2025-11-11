package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.lifecycle.ViewModel
import java.io.File

interface State

interface DirtyFilesState : State {
    val dirtyFiles: Set<DirtyFile>

    fun isFileDirty(file: String): Boolean

    fun updateIsDirty(
        path: String,
        isDirty: Boolean,
    )

    fun add(
        file: File,
        isDirty: Boolean = false,
    )

    fun remove(path: String)
}

internal class DefaultDirtyFilesState : DirtyFilesState, ViewModel() {

    private val _dirtyFiles: SnapshotStateSet<DirtyFile> = mutableStateSetOf()
    override val dirtyFiles: Set<DirtyFile> get() = _dirtyFiles

    override fun updateIsDirty(
        path: String,
        isDirty: Boolean,
    ) = update(path) { it.copy(isDirty = isDirty) }

    override fun add(
        file: File,
        isDirty: Boolean,
    ) {
        withMutableSnapshot {
            val path = file.absolutePath
            val existing = _dirtyFiles.find { it.file.absolutePath == path }
            if (existing != null) _dirtyFiles.remove(existing)
            _dirtyFiles.add(
                DirtyFile(
                    file = file,
                    isDirty = isDirty,
                )
            )
        }
    }

    override fun isFileDirty(file: String): Boolean =
        dirtyFiles.find { it.file.absolutePath == file }?.isDirty ?: false


    override fun remove(path: String) {
        withMutableSnapshot {
            _dirtyFiles.find { it.file.absolutePath == path }?.let(_dirtyFiles::remove)
        }
    }

    private fun update(
        path: String,
        transform: (DirtyFile) -> DirtyFile,
    ) {
        withMutableSnapshot {
            _dirtyFiles.find { it.file.absolutePath == path }?.let { old ->
                _dirtyFiles.remove(old)
                _dirtyFiles.add(transform(old))
            }
        }
    }
}

data class DirtyFile(
    val file: File,
    val isDirty: Boolean,
) {
    override fun equals(other: Any?) =
        other is DirtyFile && file.absolutePath == other.file.absolutePath

    override fun hashCode(): Int = file.absolutePath.hashCode()
}