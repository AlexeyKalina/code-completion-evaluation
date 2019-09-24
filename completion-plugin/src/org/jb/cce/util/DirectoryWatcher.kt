package org.jb.cce.util

import com.intellij.util.io.*
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DirectoryWatcher(private val logsDir: String, private val outputDir: String, private val trainingPercentage: Int) {
    private val executor = Executors.newSingleThreadExecutor()
    private val watcher: WatchService = FileSystems.getDefault().newWatchService()
    private val formatter = SimpleDateFormat("dd_MM_yyyy")
    private val watchKey: WatchKey

    init {
        this.watchKey = Paths.get(logsDir).register(watcher, ENTRY_CREATE)
        Paths.get(this.outputDir).createDirectories()
    }

    fun start() {
        executor.submit(object : Runnable {
            override fun run() {
                try {
                    FileWriter(Paths.get(outputDir, "full.log").toString()).use {
                        while (true) {
                            val key = watcher.take()

                            for (event in key.pollEvents()) {
                                val name = event.context()
                                val log = Paths.get(logsDir, name.toString())
                                if (log.exists()) {
                                    it.append(log.readText())
                                    log.delete()
                                }
                            }

                            if (!key.reset()) {
                                break
                            }
                        }
                    }
                } catch (x: InterruptedException) {
                    return
                }
            }
        })
        executor.shutdown()
    }

    fun stop() {
        executor.awaitTermination(1, TimeUnit.SECONDS)
        executor.shutdownNow()
        saveLogs()
    }

    private fun saveLogs() {
        val fullLogsFile = Paths.get(outputDir, "full.log")
        if (!fullLogsFile.exists()) return

        val logsReader = fullLogsFile.toFile().bufferedReader(bufferSize = 64 * 1024 * 1024)
        val userId = getUserId(logsReader.readLine() ?: return)
        val trainingLogsWriter = getLogsWriter("train", userId)
        val validateLogsWriter = getLogsWriter("validate", userId)

        val split = mutableMapOf<String, Boolean>()
        for (session in logsReader.lines()) {
            val sessionId = getSessionId(session)
            if (sessionId !in split) split[sessionId] = (0..99).random() < trainingPercentage
            (if (split[sessionId]!!) trainingLogsWriter else validateLogsWriter).appendln(session)
        }
        logsReader.close()
        saveSessionsInfo(split.size, split.count { it.value })
        fullLogsFile.delete()
    }

    private fun getLogsWriter(datasetType: String, userId: String): BufferedWriter {
        val logsFile = Paths.get(outputDir, datasetType, formatter.format(Date()), userId)
        logsFile.createFile()
        return logsFile.toFile().bufferedWriter()
    }

    private fun saveSessionsInfo(all: Int, training: Int) {
        val infoFile = Paths.get(outputDir, "info")
        infoFile.toFile().writeText("All sessions: $all\nTraining sessions: $training\nValidate sessions: ${all - training}")
    }

    private fun getUserId(line: String) = line.split("\t")[3]
    private fun getSessionId(line: String) = line.split("\t")[4]
}