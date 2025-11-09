package com.evandhardspace.loon.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.forceHandleKeyEvent

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    onSubmitAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    forceHandleKeyEvent<AppKeyEvent.Enter>("appDialog") {
        onSubmitAction()
        true
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}