package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.ActionsInterpretationHandler
import org.jb.cce.util.Progress

class ActionsInterpretationStep(project: Project, isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"

    override fun evaluateStep(workspace: EvaluationWorkspace, result: FutureResult<EvaluationWorkspace?>, progress: Progress) {
        ActionsInterpretationHandler(project).invoke(workspace, workspace, progress)
        result.set(workspace)
    }
}