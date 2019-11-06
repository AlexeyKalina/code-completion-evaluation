package org.jb.cce.evaluation.step

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.HeadlessEvaluationAbortHandler
import org.jb.cce.evaluation.UIEvaluationAbortHandler
import org.jb.cce.util.CommandLineProgress
import org.jb.cce.util.IdeaProgress
import org.jb.cce.util.Progress

abstract class BackgroundEvaluationStep(protected val project: Project, private val isHeadless: Boolean): EvaluationStep {
    protected companion object {
        val LOG = Logger.getInstance(BackgroundEvaluationStep::class.java)
    }

    abstract fun evaluateStep(workspace: EvaluationWorkspace, result: FutureResult<EvaluationWorkspace?>, progress: Progress)

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val result = FutureResult<EvaluationWorkspace?>()
        val task = object : Task.Backgroundable(project, name, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                evaluateStep(workspace, result, getProgress(indicator))
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

    private val evaluationAbortedHandler =
            if (isHeadless) HeadlessEvaluationAbortHandler() else UIEvaluationAbortHandler(project)

    private fun getProgress(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)
}