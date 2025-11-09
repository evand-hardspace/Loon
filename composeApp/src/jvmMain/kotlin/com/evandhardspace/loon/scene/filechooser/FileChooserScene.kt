package com.evandhardspace.loon.scene.filechooser

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evandhardspace.loon.coreutils.OnEffect
import com.evandhardspace.loon.dialog.AppDialog
import com.evandhardspace.loon.presentation.FileChooser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UNCHECKED_CAST")
@Composable
fun FileChooserScene(
    modifier: Modifier = Modifier,
    onDirectorySelected: (path: String) -> Unit,
) {
    val viewModel: FileChooserViewModel = viewModel { FileChooserViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    OnEffect(viewModel.effect) { effect ->
        when (effect) {
            is FileChooserEffect.FileSelected -> onDirectorySelected(effect.selectedFilePath)
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragging = false

                val transferable = event.awtTransferable
                val path = when {
                    transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        files.firstOrNull()?.absolutePath
                    }

                    transferable.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                        transferable.getTransferData(DataFlavor.stringFlavor) as? String
                    }

                    else -> null
                }
                val isDirectory = path?.let { File(it) }?.isDirectory == true

                path?.let {
                    selectedPath = it
                    if (isDirectory) {
                        FileChooserAction.ChoseFile(Result.success(it)).let(viewModel::onAction)
                        showSuccess = true
                    } else {
                        FileChooserAction.ChoseFile(Result.failure(UnsupportedOperationException("Not a directory")))
                            .let(viewModel::onAction)
                    }
                    coroutineScope.launch {
                        delay(2000)
                        showSuccess = false
                    }
                }

                return path != null
            }
        }
    }

    FileChooserContent(
        modifier = modifier,
        state = state,
        perform = viewModel::onAction,
        isDragging = isDragging,
        selectedPath = selectedPath,
        showSuccess = showSuccess,
        dragAndDropTarget = dragAndDropTarget,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FileChooserContent(
    state: FileChooserState,
    perform: FileChooserAction.() -> Unit,
    isDragging: Boolean,
    selectedPath: String?,
    showSuccess: Boolean,
    dragAndDropTarget: DragAndDropTarget,
    modifier: Modifier = Modifier,
) {
    val error = state.error

    if (error != null) {
        AppDialog(
            onDismissRequest = { FileChooserAction.DismissError.perform() },
            onSubmitAction = { FileChooserAction.DismissError.perform() },
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(error)
                    Button(onClick = { FileChooserAction.DismissError.perform() }) {
                        Text("OK")
                    }
                }
            }
        }
    }

    Surface(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Animated gradient background
            val infiniteTransition = rememberInfiniteTransition(label = "gradient")
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset"
            )

            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                // Drop Zone
                Box(
                    modifier = Modifier
                        .size(400.dp, 300.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDragging) {
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    )
                                } else if (showSuccess) {
                                    listOf(
                                        Color(0xFF4CAF50).copy(alpha = 0.2f),
                                        Color(0xFF8BC34A).copy(alpha = 0.2f),
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    )
                                }
                            )
                        )
                        .border(
                            border = BorderStroke(
                                width = if (isDragging) 3.dp else 2.dp,
                                color = if (isDragging) {
                                    MaterialTheme.colorScheme.primary
                                } else if (showSuccess) {
                                    Color(0xFF4CAF50)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                },
                            ),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { true },
                            target = dragAndDropTarget,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {

                        Icon(
                            imageVector = when {
                                showSuccess -> Icons.Default.CheckCircle
                                isDragging -> Icons.Default.CloudUpload
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp),
                            tint = when {
                                showSuccess -> Color(0xFF4CAF50)
                                isDragging -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )

                        Text(
                            text = when {
                                showSuccess -> "Directory Selected!"
                                isDragging -> "Drop Here"
                                else -> "Drag & Drop Directory"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        AnimatedVisibility(
                            visible = selectedPath != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            selectedPath?.let { path ->
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }

                        if (!isDragging && !showSuccess) {
                            Text(
                                text = "or",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isDragging,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Button(
                        onClick = {
                            FileChooser()
                                .selectDirectory()
                                .let { FileChooserAction.ChoseFile(it) }
                                .perform()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 200.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Browse Directory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}