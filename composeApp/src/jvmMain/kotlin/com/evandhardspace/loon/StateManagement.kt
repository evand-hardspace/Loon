package com.evandhardspace.loon

import com.evandhardspace.loon.presentation.state.DefaultDirtyFilesState
import com.evandhardspace.loon.presentation.state.DefaultFileState
import com.evandhardspace.loon.presentation.state.DefaultSelectedFileState
import com.evandhardspace.loon.presentation.state.DefaultTabsState
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.FileState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.TabsState
import com.evandhardspace.loon.presentation.state.register
import com.evandhardspace.loon.presentation.state.registerStates

fun initStates() {
    registerStates {
        register<DirtyFilesState> {
            DefaultDirtyFilesState()
        }
        register<SelectedFileState> {
            DefaultSelectedFileState()
        }
        register<TabsState> {
            DefaultTabsState()
        }
        register<FileState> {
            DefaultFileState()
        }
    }
}