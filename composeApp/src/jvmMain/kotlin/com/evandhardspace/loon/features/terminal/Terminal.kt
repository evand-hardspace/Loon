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
import androidx.compose.ui.text.font.FontStyle
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
    var output by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var currentPrompt by remember { mutableStateOf("") }
    var currentDirectory by remember { mutableStateOf(startDirectory) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }

    var process: PtyProcess? by remember { mutableStateOf(null) }
    var writer: OutputStreamWriter? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    handleKeyEvent<AppKeyEvent.TabMenu>("terminal") {
        isExpanded = !isExpanded
        onToggleVisibility(isExpanded)
        true
    }

    // Auto-scroll to bottom when output changes
    LaunchedEffect(output) {
        verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
    }

    // Initialize PTY process
    LaunchedEffect(refreshTrigger) {
        // Clean up previous process if exists
        process?.destroy()
        output = ""
        input = ""
        currentDirectory = startDirectory

        withContext(Dispatchers.IO) {
            val shell = if (System.getProperty("os.name").contains("Windows")) {
                arrayOf("cmd.exe")
            } else {
                arrayOf("/bin/bash", "-i")
            }

            val env = System.getenv().toMutableMap()
            env["TERM"] = "dumb"  // Use dumb terminal to avoid escape sequences

            // Set custom prompt that matches the input field display
            // Use \u for username, \w for path with ~ substitution
            val username = System.getProperty("user.name")
            env["PS1"] = "\\u:\\w\\$ "
            env["PS2"] = "> "  // Continuation prompt for multiline commands

            // Disable bash history and other features
            env["HISTFILE"] = ""
            env["BASH_SILENCE_DEPRECATION_WARNING"] = "1"

            val ptyProcess = PtyProcessBuilder()
                .setCommand(shell)
                .setEnvironment(env)
                .setDirectory(startDirectory)
                .start()

            process = ptyProcess
            writer = OutputStreamWriter(ptyProcess.outputStream)

            val reader = InputStreamReader(ptyProcess.inputStream)
            val buffer = CharArray(1024)

            try {
                while (true) {
                    val charsRead = reader.read(buffer)
                    if (charsRead == -1) break

                    var text = String(buffer, 0, charsRead)

                    // Filter out ANSI escape sequences
                    text = text.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "")
                    text = text.replace(Regex("\u001B\\][0-9;]*.*?\u0007"), "")
                    text = text.replace(Regex("\\[\\?[0-9]+[a-z]"), "")

                    withContext(Dispatchers.Main) {
                        // Extract current directory from prompt
                        // Look for patterns like "username:path$ " to update currentDirectory
                        val promptPattern = Regex("([^:]+):([^$]+)\\$")
                        val match = promptPattern.find(text)
                        if (match != null) {
                            val path = match.groupValues[2]
                            val homeDir = System.getProperty("user.home")
                            currentDirectory = if (path.startsWith("~")) {
                                homeDir + path.substring(1)
                            } else {
                                path
                            }
                        }

                        output += text
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    output += "\n[Process terminated: ${e.message}]"
                }
            }
        }

        // Request focus for input
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            process?.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Header with refresh button
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
                    onClick = {
                        refreshTrigger++
                    },
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
                        contentDescription = if (isExpanded) "Collapse Terminal" else "Expand Terminal",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Terminal output area
        if (isExpanded.not()) return
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
                    // Remove trailing prompt from display
                    val displayOutput = remember(output) {
                        // Remove the last prompt if it's there
                        val promptPattern = """[\w.]+:[^\$]+\$\s*$""".toRegex()
                        output.replace(promptPattern, "")
                    }

                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = displayOutput,
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

        // Input field with prompt and cursor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Display username and current directory
            val username = System.getProperty("user.name")
            val homeDir = System.getProperty("user.home")
            val displayPath = if (currentDirectory.startsWith(homeDir)) {
                "~" + currentDirectory.substring(homeDir.length)
            } else {
                currentDirectory
            }

            Text(
                text = "$username:$displayPath\$ ",
                color = Color(0xFF2C83AA),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            BasicTextField(
                value = input,
                onValueChange = { newValue ->
                    // Check if newline was entered (command submitted)
                    if (newValue.contains("\n") || newValue.contains("\r")) {
                        val command = input.trim()
                        // Only send non-empty commands
                        if (command.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                writer?.write(command + "\n")
                                writer?.flush()
                            }
                        }
                        input = ""
                    } else {
                        input = newValue
                    }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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