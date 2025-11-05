package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.Flow
import java.io.File

interface TabsState : State {
    val tabs: List<File>
    val tabsAsFlow: Flow<List<File>>

    fun addTab(file: File)

    fun removeTab(file: File)
}

internal class DefaultTabsState : TabsState {
    private val _tabs: SnapshotStateList<File> = mutableStateListOf()
    override val tabs: List<File> = _tabs
    override val tabsAsFlow: Flow<List<File>> = snapshotFlow { _tabs.toList() }

    override fun addTab(file: File) {
        _tabs.add(file)
    }

    override fun removeTab(file: File) {
        _tabs.remove(file)
    }
}