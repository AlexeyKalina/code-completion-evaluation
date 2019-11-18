package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.CompletionInvoker
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.ActionsInterpretationHandler
import org.jb.cce.util.Progress

class ActionsInterpretationStep(
        private val config: Config.ActionsInterpretation,
        private val language: String,
        private val completionInvoker: CompletionInvoker,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"

    override fun runInBackground(workspace: EvaluationWorkspace, progress: Progress): EvaluationWorkspace {
        ActionsInterpretationHandler(config, language, completionInvoker, project).invoke(workspace, workspace, progress)
        return workspace
    }
}