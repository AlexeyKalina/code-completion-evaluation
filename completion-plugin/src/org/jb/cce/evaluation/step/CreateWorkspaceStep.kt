package org.jb.cce.evaluation.step

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.TwoWorkspaceHandler

abstract class CreateWorkspaceStep(
        private val handler: TwoWorkspaceHandler,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val result = FutureResult<EvaluationWorkspace?>()
        val task = object : Task.Backgroundable(project, name) {

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val newWorkspace = EvaluationWorkspace(workspace.path().parent.toString(), config = workspace.config)
                handler.invoke(workspace, newWorkspace, getProgress(indicator))
                result.set(newWorkspace)
            }

            override fun onCancel() {
                evaluationAbortedHandler.onCancel(this.title)
                result.set(null)
            }

            override fun onThrowable(error: Throwable) {
                evaluationAbortedHandler.onError(error, this.title)
                result.set(null)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        return result.get()
    }
}