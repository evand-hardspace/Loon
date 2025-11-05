package com.evandhardspace.loon.features.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.evandhardspace.loon.presentation.state.DirtyFilesState
import com.evandhardspace.loon.presentation.state.SelectedFileState
import com.evandhardspace.loon.presentation.state.getState
import org.jetbrains.skiko.Cursor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    tabs: List<File>,
    selectedTab: File?,
    onTabClick: (File) -> Unit,
    onTabClosed: (File) -> Unit,
) {
    val dirtyFilesState: DirtyFilesState = getState()

    if (tabs.isEmpty()) {
        Spacer(Modifier.height(20.dp))
        return
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
            tabs
            .filter { it.isFile }
            .forEach { file ->
                val selected = file == selectedTab
                val isDirty = dirtyFilesState.dirtyStates.find { it.file.absolutePath == file.absolutePath }?.isDirty ?: false
                var isHovered by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .clickable(onClick = { onTabClick(file) })
                        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                        .background(if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .background(if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Dirty indicator (blue dot)
                        if (isDirty) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2196F3), CircleShape)
                                    .padding(end = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = file.name,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close $file",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    onTabClosed(file)
                                }
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
    }
}