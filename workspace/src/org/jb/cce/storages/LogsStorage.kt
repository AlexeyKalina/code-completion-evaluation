package org.jb.cce.storages

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class LogsStorage(private val storageDir: String) {
    private val formatter = SimpleDateFormat("dd_MM_yyyy")
    private val sessionIds = linkedSetOf<String>()

    val path = storageDir

    fun save(logsPath: String, languageName: String, trainingPercentage: Int) {
        val outputDir = Paths.get(storageDir, languageName)
        Files.createDirectories(outputDir)
        FileWriter(Paths.get(outputDir.toString(), "full.log").toString()).use { writer ->
            for (logChunk in File(logsPath).listFiles()
                    .map { it.name.toString() }
                    .sortedBy { it.substringAfterLast('_').toInt() }) {
                Paths.get(logsPath, logChunk).run {
                    if (Files.exists(this)) {
                        with(this.toFile().readText()) {
                            sessionIds.addAll(split("\n").filter { it.isNotBlank() }.map { getSessionId(it) })
                            writer.append(this)
                        }
                        Files.delete(this)
                    }
                }
            }
        }
        saveLogs(outputDir.toString(), trainingPercentage)
        File(storageDir).compress()
    }

    private fun saveLogs(outputDir: String, trainingPercentage: Int) {
        val fullLogsFile = Paths.get(outputDir, "full.log")
        if (!Files.exists(fullLogsFile)) return
        val trainSize = (sessionIds.size * trainingPercentage.toDouble() / 100.0).toInt()
        val trainSessionIds = sessionIds.take(trainSize).toSet()

        fullLogsFile.toFile().bufferedReader(bufferSize = 1024 * 1024).use {
            val firstLine = it.readLine() ?: return
            val userId = getUserId(firstLine)
            val trainingLogsWriter = getLogsWriter(outputDir, "train", userId)
            val validateLogsWriter = getLogsWriter(outputDir, "validate", userId)
            trainingLogsWriter.appendln(firstLine)

            for (line in it.lines())
                (if (trainSessionIds.contains(getSessionId(line)))
                    trainingLogsWriter else validateLogsWriter).appendln(line)
        }
        saveSessionsInfo(outputDir, sessionIds.size, trainSize)
        fullLogsFile.toFile().delete()
    }

    private fun getLogsWriter(outputDir: String, datasetType: String, userId: String): BufferedWriter {
        val resultDir = Paths.get(outputDir, datasetType, formatter.format(Date()))
        Files.createDirectories(resultDir)
        val logsFile = Paths.get(resultDir.toString(), userId)
        Files.createFile(logsFile)
        return logsFile.toFile().bufferedWriter()
    }

    private fun saveSessionsInfo(outputDir: String, all: Int, training: Int) {
        val infoFile = Paths.get(outputDir, "info")
        infoFile.toFile().writeText("All sessions: $all\nTraining sessions: $training\nValidate sessions: ${all - training}")
    }

    private fun getUserId(line: String) = line.split("\t")[3]
    private fun getSessionId(line: String) = line.split("\t")[4]
}