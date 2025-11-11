package com.evandhardspace.loon.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified S> state(
    crossinline initializer: () -> S,
): S where S : State, S : ViewModel {
    return viewModel { initializer() }
}

@Composable
fun ProvideStates(
    content: @Composable () -> Unit,
) {
    val dirtyFilesState: DirtyFilesState = state { DefaultDirtyFilesState() }
    val selectedFileState: SelectedFileState = state { DefaultSelectedFileState() }
    val tabsState: TabsState = state { DefaultTabsState() }
    val fileTreeState: FileTreeState = state { DefaultFileTreeState() }

    CompositionLocalProvider(
        LocalDirtyFilesState provides dirtyFilesState,
        LocalSelectedFileState provides selectedFileState,
        LocalTabsState provides tabsState,
        LocalFileTreeState provides fileTreeState,
        content = content,
    )
}

@Composable
inline fun <reified VM : ViewModel> viewModelWithState(
    crossinline initializer: @Composable StateScope.() -> VM,
): VM = StateScope.initializer().let { vm -> viewModel { vm } }

data object StateScope

@Suppress("UNCHECKED_CAST")
@Composable
inline fun <reified T : State> StateScope.get(): T = LocalStates[T::class]!!.current as T
