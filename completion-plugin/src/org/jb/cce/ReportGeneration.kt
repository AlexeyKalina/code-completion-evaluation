package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jb.cce.actions.OpenBrowserDialog
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricsEvaluator
import kotlin.system.exitProcess

class ReportGeneration(private val reportGenerator: HtmlReportGenerator) {
    private companion object {
        val LOG = Logger.getInstance(ReportGeneration::class.java)
    }

    data class SessionsInfo(val path: String, val sessionsPath: String, val evaluationType: String)
    private val sessionFiles: MutableMap<String, MutableList<SessionsInfo>> = mutableMapOf()

    fun generateReportUnderProgress(sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>, project: Project, isHeadless: Boolean) {
        val task = object : Task.Backgroundable(project, "Report generation") {
            private var reportPath: String? = null

            override fun run(indicator: ProgressIndicator) {
                try {
                    reportPath = generateReport(sessionStorages, errorStorages)
                } catch (e: IllegalStateException) {
                    LOG.error("Report generation error", e)
                }
            }

            override fun onSuccess() = finishWork(reportPath, project, isHeadless)
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    fun generateReport(sessionStorages: List<SessionsStorage>, errorStorages: List<FileErrorsStorage>): String {
        val evaluationType2storage = mutableMapOf<String, SessionsStorage>()
        val evaluationType2evaluator = mutableMapOf<String, MetricsEvaluator>()
        for (storage in sessionStorages) {
            if (evaluationType2evaluator.containsKey(storage.evaluationType))
                throw IllegalStateException("Workspaces have same evaluation types. Change evaluation type in config.json.")
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
            // TODO: filter out some sessions
            reportGenerator.generateFileReport(fileEvaluations)
        }
        for (errorsStorage in errorStorages)
            reportGenerator.generateErrorReports(errorsStorage.getErrors())
        return reportGenerator.generateGlobalReport(evaluationType2evaluator.values.map { it.result() }.flatten())
    }
}

fun finishWork(reportPath: String?, project: Project, isHeadless: Boolean) {
    if (reportPath == null)
        if (isHeadless) {
            System.err.println("Evaluation completed. Report wasn't generated")
            exitProcess(1)
        } else{
            Messages.showInfoMessage(project, "Report wasn't generated", "Evaluation completed")
            return
        }

    if (isHeadless) {
        println("Evaluation completed. Report: $reportPath")
        exitProcess(0)
    } else {
        ApplicationManager.getApplication().invokeAndWait {
            if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }
}
