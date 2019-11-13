package org.jb.cce.evaluation.step

import org.jb.cce.EvaluationWorkspace
import kotlin.system.exitProcess

class HeadlessFinishEvaluationStep : FinishEvaluationStep() {
    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        print("Evaluation completed. ")
        if (workspace.reports.isEmpty()) println(" Workspace: ${workspace.path()}")
        else {
            println("Reports:")
            workspace.reports.forEach { println("${it.key}: ${it.value}") }
        }
        exitProcess(0)
    }
}