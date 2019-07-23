package org.jb.cce.util

import com.intellij.util.io.*
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate

class DirectoryWatcher(private val logsDir: String, private val outputDir: String) {
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
        val resultLogsFile = Paths.get(outputDir, formatter.format(Date()), firstLine.split("\t")[3])
        resultLogsFile.createFile()

        resultLogsFile.toFile().bufferedWriter().use { writer ->
            Files.find(Paths.get(outputDir), 1, { _, attrs -> !attrs.isDirectory }, emptyArray())
                    .map(Path::toFile)
                    .forEach {
                        writer.append(it.readText())
                        it.delete()
                    }
        }
    }
}
