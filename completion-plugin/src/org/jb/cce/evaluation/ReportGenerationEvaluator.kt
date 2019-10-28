package org.jb.cce.evaluation

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.SessionsStorage

class ReportGenerationEvaluator(private val reportGenerator: HtmlReportGenerator, project: Project, isHeadless: Boolean) : BaseEvaluator(project, isHeadless) {
    data class SessionsInfo(val path: String, val sessionsPath: String, val evaluationType: String)
    private val sessionFiles: MutableMap<String, MutableList<SessionsInfo>> = mutableMapOf()

    fun generateReportUnderProgress(sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>) {
        val task = object : Task.Backgroundable(project, "Report generation") {
            private var reportPath: String? = null

            override fun run(indicator: ProgressIndicator) {
                reportPath = generateReport(sessionStorages, errorStorages)
            }

            override fun onSuccess() = finisher.onSuccess(reportPath)

            override fun onCancel() = finisher.onCancel(this.title)

            override fun onThrowable(error: Throwable) {
                LOG.error(error)
                finisher.onError(error, this.title)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    fun generateReport(sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>): String {
        val evaluationType2storage = mutableMapOf<String, SessionsStorage>()
        val evaluationType2evaluator = mutableMapOf<String, MetricsEvaluator>()
        for (storage in sessionStorages) {
            if (evaluationType2evaluator.containsKey(storage.evaluationTitle))
                throw IllegalStateException("Workspaces have same evaluation titles. Change evaluation title in config.json.")
            evaluationType2storage[storage.evaluationTitle] = storage
            evaluationType2evaluator[storage.evaluationTitle] = MetricsEvaluator.withDefaultMetrics(storage.evaluationTitle)
            for (pathsPair in storage.getSessionFiles()) {
                val sessionFile = sessionFiles.getOrPut(pathsPair.first) { mutableListOf() }
                sessionFile.add(SessionsInfo(pathsPair.first, pathsPair.second, storage.evaluationTitle))
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
        for (errorsStorage in errorStorages)
            reportGenerator.generateErrorReports(errorsStorage.getErrors())
        return reportGenerator.generateGlobalReport(evaluationType2evaluator.values.map { it.result() }.flatten())
    }
}
