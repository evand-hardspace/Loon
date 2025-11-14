package com.evandhardspace.loon.features.vcs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey

data class GitFileStatus(
    val added: Set<String> = emptySet(),
    val changed: Set<String> = emptySet(),
    val untracked: Set<String> = emptySet(),
    val modified: Set<String> = emptySet(),
    val removed: Set<String> = emptySet(),
    val missing: Set<String> = emptySet(),
    val conflicting: Set<String> = emptySet(),
    val isGitRepository: Boolean = true
)

// Repository for Git operations
class GitRepository(private val repoPath: String) {
    private var git: Git? = null
    private var repository: Repository? = null

    init {
        openRepository()
    }

    private fun openRepository() {
        try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) {
                println("No .git directory found at: ${gitDir.absolutePath}")
                return
            }

            repository = FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build()
            git = Git(repository)
            println("Git repository opened successfully")
        } catch (e: Exception) {
            println("Error opening repository: ${e.message}")
        }
    }

    fun isInitialized(): Boolean {
        return git != null && repository != null
    }

    fun reinitialize() {
        close()
        openRepository()
    }

    fun stageFile(relativePath: String) {
        try {
            git?.add()?.addFilepattern(relativePath)?.call()
            println("Added file to staging: $relativePath")
        } catch (e: Exception) {
            println("Error adding file: ${e.message}")
        }
    }

    fun unstageFile(relativePath: String) {
        try {
            git?.reset()?.addPath(relativePath)?.call()
            println("Removed file from staging: $relativePath")
        } catch (e: Exception) {
            println("Error removing file from staging: ${e.message}")
        }
    }

    fun getStatus(): GitFileStatus? {
        if (!isInitialized()) {
            return GitFileStatus(isGitRepository = false)
        }

        return try {
            val status: Status = git?.status()?.call() ?: return GitFileStatus(isGitRepository = false)

            GitFileStatus(
                added = status.added,
                changed = status.changed,
                untracked = status.untracked,
                modified = status.modified,
                removed = status.removed,
                missing = status.missing,
                conflicting = status.conflicting,
                isGitRepository = true
            )
        } catch (e: Exception) {
            println("Error getting status: ${e.message}")
            GitFileStatus(isGitRepository = false)
        }
    }

    fun close() {
        try {
            git?.close()
            repository?.close()
            git = null
            repository = null
            println("Git repository closed")
        } catch (e: Exception) {
            println("Error closing repository: ${e.message}")
        }
    }
}

// File system watcher for Git repository
class GitWatcher(
    val gitRepo: GitRepository,
    val repoPath: String,
) {

    fun watchGitChanges(): Flow<GitFileStatus?> = callbackFlow {
        val watchService = FileSystems.getDefault().newWatchService()
        val repoDir = Paths.get(repoPath)
        val gitDirPath = Paths.get(repoPath, ".git")

        var gitDirExists = Files.exists(gitDirPath)
        val registeredKeys = mutableMapOf<Path, WatchKey>()

        // Function to register a directory
        fun registerDirectory(path: Path) {
            try {
                if (Files.exists(path) && Files.isDirectory(path)) {
                    val key = path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )
                    registeredKeys[path] = key
                    println("Registered watcher for: $path")
                }
            } catch (e: Exception) {
                println("Could not register directory $path: ${e.message}")
            }
        }

        // Function to unregister all git-related directories
        fun unregisterGitDirectories() {
            registeredKeys.entries.removeIf { (path, key) ->
                if (path.startsWith(gitDirPath)) {
                    try {
                        key.cancel()
                        println("Unregistered watcher for: $path")
                    } catch (e: Exception) {
                        println("Error canceling key for $path: ${e.message}")
                    }
                    true
                } else {
                    false
                }
            }
        }

        // Function to register .git directory and subdirectories
        fun registerGitDirectories() {
            if (!Files.exists(gitDirPath)) {
                println(".git directory does not exist")
                return
            }

            // Register .git directory
            registerDirectory(gitDirPath)

            // Register subdirectories in .git
            val gitSubDirs = listOf("refs", "objects", "hooks", "info", "logs")
            gitSubDirs.forEach { subDir ->
                val subPath = gitDirPath.resolve(subDir)
                registerDirectory(subPath)
            }
        }

        // Always watch the repository root
        registerDirectory(repoDir)

        // Register git directories if they exist
        if (gitDirExists) {
            registerGitDirectories()
        }

        // Send initial status
        launch {
            trySend(gitRepo.getStatus())
        }

        // Polling coroutine to check for .git existence changes
        launch(Dispatchers.IO) {
            while (isActive) {
                delay(500) // Check every 500ms

                val currentlyExists = Files.exists(gitDirPath)

                if (currentlyExists != gitDirExists) {
                    println(".git existence changed: was=$gitDirExists, now=$currentlyExists")
                    gitDirExists = currentlyExists

                    if (currentlyExists) {
                        println(".git directory appeared - reinitializing")
                        delay(100) // Small delay for directory to be fully created
                        gitRepo.reinitialize()
                        registerGitDirectories()
                    } else {
                        println(".git directory disappeared")
                        gitRepo.close()
                        unregisterGitDirectories()
                    }

                    // Emit new status
                    trySend(gitRepo.getStatus())
                }
            }
        }

        // Watch for file changes
        launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val key = watchService.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS)

                    if (key == null) {
                        continue
                    }

                    for (event in key.pollEvents()) {
                        val kind = event.kind()

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue
                        }

                        @Suppress("UNCHECKED_CAST")
                        val ev = event as WatchEvent<Path>
                        val filename = ev.context()

                        println("File change detected: $kind - $filename")

                        // Emit new status on any change
                        trySend(gitRepo.getStatus())
                    }

                    val valid = key.reset()
                    if (!valid) {
                        println("Watch key no longer valid, removing from registered keys")
                        registeredKeys.entries.removeIf { it.value == key }
                    }
                }
            } catch (e: Exception) {
                println("Watch service error: ${e.message}")
                e.printStackTrace()
            }
        }

        awaitClose {
            registeredKeys.values.forEach { key ->
                try {
                    key.cancel()
                } catch (e: Exception) {
                    println("Error canceling key: ${e.message}")
                }
            }
            gitRepo.close()
            watchService.close()
            println("Git watcher closed")
        }
    }.flowOn(Dispatchers.IO)
}