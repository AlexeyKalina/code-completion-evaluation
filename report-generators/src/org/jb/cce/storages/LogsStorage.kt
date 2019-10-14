package org.jb.cce.storages

import java.nio.file.Paths

class LogsStorage(storageDir: String) : EvaluationStorage(storageDir) {
    private lateinit var watcher: DirectoryWatcher

    fun watch(logsPath: String, languageName: String, trainingPercentage: Int) {
        val outputDir = Paths.get(storageDir, languageName).toString()
        watcher = DirectoryWatcher(logsPath, outputDir, trainingPercentage)
        watcher.start()
    }

    fun stopWatching() {
        watcher.stop()
        compress()
    }
}