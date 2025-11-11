package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

val LocalDirtyFilesState: ProvidableCompositionLocal<DirtyFilesState> = compositionLocalOf { error("Not provided") }
val LocalSelectedFileState: ProvidableCompositionLocal<SelectedFileState> = compositionLocalOf { error("Not provided") }
val LocalTabsState: ProvidableCompositionLocal<TabsState> = compositionLocalOf { error("Not provided") }
val LocalFileState: ProvidableCompositionLocal<FileState> = compositionLocalOf { error("not provided") }

val LocalStates = mapOf(
    DirtyFilesState::class to LocalDirtyFilesState,
    SelectedFileState::class to LocalSelectedFileState,
    TabsState::class to LocalTabsState,
    FileState::class to LocalFileState,
)