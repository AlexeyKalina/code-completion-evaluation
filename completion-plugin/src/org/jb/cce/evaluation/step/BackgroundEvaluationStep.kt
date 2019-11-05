package org.jb.cce.evaluation.step

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.jb.cce.evaluation.EvaluationAbortedHandler
import org.jb.cce.util.CommandLineProgress
import org.jb.cce.util.IdeaProgress

abstract class BackgroundEvaluationStep(protected val project: Project, private val isHeadless: Boolean): EvaluationStep {
    protected companion object {
        val LOG = Logger.getInstance(BackgroundEvaluationStep::class.java)
    }

    protected val evaluationAbortedHandler = EvaluationAbortedHandler(project, isHeadless)

    protected fun getProgress(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)
}