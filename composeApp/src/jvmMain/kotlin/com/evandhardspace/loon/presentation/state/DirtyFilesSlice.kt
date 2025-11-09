package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.compose.runtime.snapshots.SnapshotStateSet
import java.io.File

interface Slice

interface DirtyFilesSlice: Slice {
    val dirtyStates: Set<DirtyState>

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

internal class DefaultDirtyFilesSlice : DirtyFilesSlice {

    private val _dirtyStates: SnapshotStateSet<DirtyState> = mutableStateSetOf()
    override val dirtyStates: Set<DirtyState> get() = _dirtyStates

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
            val existing = _dirtyStates.find { it.file.absolutePath == path }
            if (existing != null) _dirtyStates.remove(existing)
            _dirtyStates.add(
                DirtyState(
                    file = file,
                    isDirty = isDirty,
                )
            )
        }
    }

    override fun remove(path: String) {
        withMutableSnapshot {
            _dirtyStates.find { it.file.absolutePath == path }?.let(_dirtyStates::remove)
        }
    }

    private fun update(
        path: String,
        transform: (DirtyState) -> DirtyState,
    ) {
        withMutableSnapshot {
            _dirtyStates.find { it.file.absolutePath == path }?.let { old ->
                _dirtyStates.remove(old)
                _dirtyStates.add(transform(old))
            }
        }
    }
}

data class DirtyState(
    val file: File,
    val isDirty: Boolean,
) {
    // Equality based only on path
    override fun equals(other: Any?) =
        other is DirtyState && file.absolutePath == other.file.absolutePath

    override fun hashCode(): Int = file.absolutePath.hashCode()
}