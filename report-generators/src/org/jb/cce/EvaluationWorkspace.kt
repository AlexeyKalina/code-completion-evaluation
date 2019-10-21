package org.jb.cce

import org.jb.cce.storages.ActionsStorage
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.LogsStorage
import org.jb.cce.storages.SessionsStorage
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace(outputDir: String, evaluationType: String, existing: Boolean = false) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }
    private val baseDir: String = if (existing) outputDir else Paths.get(outputDir, formatter.format(Date())).toString()

    private val sessionsDir = Paths.get(baseDir, "data")
    private val logsDir = Paths.get(baseDir, "logs")
    private val actionsDir = Paths.get(baseDir, "actions")
    private val errorsDir = Paths.get(baseDir, "errors")
    private val reportsDir = Paths.get(baseDir, "reports")

    init {
        Files.createDirectories(sessionsDir)
        Files.createDirectories(logsDir)
        Files.createDirectories(actionsDir)
        Files.createDirectories(errorsDir)
        Files.createDirectories(reportsDir)
    }

    fun reportsDirectory() = reportsDir.toString()

    val sessionsStorage = SessionsStorage(sessionsDir.toString(), evaluationType)

    val actionsStorage = ActionsStorage(actionsDir.toString())

    val errorsStorage = FileErrorsStorage(errorsDir.toString())

    val logsStorage = LogsStorage(logsDir.toString())

    override fun toString(): String = baseDir
}