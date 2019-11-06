package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.TwoWorkspaceHandler
import org.jb.cce.util.Progress

abstract class CreateWorkspaceStep(
        private val config: Config,
        private val handler: TwoWorkspaceHandler,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {

    override fun runInBackground(workspace: EvaluationWorkspace, progress: Progress): EvaluationWorkspace {
        val newWorkspace = EvaluationWorkspace.create(config)
        handler.invoke(workspace, newWorkspace, progress)
        return newWorkspace
    }
}