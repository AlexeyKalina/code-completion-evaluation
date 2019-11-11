package org.jb.cce.evaluation.step

import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import kotlin.system.exitProcess

class HeadlessFinishEvaluationStep : FinishEvaluationStep() {
    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val reportPaths = HtmlReportGenerator.resultReports(workspace.reportsDirectory())
        print("Evaluation completed. ")
        if (reportPaths.isEmpty()) println(" Workspace: ${workspace.path()}")
        else {
            println("Reports:")
            reportPaths.forEach { println("${it.key}: ${it.value}") }
        }
        exitProcess(0)
    }
}