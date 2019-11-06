package org.jb.cce.evaluation

import org.jb.cce.exceptions.ExceptionsUtil
import kotlin.system.exitProcess

class HeadlessEvaluationAbortHandler : EvaluationAbortHandler {
    override fun onError(error: Throwable, stage: String) {
        println("$stage error. ${error.localizedMessage}")
        println("StackTrace:")
        println(ExceptionsUtil.stackTraceToString(error))
        exitProcess(1)
    }

    override fun onCancel(stage: String) {
        println("$stage was cancelled by user.")
        exitProcess(0)
    }
}