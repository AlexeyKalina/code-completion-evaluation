package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.ActionsInterpretationHandler
import org.jb.cce.util.Progress

class ActionsInterpretationStep(
        private val config: Config.ActionsInterpretation,
        private val language: String,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"

    override fun evaluateStep(workspace: EvaluationWorkspace, result: FutureResult<EvaluationWorkspace?>, progress: Progress) {
        ActionsInterpretationHandler(config, language, project).invoke(workspace, workspace, progress)
        result.set(workspace)
    }
}