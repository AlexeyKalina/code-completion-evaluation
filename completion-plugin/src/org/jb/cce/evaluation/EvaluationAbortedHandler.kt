package org.jb.cce.evaluation

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jb.cce.exceptions.ExceptionsUtil
import kotlin.system.exitProcess

class EvaluationAbortedHandler(private val project: Project, private val isHeadless: Boolean) {
    fun onError(error: Throwable, stage: String) {
        if (isHeadless) {
            println("$stage error. ${error.localizedMessage}")
            println("StackTrace:")
            println(ExceptionsUtil.stackTraceToString(error))
            exitProcess(1)
        } else{
            Messages.showErrorDialog(project, error.localizedMessage, "$stage error")
        }
    }

    fun onCancel(stage: String) {
        if (isHeadless) {
            println("$stage was cancelled by user.")
            exitProcess(0)
        } else{
            Messages.showInfoMessage(project, "$stage was cancelled by user.", "Evaluation was cancelled")
        }
    }
}