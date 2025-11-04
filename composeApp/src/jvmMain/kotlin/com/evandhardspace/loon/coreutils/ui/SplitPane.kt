package com.evandhardspace.loon.coreutils.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
fun SplitPane(
    leftContent: @Composable BoxScope.() -> Unit,
    rightContent: @Composable BoxScope.() -> Unit,
    initialRatio: Float = 0.3f,
    modifier: Modifier = Modifier,
) {
    var dividerPosition by remember { mutableStateOf(initialRatio) }
    var isDragging by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dividerPosition = (dividerPosition + dragAmount.x / size.width)
                            .coerceIn(0.1f, 0.9f)
                    }
                )
            }
    ) {
        // Left pane
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(dividerPosition)
        ) {
            leftContent()
        }

        // Divider
        Row {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .background(MaterialTheme.colorScheme.background)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .background(if (isDragging) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Right pane
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - dividerPosition)
        ) {
            rightContent()
        }
    }
}