package org.jb.cce.util

import com.intellij.util.io.createDirectories
import com.intellij.util.io.createFile
import com.intellij.util.io.exists
import com.intellij.util.io.move
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate
import kotlin.streams.toList

class DirectoryWatcher(private val logsDir: String, private val outputDir: String, private val trainingPercentage: Int) {
    private val executor = Executors.newSingleThreadExecutor()
    private val watcher: WatchService = FileSystems.getDefault().newWatchService()
    private val formatter = SimpleDateFormat("dd_MM_yyyy")
    private val watchKey: WatchKey
    private var logsCounter = 0

    init {
        this.watchKey = Paths.get(logsDir).register(watcher, ENTRY_CREATE)
        Paths.get(this.outputDir).createDirectories()
    }

    fun start() {
        executor.submit(object : Runnable {
            override fun run() {
                try {
                    while (true) {
                        val key = watcher.take()

                        for (event in key.pollEvents()) {
                            val name = event.context()
                            val log = Paths.get(logsDir, name.toString())
                            if (log.exists()) {
                                log.move(Paths.get(outputDir, "$logsCounter.log"))
                                logsCounter++
                            }
                        }

                        if (!key.reset()) {
                            break
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
        val firstLogsFile = Paths.get(outputDir, "0.log")
        if (!firstLogsFile.exists()) return

        val firstLine = FileReader(firstLogsFile.toFile()).use { it.readLines().first() }
        val baseDir = Paths.get(outputDir, formatter.format(Date())).createDirectories()
        val userId = firstLine.split("\t")[3]
        val trainingLogsWriter = getLogsWriter(baseDir, "training", userId)
        val testLogsWriter = getLogsWriter(baseDir, "test", userId)

        val files = Files.find(Paths.get(outputDir), 1, BiPredicate { _: Path, attrs: BasicFileAttributes -> !attrs.isDirectory })
                .map(Path::toFile)
                .toList()

        val threshold = files.count() * (trainingPercentage.toDouble() / 100.0)
        appendLogs(files, files.indices.filter { it < threshold }, trainingLogsWriter)
        appendLogs(files, files.indices.filter { it >= threshold }, testLogsWriter)
    }

    private fun getLogsWriter(baseDir: Path, datasetType: String, userId: String): BufferedWriter {
        val logsFile = Paths.get(baseDir.toString(), datasetType, userId)
        logsFile.createFile()
        return logsFile.toFile().bufferedWriter()
    }

    private fun appendLogs(files: List<File>, indices: List<Int>, writer: BufferedWriter) {
        writer.use {
            for (i in indices) {
                it.append(files[i].readText())
                files[i].delete()
            }
        }
    }
}
