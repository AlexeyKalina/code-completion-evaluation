package org.jb.cce.evaluation

interface EvaluationAbortHandler {
    fun onError(error: Throwable, stage: String)
    fun onCancel(stage: String)
}