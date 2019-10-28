package org.jb.cce.evaluation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.jb.cce.util.CommandLineProgress
import org.jb.cce.util.IdeaProgress

abstract class BaseEvaluator(protected val project: Project, private val isHeadless: Boolean) {
    protected companion object {
        val LOG = Logger.getInstance(ActionsInterpretationEvaluator::class.java)
    }

    protected val finisher = EvaluationFinisher(project, isHeadless)

    protected fun getProcess(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)
}