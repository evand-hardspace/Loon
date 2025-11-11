package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

interface TabsState : State {
    val tabs: List<File>
    val tabEvents: Flow<TabEvent>

    suspend fun addTab(file: File)

    suspend fun removeTab(file: File)
}

internal class DefaultTabsState : TabsState, ViewModel() {

    private val _tabs: SnapshotStateList<File> = mutableStateListOf()
    override val tabs: List<File> = _tabs

    private val _tabEvents = MutableSharedFlow<TabEvent>()
    override val tabEvents: Flow<TabEvent> = _tabEvents.asSharedFlow()

    override suspend fun addTab(file: File) {
        _tabs.add(file)
        _tabEvents.emit(TabEvent.AddedTab(file))
    }

    override suspend fun removeTab(file: File) {
        _tabs.remove(file)
        _tabEvents.emit(TabEvent.RemovedTab(file))
    }
}

sealed interface TabEvent {
    data class RemovedTab(val file: File) : TabEvent
    data class AddedTab(val file: File) : TabEvent
}