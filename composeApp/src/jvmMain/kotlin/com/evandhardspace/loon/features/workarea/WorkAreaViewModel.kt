package com.evandhardspace.loon.features.workarea

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.TabEvent
import com.evandhardspace.loon.presentation.state.TabsState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

class WorkAreaViewModel(
    private val selectedFileState: SelectedFileState,
    dirtyFileState: DirtyFilesState,
    tabsState: TabsState,
) : ViewModel() {
    val holders: SnapshotStateMap<String, WorkAreaHolder> = mutableStateMapOf()

    private val workHolderFactory: WorkAreaHolderFactory = WorkAreaHolderFactory(dirtyFileState)

    val currentHolder: WorkAreaHolder?
        get() = holders[selected]

    val selected: String?
        get() = selectedFileState.selectedFile?.absolutePath

    init {
        tabsState
            .tabEvents
            .onEach { event ->
                when (event) {
                    is TabEvent.AddedTab -> addHolder(event.file)
                    is TabEvent.RemovedTab -> removeHolder(event.file.absolutePath)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun addHolder(file: File) {
        val holder = workHolderFactory.create(file)
        holder.init()
        holders[file.absolutePath] = holder
    }

    private fun removeHolder(filePath: String) {
        val holder = holders.remove(filePath)
        holder?.dispose()
    }
}
