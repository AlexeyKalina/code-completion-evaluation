package org.jb.cce.evaluation

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class UIEvaluationAbortHandler(private val project: Project) : EvaluationAbortHandler {
    override fun onError(error: Throwable, stage: String) {
        Messages.showErrorDialog(project, error.localizedMessage, "$stage error")
    }

    override fun onCancel(stage: String) {
        Messages.showInfoMessage(project, "$stage was cancelled by user.", "Evaluation was cancelled")
    }
}