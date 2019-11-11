package org.jb.cce.evaluation.step

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.dialog.OpenBrowserDialog

class UIFinishEvaluationStep(private val project: Project) : FinishEvaluationStep() {
    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val resultReports = HtmlReportGenerator.resultReports(workspace.reportsDirectory())
        if (resultReports.isNotEmpty()) ApplicationManager.getApplication().invokeAndWait {
            val dialog = OpenBrowserDialog(resultReports.map { it.key })
            if (dialog.showAndGet()) {
                dialog.reportNamesForOpening.forEach {
                    BrowserUtil.browse(resultReports[it].toString())
                }
            }
        } else ApplicationManager.getApplication().invokeAndWait{
            Messages.showInfoMessage(project, "Evaluation completed", "Evaluation completed")
        }
        return workspace
    }
}