package org.jb.cce.evaluation

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jb.cce.dialog.OpenBrowserDialog
import org.jb.cce.exceptions.ExceptionsUtil
import kotlin.system.exitProcess

class EvaluationFinisher(private val project: Project, private val isHeadless: Boolean) {
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

    fun onSuccess(reportPath: String? = null) {
        when {
            isHeadless -> {
                println("Evaluation completed.${if (reportPath != null) " Report: $reportPath" else ""}")
                exitProcess(0)
            }
            reportPath == null -> Messages.showInfoMessage(project, "Evaluation completed", "Evaluation completed")
            else -> ApplicationManager.getApplication().invokeAndWait {
                if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
            }
        }
    }
}