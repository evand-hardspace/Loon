package com.evandhardspace.loon

import com.evandhardspace.loon.presentation.state.DefaultDirtyFilesSlice
import com.evandhardspace.loon.presentation.state.DefaultFileSlice
import com.evandhardspace.loon.presentation.state.DefaultSelectedFileSlice
import com.evandhardspace.loon.presentation.state.DefaultTabsSlice
import com.evandhardspace.loon.presentation.state.DirtyFilesSlice
import com.evandhardspace.loon.presentation.state.FileSlice
import com.evandhardspace.loon.presentation.state.SelectedFileSlice
import com.evandhardspace.loon.presentation.state.TabsSlice
import com.evandhardspace.loon.presentation.state.register
import com.evandhardspace.loon.presentation.state.registerSlice

fun initStates() {
    registerSlice {
        register<DirtyFilesSlice> {
            DefaultDirtyFilesSlice()
        }
        register<SelectedFileSlice> {
            DefaultSelectedFileSlice()
        }
        register<TabsSlice> {
            DefaultTabsSlice()
        }
        register<FileSlice> {
            DefaultFileSlice()
        }
    }
}