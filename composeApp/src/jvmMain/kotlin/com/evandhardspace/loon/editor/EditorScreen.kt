package com.evandhardspace.loon.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evandhardspace.loon.utils.ui.SplitPane

@Composable
fun EditorScreen(
    selectedPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SplitPane(
        modifier = modifier,
        leftContent = {
            Column {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text("FileTree: $selectedPath")
            }
        },
        rightContent = {
            Text("Editor")
        }
    )
}

