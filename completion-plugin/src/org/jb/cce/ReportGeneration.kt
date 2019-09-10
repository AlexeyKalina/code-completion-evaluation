package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import org.jb.cce.actions.OpenBrowserDialog
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.info.MetricsEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricsEvaluator
import kotlin.system.exitProcess

fun generateReport(reportGenerator: HtmlReportGenerator, sessionsInfo: List<SessionsEvaluationInfo>, errors: List<FileErrorInfo>): String {
    val metricsInfo = evaluateMetrics(sessionsInfo)
    return reportGenerator.generateReport(sessionsInfo, metricsInfo, errors)
}

fun generateReportUnderProgress(sessions: List<SessionsEvaluationInfo>, errors: List<FileErrorInfo>,
                                        reportGenerator: HtmlReportGenerator, project: Project, isHeadless: Boolean) {
    val task = object : Task.Backgroundable(project, "Report generation") {
        private var reportPath: String? = null

        override fun run(indicator: ProgressIndicator) {
            reportPath = generateReport(reportGenerator, sessions, errors)
        }

        override fun onSuccess() = finishWork(reportPath, isHeadless)
    }
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
}

fun finishWork(reportPath: String?, isHeadless: Boolean) {
    if (reportPath == null)
        if (isHeadless) exitProcess(1) else return

    if (isHeadless) {
        println("Evaluation completed. Report: $reportPath")
        exitProcess(0)
    } else {
        ApplicationManager.getApplication().invokeAndWait {
            if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }
}

private fun evaluateMetrics(evaluationsInfo: List<SessionsEvaluationInfo>): List<MetricsEvaluationInfo> {
    val metricsInfo = mutableListOf<MetricsEvaluationInfo>()
    for (sessionsInfo in evaluationsInfo) {
        val metricsEvaluator = MetricsEvaluator.withDefaultMetrics()
        val filesInfo = mutableListOf<FileEvaluationInfo<MetricInfo>>()
        for (file in sessionsInfo.sessions) {
            filesInfo.add(FileEvaluationInfo(file.filePath, metricsEvaluator.evaluate(file.results), file.text))
        }
        metricsInfo.add(MetricsEvaluationInfo(metricsEvaluator.result(), filesInfo, sessionsInfo.info))
    }
    return metricsInfo
}
