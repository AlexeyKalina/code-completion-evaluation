package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.TwoWorkspaceHandler
import org.jb.cce.util.Progress

abstract class CreateWorkspaceStep(
        private val config: Config,
        private val handler: TwoWorkspaceHandler,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {

    override fun evaluateStep(workspace: EvaluationWorkspace, result: FutureResult<EvaluationWorkspace?>, progress: Progress) {
        val newWorkspace = EvaluationWorkspace(workspace.path().parent.toString(), config = config)
        handler.invoke(workspace, newWorkspace, progress)
        result.set(newWorkspace)
    }
}