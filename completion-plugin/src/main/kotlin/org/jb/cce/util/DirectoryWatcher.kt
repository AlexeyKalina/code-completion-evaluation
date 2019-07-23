package org.jb.cce.util

import com.intellij.util.io.exists
import com.intellij.util.io.move
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DirectoryWatcher(private val logsDir: String, private val outputDir: String) {
    private val executor = Executors.newSingleThreadExecutor()
    private val watcher: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKey: WatchKey
    private var logsCounter = 0

    init {
        this.watchKey = Paths.get(logsDir).register(watcher, ENTRY_CREATE)
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
    }
}
