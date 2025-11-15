package com.evandhardspace.loon.features.terminal

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evandhardspace.loon.keyhandler.AppKeyEvent
import com.evandhardspace.loon.keyhandler.handleKeyEvent
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
fun Terminal(
    modifier: Modifier = Modifier,
    startDirectory: String,
    onToggleVisibility: (isExpanded: Boolean) -> Unit,
) {
    // Terminal state
    var terminalHistory by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var currentDirectory by remember { mutableStateOf(startDirectory) }
    var isExpanded by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // PTY process references
    var process: PtyProcess? by remember { mutableStateOf(null) }
    var writer: OutputStreamWriter? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    // User info
    val username = remember { System.getProperty("user.name") }
    val homeDir = remember { System.getProperty("user.home") }

    // Display path with ~ substitution
    val displayPath = remember(currentDirectory, homeDir) {
        if (currentDirectory.startsWith(homeDir)) {
            "~" + currentDirectory.substring(homeDir.length)
        } else {
            currentDirectory
        }
    }

    // Keyboard shortcut handler
    handleKeyEvent<AppKeyEvent.ToggleTerminal>("terminal") {
        isExpanded = !isExpanded
        onToggleVisibility(isExpanded)
        true
    }

    // Auto-scroll to bottom when history changes
    LaunchedEffect(terminalHistory) {
        verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
    }

    // Initialize and manage PTY process
    LaunchedEffect(refreshTrigger) {
        // Cleanup previous process
        process?.destroy()
        terminalHistory = ""
        currentInput = ""
        currentDirectory = startDirectory

        withContext(Dispatchers.IO) {
            // Determine shell based on OS
            val shell = if (System.getProperty("os.name").contains("Windows")) {
                arrayOf("cmd.exe")
            } else {
                arrayOf("/bin/bash", "-i")
            }

            // Configure environment
            val env = System.getenv().toMutableMap()
            env["TERM"] = "dumb"
            env["PS1"] = ""  // Disable shell prompt - we'll show our own
            env["PS2"] = ""
            env["HISTFILE"] = ""
            env["BASH_SILENCE_DEPRECATION_WARNING"] = "1"

            // Start PTY process
            val ptyProcess = PtyProcessBuilder()
                .setCommand(shell)
                .setEnvironment(env)
                .setDirectory(startDirectory)
                .start()

            process = ptyProcess
            writer = OutputStreamWriter(ptyProcess.outputStream)

            writer?.write("stty -echo\n")
            writer?.flush()

            // Read output in background
            val reader = InputStreamReader(ptyProcess.inputStream)
            val buffer = CharArray(8192)

            try {
                while (true) {
                    val charsRead = reader.read(buffer)
                    if (charsRead == -1) break

                    var text = String(buffer, 0, charsRead)

                    // Remove ANSI escape sequences
                    text = text.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "")
                    text = text.replace(Regex("\u001B\\][0-9;]*.*?\u0007"), "")
                    text = text.replace(Regex("\\[\\?[0-9]+[a-z]"), "")

                    withContext(Dispatchers.Main) {
                        terminalHistory += text
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalHistory += "\n[Process terminated: ${e.message}]\n"
                }
            }
        }

        // Focus input field
        focusRequester.requestFocus()
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            process?.destroy()
        }
    }

    // Command execution handler
    fun executeCommand(command: String) {
        if (command.isBlank()) return

        // Add command to history with prompt
        terminalHistory += "$username:$displayPath\$ $command\n"

        // Handle built-in commands
        when {
            command.trim().startsWith("cd ") -> {
                val newPath = command.trim().substring(3).trim()
                val targetDir = when {
                    newPath == "~" -> homeDir
                    newPath.startsWith("~/") -> homeDir + newPath.substring(1)
                    newPath.startsWith("/") -> newPath
                    else -> "$currentDirectory/$newPath"
                }

                // Normalize path
                val file = java.io.File(targetDir)
                if (file.exists() && file.isDirectory) {
                    currentDirectory = file.canonicalPath
                    // Send cd command to shell as well
                    scope.launch(Dispatchers.IO) {
                        writer?.write("cd \"$targetDir\"\n")
                        writer?.flush()
                    }
                } else {
                    terminalHistory += "cd: no such file or directory: $newPath\n"
                }
            }
            command.trim() == "clear" -> {
                terminalHistory = ""
            }
            else -> {
                // Send command to PTY process
                scope.launch(Dispatchers.IO) {
                    writer?.write("$command\n")
                    writer?.flush()
                }
            }
        }

        currentInput = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252526))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terminal",
                color = Color(0xFFCCCCCC),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Row {
                IconButton(
                    onClick = { refreshTrigger++ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Terminal",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isExpanded = !isExpanded
                        onToggleVisibility(isExpanded)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (!isExpanded) return

        // Terminal output area with scrollbars
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if(terminalHistory.startsWith("stty -echo") && terminalHistory.length >= 24) {
                                terminalHistory.substring(24)
                            } else terminalHistory, // fixme
                            color = Color(0xFFCCCCCC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(verticalScrollState),
                style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    hoverColor = MaterialTheme.colorScheme.onBackground,
                    thickness = 6.dp,
                ),
            )

            HorizontalScrollbar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                adapter = rememberScrollbarAdapter(horizontalScrollState),
                style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    hoverColor = MaterialTheme.colorScheme.onBackground,
                    thickness = 6.dp,
                ),
            )
        }

        // Input area with prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prompt: username:path$
            Text(
                text = "$username:$displayPath\$ ",
                color = Color(0xFF2C83AA),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            // Input field
            BasicTextField(
                value = currentInput,
                onValueChange = { newValue ->
                    // Handle Enter key
                    if (newValue.contains("\n") || newValue.contains("\r")) {
                        executeCommand(currentInput)
                    } else {
                        currentInput = newValue
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFFCCCCCC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color(0xFF2C83AA)),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
}