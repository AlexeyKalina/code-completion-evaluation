package org.jb.cce

import com.google.gson.Gson
import org.jb.cce.storages.ActionsStorage
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.LogsStorage
import org.jb.cce.storages.SessionsStorage
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace(outputDir: String, existing: Boolean = false) {
    companion object {
        private const val statsFile = "stats.json"
        private val gson = Gson()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }

    private val basePath: Path = Paths.get(outputDir).toAbsolutePath()
            .let { if (existing) it else it.resolve(formatter.format(Date())) }

    private val sessionsDir = subdir("data")
    private val logsDir = subdir("logs")
    private val actionsDir = subdir("actions")
    private val errorsDir = subdir("errors")
    private val reportsDir = subdir("reports")

    fun reportsDirectory(): String = reportsDir.toString()

    fun path(): Path = basePath

    val sessionsStorage: SessionsStorage = SessionsStorage(sessionsDir.toString())

    val actionsStorage: ActionsStorage = ActionsStorage(actionsDir.toString())

    val errorsStorage: FileErrorsStorage = FileErrorsStorage(errorsDir.toString())

    val logsStorage: LogsStorage = LogsStorage(logsDir.toString())

    override fun toString(): String = "Evaluation workspace: $basePath"

    fun dumpStatistics(stats: Map<String, Long>) =
            FileWriter(basePath.resolve(statsFile).toString()).use { it.write(gson.toJson(stats)) }

    private fun subdir(name: String): Path {
        val directory = basePath.resolve(name)
        Files.createDirectories(directory)
        return directory
    }
}