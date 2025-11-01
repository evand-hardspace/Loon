package com.evandhardspace.loon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.evandhardspace.loon.chooser.FileChooserScreen
import com.evandhardspace.loon.editor.EditorScreen
import kotlinx.serialization.Serializable

@Serializable
data object FileChooserRoute

@Serializable
data class EditorRoute(val selectedPath: String)

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = FileChooserRoute,
    ) {
        composable<FileChooserRoute> {
            FileChooserScreen(
                modifier = Modifier.fillMaxSize(),
                onDirectorySelected = { path ->
                    println("navigate")
                    navController.navigate(EditorRoute(path))
                }
            )
        }
        composable<EditorRoute> {
            EditorScreen(
                selectedPath = it.toRoute<EditorRoute>().selectedPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}