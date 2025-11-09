package com.evandhardspace.loon

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    registerSlices() // TODO manage lifecycle
    Window(
        onCloseRequest = ::exitApplication,
        title = "Loon",
    ) {
        App()
    }
}