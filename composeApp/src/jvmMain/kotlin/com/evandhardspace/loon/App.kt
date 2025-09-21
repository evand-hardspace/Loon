package com.evandhardspace.loon

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import javax.swing.JFileChooser

@Composable
fun App() {
    var selectedDirectory by remember { mutableStateOf<String?>(null) }

    Column {
        Button(onClick = {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            }
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = chooser.selectedFile.absolutePath
            }
        }) {
            Text("Select Directory")
        }

        selectedDirectory?.let {
            Text("Selected directory: $it")
        }
    }
}