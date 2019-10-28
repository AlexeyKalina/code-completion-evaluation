package org.jb.cce

import org.jb.cce.storages.ActionsStorage
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.LogsStorage
import org.jb.cce.storages.SessionsStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace(outputDir: String, evaluationType: String, existing: Boolean = false) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }

    private val basePath: Path = Paths.get(outputDir).toAbsolutePath()
        .let { if (existing) it else it.resolve(formatter.format(Date())) }

    private val sessionsDir = basePath.resolve("data")
    private val logsDir = basePath.resolve("logs")
    private val actionsDir = basePath.resolve("actions")
    private val errorsDir = basePath.resolve("errors")
    private val reportsDir = basePath.resolve("reports")

    init {
        Files.createDirectories(sessionsDir)
        Files.createDirectories(logsDir)
        Files.createDirectories(actionsDir)
        Files.createDirectories(errorsDir)
        Files.createDirectories(reportsDir)
    }

    fun reportsDirectory() = reportsDir.toString()

    fun path(): Path = basePath

    val sessionsStorage = SessionsStorage(sessionsDir.toString(), evaluationType)

    val actionsStorage = ActionsStorage(actionsDir.toString())

    val errorsStorage = FileErrorsStorage(errorsDir.toString())

    val logsStorage = LogsStorage(logsDir.toString())

    override fun toString(): String = "Evaluation workspace: $basePath"
}