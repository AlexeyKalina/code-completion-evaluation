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

        val lines = FileReader(fullLogsFile.toFile()).use { it.readLines() }
        if (lines.isEmpty()) return
        val userId = getUserId(lines.first())
        val trainingLogsWriter = getLogsWriter("train", userId)
        val validateLogsWriter = getLogsWriter("validate", userId)

        val sessions = lines.groupBy { getSessionId(it) }
        val threshold = (sessions.count() * (trainingPercentage.toDouble() / 100.0)).toInt()
        val trainingSessions = mutableListOf<String>()
        val validateSessions = mutableListOf<String>()
        var counter = 0
        for (session in sessions) {
            if (counter < threshold) trainingSessions.addAll(session.value)
            else validateSessions.addAll(session.value)
            counter++
        }
        appendLogs(trainingSessions, trainingLogsWriter)
        appendLogs(validateSessions, validateLogsWriter)
        saveSessionsInfo(sessions.size, threshold)
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

    private fun appendLogs(lines: List<String>, writer: BufferedWriter) {
        writer.use { for (line in lines) it.appendln(line) }
    }

    private fun getUserId(line: String) = line.split("\t")[3]
    private fun getSessionId(line: String) = line.split("\t")[4]
}