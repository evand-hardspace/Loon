package com.evandhardspace.loon.features.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evandhardspace.loon.features.filetree.relativeToRoot
import com.evandhardspace.loon.features.vcs.GitFileStatus
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
import java.io.File

@Composable
fun TabSelector(
    modifier: Modifier = Modifier,
    tabs: List<File>,
    selectedTabFile: File?,
    onClose: (selectedFile: File) -> Unit,
) {
    var isTabMoveForwardOpened: Boolean? by remember { mutableStateOf(null) }

    handleKeyEvent<AppKeyEvent.TabMenu>("work_space") { event ->
        isTabMoveForwardOpened = event.isShiftPressed.not()
        true
    }
    handleKeyEvent<AppKeyEvent.ReleaseCtrl>("work_space") {
        isTabMoveForwardOpened = null
        true
    }

    isTabMoveForwardOpened?.let { moveForward ->
        TabSelectorContent(
            modifier = modifier,
            moveForward = moveForward,
            tabs = tabs,
            selectedTabFile = selectedTabFile,
            onClose = { file ->
                isTabMoveForwardOpened = null
                onClose(file)
            },
        )
    }
}

@Composable
fun TabSelectorContent(
    tabs: List<File>,
    moveForward: Boolean,
    selectedTabFile: File?,
    onClose: (selectedFile: File) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    var selectedTabIndex by remember(tabs) {
        mutableIntStateOf(
            tabs.indexOf(selectedTabFile).takeIf { it >= 0 }
                ?.let {
                    (if (moveForward) it + 1 else {
                        if (it - 1 < 0) tabs.size - 1 else it - 1
                    }
                            ) % tabs.size
                } ?: 0 // todo extract
        )
    }
    handleKeyEvent<AppKeyEvent.ReleaseCtrl>("tab_selector") {
        onClose(tabs[selectedTabIndex])
        true
    }

    handleKeyEvent<AppKeyEvent.TabMenu>("tab_selector") { event ->
        selectedTabIndex = (
                if (event.isShiftPressed) {
                    if (selectedTabIndex - 1 < 0) tabs.size - 1 else selectedTabIndex - 1 // todo extract
                } else {
                    selectedTabIndex + 1
                }
                ) % tabs.size
        true
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            tabs.forEachIndexed { i, item ->
                Text(
                    text = item.name,
                    color = if (i == selectedTabIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}