package org.jb.cce.storages

import java.io.File
import java.nio.file.Paths

class LogsStorage(private val storageDir: String) {
    private lateinit var watcher: DirectoryWatcher

    fun watch(logsPath: String, languageName: String, trainingPercentage: Int) {
        val outputDir = Paths.get(storageDir, languageName).toString()
        watcher = DirectoryWatcher(logsPath, outputDir, trainingPercentage)
        watcher.start()
    }

    fun stopWatching() {
        watcher.stop()
        File(storageDir).compress()
    }
}