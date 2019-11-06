package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.SessionsStorage
import org.jb.cce.util.Progress

class ReportGenerationStep(
        private val inputWorkspaces: List<EvaluationWorkspace>?,
        project: Project,
        isHeadless: Boolean) : BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Report generation"

    override val description: String = "Generation of HTML-report"

    override fun evaluateStep(workspace: EvaluationWorkspace, result: FutureResult<EvaluationWorkspace?>, progress: Progress) {
        val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
        val workspaces = inputWorkspaces ?: listOf(workspace)
        generateReport(reportGenerator, workspaces.map { it.sessionsStorage }, workspaces.map { it.errorsStorage })
        result.set(workspace)
    }

    data class SessionsInfo(val path: String, val sessionsPath: String, val evaluationType: String)
    private val sessionFiles: MutableMap<String, MutableList<SessionsInfo>> = mutableMapOf()

    private fun generateReport(reportGenerator: HtmlReportGenerator, sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>): String {
        val title2storage = mutableMapOf<String, SessionsStorage>()
        val title2evaluator = mutableMapOf<String, MetricsEvaluator>()
        for (storage in sessionStorages) {
            if (title2evaluator.containsKey(storage.evaluationTitle))
                throw IllegalStateException("Workspaces have same evaluation titles. Change evaluation title in config.json.")
            title2storage[storage.evaluationTitle] = storage
            title2evaluator[storage.evaluationTitle] = MetricsEvaluator.withDefaultMetrics(storage.evaluationTitle)
            for (pathsPair in storage.getSessionFiles()) {
                val sessionFile = sessionFiles.getOrPut(pathsPair.first) { mutableListOf() }
                sessionFile.add(SessionsInfo(pathsPair.first, pathsPair.second, storage.evaluationTitle))
            }
        }
        for (sessionFile in sessionFiles) {
            val fileEvaluations = mutableListOf<FileEvaluationInfo>()
            for (file in sessionFile.value) {
                val sessionsEvaluation = title2storage[file.evaluationType]!!.getSessions(file.path)
                val metricsEvaluation = title2evaluator[file.evaluationType]!!.evaluate(sessionsEvaluation.sessions)
                fileEvaluations.add(FileEvaluationInfo(sessionsEvaluation, metricsEvaluation, file.evaluationType))
            }
            reportGenerator.generateFileReport(fileEvaluations)
        }
        for (errorsStorage in errorStorages)
            reportGenerator.generateErrorReports(errorsStorage.getErrors())
        return reportGenerator.generateGlobalReport(title2evaluator.values.map { it.result() }.flatten())
    }
}
