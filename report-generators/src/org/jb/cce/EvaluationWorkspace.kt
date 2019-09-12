package org.jb.cce

import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricsEvaluator
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace(outputDir: String) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }
    private val baseDir: String = Paths.get(outputDir, formatter.format(Date())).toString()

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

    data class SessionsInfo(val path: String, val sessionsPath: String, val evaluationType: String)
    private val sessionFiles: MutableMap<String, MutableList<SessionsInfo>> = mutableMapOf()

    private val reportGenerator = HtmlReportGenerator(baseDir, reportsDir.toString(), resourcesDir.toString())

    fun logsDirectory() = logsDir.toString()

    fun sessionsDirectory() = sessionsDir.toString()

    fun actionsDirectory() = actionsDir.toString()

    fun errorsDirectory() = errorsDir.toString()

    fun generateReport(sessionsStorage: List<SessionsStorage>, errorsStorage: FileErrorsStorage?): String {
        val evaluationType2storage = mutableMapOf<String, SessionsStorage>()
        val evaluationType2evaluator = mutableMapOf<String, MetricsEvaluator>()
        for (storage in sessionsStorage) {
            evaluationType2storage[storage.evaluationType] = storage
            evaluationType2evaluator[storage.evaluationType] = MetricsEvaluator.withDefaultMetrics(storage.evaluationType)
            for (pathsPair in storage.getSessionFiles()) {
                val sessionFile = sessionFiles.getOrPut(pathsPair.first) { mutableListOf() }
                sessionFile.add(SessionsInfo(pathsPair.first, pathsPair.second, storage.evaluationType))
            }
        }
        for (sessionFile in sessionFiles) {
            val fileEvaluations = mutableListOf<FileEvaluationInfo>()
            for (file in sessionFile.value) {
                val sessionsEvaluation = evaluationType2storage[file.evaluationType]!!.getSessions(file.path)
                val metricsEvaluation = evaluationType2evaluator[file.evaluationType]!!.evaluate(sessionsEvaluation.sessions)
                fileEvaluations.add(FileEvaluationInfo(sessionsEvaluation, metricsEvaluation, file.evaluationType))
            }
            reportGenerator.generateFileReport(fileEvaluations)
        }
        reportGenerator.generateErrorReports(errorsStorage?.getErrors() ?: emptyList())
        return reportGenerator.generateGlobalReport(evaluationType2evaluator.values.map { it.result() }.flatten())
    }
}