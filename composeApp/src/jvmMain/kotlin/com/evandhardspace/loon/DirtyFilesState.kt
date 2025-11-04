package com.evandhardspace.loon

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.compose.runtime.snapshots.SnapshotStateSet
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DirtyFilesState {
    val state: Set<DirtyState>
    val stateAsFlow: Flow<Set<DirtyState>>

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

fun DirtyFilesState(): DirtyFilesState = DefaultDirtyFilesState

internal object DefaultDirtyFilesState : DirtyFilesState {

    private val _state: SnapshotStateSet<DirtyState> = mutableStateSetOf()
    override val state: Set<DirtyState> get() = _state

    // Flow for observing changes
    override val stateAsFlow: Flow<Set<DirtyState>> = snapshotFlow { _state.toSet() }

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
            val existing = _state.find { it.file.absolutePath == path }
            if (existing != null) _state.remove(existing)
            _state.add(
                DirtyState(
                    file = file,
                    isDirty = isDirty,
                )
            )
        }
    }

    override fun remove(path: String) {
        withMutableSnapshot {
            _state.find { it.file.absolutePath == path }?.let(_state::remove)
        }
    }

    private fun update(
        path: String,
        transform: (DirtyState) -> DirtyState,
    ) {
        withMutableSnapshot {
            _state.find { it.file.absolutePath == path }?.let { old ->
                _state.remove(old)
                _state.add(transform(old))
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