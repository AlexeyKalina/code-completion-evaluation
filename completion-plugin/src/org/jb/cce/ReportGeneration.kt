package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jb.cce.actions.OpenBrowserDialog
import kotlin.system.exitProcess

fun generateReportUnderProgress(workspace: EvaluationWorkspace, sessionsStorage: List<SessionsStorage>, errorsStorage: FileErrorsStorage?, project: Project, isHeadless: Boolean) {
    val task = object : Task.Backgroundable(project, "Report generation") {
        private var reportPath: String? = null

        override fun run(indicator: ProgressIndicator) {
            reportPath = workspace.generateReport(sessionsStorage, errorsStorage)
        }

        override fun onSuccess() = finishWork(reportPath, project, isHeadless)
    }
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
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
