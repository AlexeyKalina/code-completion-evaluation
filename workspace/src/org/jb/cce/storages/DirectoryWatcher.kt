package org.jb.cce.storages

import java.io.BufferedWriter
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
    private val sessionIds = linkedSetOf<String>()

    init {
        this.watchKey = Paths.get(logsDir).register(watcher, ENTRY_CREATE)
        Files.createDirectories(Paths.get(this.outputDir))
    }

    fun start() {
        executor.submit(object : Runnable {
            override fun run() {
                try {
                    FileWriter(Paths.get(outputDir, "full.log").toString()).use { writer ->
                        while (true) {
                            val key = watcher.take()

                            for (logChunk in key.pollEvents()
                                    .map { it.context().toString() }
                                    .sortedBy { it.substringAfterLast('_').toInt() }) {
                                Paths.get(logsDir, logChunk).run {
                                    if (Files.exists(this)) {
                                        with(this.toFile().readText()) {
                                            sessionIds.addAll(split("\n").filter { it.isNotBlank() }.map { getSessionId(it) })
                                            writer.append(this)
                                        }
                                        Files.delete(this)
                                    }
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
        if (!Files.exists(fullLogsFile)) return
        val trainSize = (sessionIds.size * trainingPercentage.toDouble() / 100.0).toInt()
        val trainSessionIds = sessionIds.take(trainSize).toSet()

        fullLogsFile.toFile().bufferedReader(bufferSize = 1024 * 1024).use {
            val firstLine = it.readLine() ?: return
            val userId = getUserId(firstLine)
            val trainingLogsWriter = getLogsWriter("train", userId)
            val validateLogsWriter = getLogsWriter("validate", userId)
            trainingLogsWriter.appendln(firstLine)

            for (line in it.lines())
                (if (trainSessionIds.contains(getSessionId(line)))
                    trainingLogsWriter else validateLogsWriter).appendln(line)
        }
        saveSessionsInfo(sessionIds.size, trainSize)
        fullLogsFile.toFile().delete()
    }

    private fun getLogsWriter(datasetType: String, userId: String): BufferedWriter {
        val resultDir = Paths.get(outputDir, datasetType, formatter.format(Date()))
        Files.createDirectories(resultDir)
        val logsFile = Paths.get(resultDir.toString(), userId)
        Files.createFile(logsFile)
        return logsFile.toFile().bufferedWriter()
    }

    private fun saveSessionsInfo(all: Int, training: Int) {
        val infoFile = Paths.get(outputDir, "info")
        infoFile.toFile().writeText("All sessions: $all\nTraining sessions: $training\nValidate sessions: ${all - training}")
    }

    private fun getUserId(line: String) = line.split("\t")[3]
    private fun getSessionId(line: String) = line.split("\t")[4]
}