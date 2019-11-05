package org.jb.cce.evaluation.step

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.highlighter.Highlighter

class HighlightingTokensInIdeStep(project: Project, isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Highlighting tokens in IDE"

    override val description: String = "Highlight tokens on which completion was called"

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val result = FutureResult<EvaluationWorkspace?>()
        val task = object : Task.Backgroundable(project, name, true) {

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val highlighter = Highlighter(project)
                val sessionFiles = workspace.sessionsStorage.getSessionFiles()
                for (file in sessionFiles) {
                    val sessionsInfo = workspace.sessionsStorage.getSessions(file.first)
                    highlighter.highlight(sessionsInfo.sessions)
                }
            }

            override fun onSuccess() {
                result.set(workspace)
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