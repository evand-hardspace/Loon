package com.evandhardspace.loon.presentation

import javax.swing.JFileChooser

interface FileChooser {

    fun selectDirectory(): Result<String>
}

fun FileChooser(): FileChooser = SwingFileChooser()

internal class SwingFileChooser(
    private val chooser: JFileChooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
    },
) : FileChooser {
    override fun selectDirectory(): Result<String> = runCatching {
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile.isDirectory) {
            chooser.selectedFile.absolutePath
        } else error("Not a directory")
    }
}
