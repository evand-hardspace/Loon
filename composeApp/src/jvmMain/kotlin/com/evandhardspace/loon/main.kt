package com.evandhardspace.loon

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.evandhardspace.loon.keyhandler.KeyEventHandler
import com.evandhardspace.loon.keyhandler.LocalKeyEventHandler

fun main() = application {
    LaunchedEffect(Unit) {
        registerSlices() // TODO manage lifecycle
    }
    val keyEventHandler = remember { KeyEventHandler() }
    CompositionLocalProvider(
        LocalKeyEventHandler provides keyEventHandler,
    ) {
        Window(
            onCloseRequest = ::exitApplication,
            onPreviewKeyEvent = keyEventHandler::onKeyEvent,
            title = "Loon",
        ) {
            App()
        }
    }
}