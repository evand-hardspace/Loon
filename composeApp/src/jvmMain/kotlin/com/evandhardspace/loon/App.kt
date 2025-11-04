package com.evandhardspace.loon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.evandhardspace.loon.features.scene.filechooser.FileChooserScene
import com.evandhardspace.loon.features.scene.editor.EditorScene
import com.evandhardspace.loon.coreutils.ui.LoonTheme

@Composable
fun App() {
    LoonTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = FileChooserRoute,
        ) {
            composable<FileChooserRoute> {
                FileChooserScene(
                    modifier = Modifier.fillMaxSize(),
                    onDirectorySelected = { path ->
                        navController.navigate(
                            route = EditorRoute(path),
                        )
                    }
                )
            }
            composable<EditorRoute> {
                EditorScene(
                    modifier = Modifier.fillMaxSize(),
                    selectedPath = it.toRoute<EditorRoute>().selectedPath,
                    onBack = navController::popBackStack,
                )
            }
        }
    }
}