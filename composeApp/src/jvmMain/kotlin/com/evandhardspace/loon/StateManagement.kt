package com.evandhardspace.loon

import com.evandhardspace.loon.presentation.state.DefaultDirtyFilesState
import com.evandhardspace.loon.presentation.state.DefaultSelectedFileState
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.SelectedFileState
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
    }
}