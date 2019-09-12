package org.jb.cce

import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace(outputDir: String, evaluationType: String, existing: Boolean = false) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }
    private val baseDir: String = if (existing) outputDir else Paths.get(outputDir, formatter.format(Date())).toString()

    private val resourcesDir = Paths.get(baseDir, "res")
    private val sessionsDir = Paths.get(baseDir, "data")
    private val logsDir = Paths.get(baseDir, "logs")
    private val actionsDir = Paths.get(baseDir, "actions")
    private val errorsDir = Paths.get(baseDir, "errors")
    private val reportsDir = Paths.get(baseDir, "reports")

    init {
        Files.createDirectories(resourcesDir)
        Files.createDirectories(sessionsDir)
        Files.createDirectories(logsDir)
        Files.createDirectories(actionsDir)
        Files.createDirectories(errorsDir)
        Files.createDirectories(reportsDir)
    }

    fun baseDirectory() = baseDir

    fun logsDirectory() = logsDir.toString()

    fun reportsDirectory() = reportsDir.toString()

    fun resourcesDirectory() = resourcesDir.toString()

    val sessionsStorage = SessionsStorage(sessionsDir.toString(), evaluationType)

    val actionsStorage = ActionsStorage(actionsDir.toString())

    val errorsStorage = FileErrorsStorage(errorsDir.toString())
}