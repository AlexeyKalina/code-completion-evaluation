package org.jb.cce.evaluation.step

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jb.cce.*
import org.jb.cce.evaluation.FilteredSessionsStorage
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.SessionsStorage
import org.jb.cce.util.Progress
import java.nio.file.Path

class ReportGenerationStep(
        private val inputWorkspaces: List<EvaluationWorkspace>?,
        filters: List<SessionsFilter>,
        project: Project,
        isHeadless: Boolean) : BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Report generation"

    override val description: String = "Generation of HTML-report"

    private val sessionsFilters: List<SessionsFilter> = listOf(SessionsFilter.ACCEPT_ALL, *filters.toTypedArray())

    override fun runInBackground(workspace: EvaluationWorkspace, progress: Progress): EvaluationWorkspace {
        val workspaces = inputWorkspaces ?: listOf(workspace)
        for ((i, filter) in sessionsFilters.withIndex()) {
            if (progress.isCanceled()) {
                LOG.info("Generating reports is canceled by user. Done: $i/${sessionsFilters.size}.")
                break
            }
            LOG.info("Start generating report for filter ${filter.name}. Done: $i/${sessionsFilters.size}.")
            progress.setProgress(filter.name, "${filter.name} filter ($i/${sessionsFilters.size})", (i.toDouble() + 1) / sessionsFilters.size)
            val reportGenerator =
                    if (ApplicationManager.getApplication().isUnitTestMode) PlainTextReportGenerator(workspace.reportsDirectory(), filter.name)
                    else HtmlReportGenerator(workspace.reportsDirectory(), filter.name)
            workspace.addReport(
                    filter.name,
                    generateReport(reportGenerator,
                        workspaces.map { it.readConfig().reports.evaluationTitle },
                        workspaces.map { FilteredSessionsStorage(filter, it.sessionsStorage) },
                        workspaces.map { it.errorsStorage })
            )
        }
        return workspace
    }

    data class SessionsInfo(val path: String, val sessionsPath: String, val evaluationType: String)

    private fun generateReport(reportGenerator: ReportGenerator, evaluationTitles: List<String>, sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>): Path {
        val sessionFiles: MutableMap<String, MutableList<SessionsInfo>> = mutableMapOf()
        val title2storage = mutableMapOf<String, SessionsStorage>()
        val title2evaluator = mutableMapOf<String, MetricsEvaluator>()
        for ((index, storage) in sessionStorages.withIndex()) {
            if (title2evaluator.containsKey(evaluationTitles[index]))
                throw IllegalStateException("Workspaces have same evaluation titles. Change evaluation title in config.json.")
            title2storage[evaluationTitles[index]] = storage
            title2evaluator[evaluationTitles[index]] = MetricsEvaluator.withDefaultMetrics(evaluationTitles[index])
            for (pathsPair in storage.getSessionFiles()) {
                val sessionFile = sessionFiles.getOrPut(pathsPair.first) { mutableListOf() }
                sessionFile.add(SessionsInfo(pathsPair.first, pathsPair.second, evaluationTitles[index]))
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
